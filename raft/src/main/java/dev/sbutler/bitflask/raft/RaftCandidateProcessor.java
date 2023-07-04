package dev.sbutler.bitflask.raft;

import dev.sbutler.bitflask.raft.RaftClusterCandidateRpcClient.RequestVotesResults;
import dev.sbutler.bitflask.raft.RaftLog.LogEntryDetails;
import jakarta.inject.Inject;

/**
 * Handles the {@link RaftModeManager.RaftMode#CANDIDATE} mode of the Raft server.
 *
 * <p>A new instance of this class should be created each time the server transitions to the
 * Candidate mode.
 */
final class RaftCandidateProcessor extends RaftModeProcessorBase {

  private final RaftClusterConfiguration raftClusterConfiguration;
  private final RaftModeManager raftModeManager;
  private final RaftElectionTimer raftElectionTimer;
  private final RaftClusterRpcChannelManager raftClusterRpcChannelManager;

  private volatile boolean shouldContinueElections = true;
  private volatile boolean hasElectionTimeoutOccurred = false;

  @Inject
  RaftCandidateProcessor(
      RaftClusterConfiguration raftClusterConfiguration,
      RaftModeManager raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      RaftElectionTimer raftElectionTimer,
      RaftClusterRpcChannelManager raftClusterRpcChannelManager) {
    super(raftModeManager, raftPersistentState, raftVolatileState);
    this.raftClusterConfiguration = raftClusterConfiguration;
    this.raftModeManager = raftModeManager;
    this.raftElectionTimer = raftElectionTimer;
    this.raftClusterRpcChannelManager = raftClusterRpcChannelManager;
  }

  @Override
  protected void beforeUpdateTermAndTransitionToFollower(int rpcTerm) {
    shouldContinueElections = false;
  }

  @Override
  protected void beforeProcessAppendEntriesRequest(AppendEntriesRequest request) {
    // Concede to new leader
    if (request.getTerm() >= raftPersistentState.getCurrentTerm()) {
      shouldContinueElections = false;
      updateTermAndTransitionToFollower(request.getTerm());
    }
  }

  public void handleElectionTimeout() {
    hasElectionTimeoutOccurred = true;
  }

  /**
   * The main loop of this class that executes until the RequestVotes RPCs sent cause termination,
   * or an RPC is received from another server that causes termination.
   */
  @Override
  public void run() {
    while (shouldContinueElections) {
      startNewElection();
      // Do not start another election until election timer timeout
      while (shouldContinueElections && !hasElectionTimeoutOccurred) {
        Thread.onSpinWait();
      }
    }
  }

  /** Starts and handles a single election cycle. */
  private void startNewElection() {
    raftPersistentState.incrementTermAndVoteForSelf();
    raftElectionTimer.restart();
    hasElectionTimeoutOccurred = false;
    try (var candidateRpcClient =
        raftClusterRpcChannelManager.createRaftClusterCandidateRpcClient()) {
      sendRequestVotesAndWait(candidateRpcClient);
    }
  }

  /**
   * Requests a vote from all other Raft servers in the cluster and waits.
   *
   * <p>This method waits until
   *
   * <ul>
   *   <li>The election has been won.
   *   <li>All responses have been received.
   *   <li>A greater term is encountered in a {@link RequestVoteResponse}.
   *   <li>An election timeout has occurred.
   *   <li>The candidate is halted.
   */
  private void sendRequestVotesAndWait(RaftClusterCandidateRpcClient candidateRpcClient) {
    LogEntryDetails lastLogEntryDetails = raftPersistentState.getRaftLog().getLastLogEntryDetails();
    RequestVoteRequest request =
        RequestVoteRequest.newBuilder()
            .setCandidateId(raftClusterConfiguration.thisRaftServerId().id())
            .setTerm(raftPersistentState.getCurrentTerm())
            .setLastLogIndex(lastLogEntryDetails.index())
            .setLastLogTerm(lastLogEntryDetails.term())
            .build();
    candidateRpcClient.requestVotes(request);

    while (shouldContinueElections && !hasElectionTimeoutOccurred) {
      RequestVotesResults requestVotesResults = candidateRpcClient.getCurrentRequestVotesResults();
      if (shouldUpdateTermAndTransitionToFollower(requestVotesResults.largestTermSeen())) {
        updateTermAndTransitionToFollower(requestVotesResults.largestTermSeen());
      } else if (receivedMajorityVotes(requestVotesResults)) {
        shouldContinueElections = false;
        raftModeManager.transitionToLeaderState();
      } else if (requestVotesResults.allResponsesReceived()) {
        break;
      }
    }
  }

  private boolean receivedMajorityVotes(RequestVotesResults requestVotesResults) {
    int totalServers = raftClusterConfiguration.clusterServers().size();
    int votesReceived = 1 + requestVotesResults.numberVotesReceived();
    double requiredForMajority = totalServers / 2.0;
    return votesReceived > requiredForMajority;
  }
}
