package dev.sbutler.bitflask.raft;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import jakarta.inject.Inject;

/**
 * Handles the {@link RaftModeManager.RaftMode#LEADER} mode of the Raft server.
 *
 * <p>A new instance of this class should be created each time the server transitions to the Leader
 * mode.
 */
final class RaftLeaderProcessor extends RaftModeProcessorBase implements RaftCommandSubmitter {

  private final RaftLog raftLog;

  @Inject
  RaftLeaderProcessor(
      RaftModeManager raftModeManager,
      RaftPersistentState raftPersistentState,
      RaftVolatileState raftVolatileState,
      RaftLog raftLog) {
    super(raftModeManager, raftPersistentState, raftVolatileState);
    this.raftLog = raftLog;
  }

  private void handleUnexpectedRequest() {
    throw StatusProto.toStatusRuntimeException(
        Status.newBuilder()
            .setCode(Code.FAILED_PRECONDITION_VALUE)
            .setMessage("This server is currently the leader and requests should not be sent to it")
            .build());
  }

  @Override
  protected void beforeProcessRequestVoteRequest(RequestVoteRequest request) {
    handleUnexpectedRequest();
  }

  @Override
  protected void beforeProcessAppendEntriesRequest(AppendEntriesRequest request) {
    handleUnexpectedRequest();
  }

  @Override
  public void handleElectionTimeout() {
    throw new IllegalStateException(
        "Raft in LEADER mode should not have an election timer running");
  }

  @Override
  public void run() {}

  @Override
  public SubmitResults submitCommand(RaftCommand raftCommand) {
    Entry newEntry = RaftCommandConverter.INSTANCE.convert(raftCommand);
    raftLog.appendEntry(newEntry);
    return new SubmitResults.Success();
  }
}
