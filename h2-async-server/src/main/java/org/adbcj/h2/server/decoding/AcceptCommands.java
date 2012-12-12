package org.adbcj.h2.server.decoding;

import org.adbcj.h2.decoding.IoUtils;
import org.adbcj.h2.decoding.ResultOrWait;
import org.adbcj.h2.protocol.CommandCodes;
import org.adbcj.h2.protocol.StatusCodes;
import org.adbcj.h2.server.responses.SendStatus;
import org.adbcj.h2.server.responses.UndoLogPosResponse;
import org.h2.constant.SysProperties;
import org.h2.engine.Session;
import org.h2.engine.SessionRemote;
import org.h2.util.SmallMap;
import org.jboss.netty.channel.Channel;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * @author roman.stoffel@gamlor.info
 */
public class AcceptCommands implements DecoderState {
    private final Session session;
    private SmallMap cache = new SmallMap(SysProperties.SERVER_CACHED_OBJECTS);

    private final ExecuteCloseCommand closeCommand = new ExecuteCloseCommand(this);
    private final ExecuteCloseResult closeResult = new ExecuteCloseResult(this);
    private final ChangeSessionId changeSessionIdHandler = new ChangeSessionId(this);
    private final ExecuteQuery executeQueryCommand = new ExecuteQuery(this);
    private final ExecuteUpdate executeCommand = new ExecuteUpdate(this);
    private final SetAutoCommitCommand changeAutoCommitHandler = new SetAutoCommitCommand(this);

    public AcceptCommands(Session session) {
        this.session = session;
    }

    @Override
    public ResultAndState decode(DataInputStream stream, Channel channel) throws IOException {
        final ResultOrWait<Integer> command = IoUtils.tryReadNextInt(stream, ResultOrWait.Start);
        if (command.couldReadResult) {
            return handleCommand(channel,command.result);
        } else {
            return ResultAndState.waitForMoreInput(this);
        }
    }

    private ResultAndState handleCommand(Channel channel, int command) {
        switch (CommandCodes.commandFor(command)) {
            case COMMAND_CLOSE:
                return ResultAndState.newState(closeCommand);
            case RESULT_CLOSE:
                return ResultAndState.newState(closeResult);
            case SESSION_CLOSE:
                session().close();
                channel.write(new SendStatus(StatusCodes.STATUS_OK));
                return ResultAndState.newState(this);
            case COMMAND_EXECUTE_QUERY:
                return ResultAndState.newState(executeQueryCommand);
            case COMMAND_EXECUTE_UPDATE:
                return ResultAndState.newState(executeCommand);
            case SESSION_PREPARE:
                return ResultAndState.newState(new PreparedStatement(this, false));
            case SESSION_PREPARE_READ_PARAMS:
                return ResultAndState.newState(new PreparedStatement(this, true));
            case SESSION_SET_AUTOCOMMIT:
                return ResultAndState.newState(changeAutoCommitHandler);
            case SESSION_SET_ID:
                return ResultAndState.newState(changeSessionIdHandler);
            case SESSION_UNDO_LOG_POS:
                final int undoLogPos = session().getUndoLogPos();
                channel.write(new UndoLogPosResponse(undoLogPos));
                return ResultAndState.newState(this);
            default:
                throw new IllegalStateException("Couldn't handle command for "+command);
        }
    }






    int getState(int oldModificationId) {
        if (session.getModificationId() == oldModificationId) {
            return SessionRemote.STATUS_OK;
        }
        return SessionRemote.STATUS_OK_STATE_CHANGED;
    }


    public Session session() {
        return session;
    }

    public SmallMap cache() {
        return cache;
    }
}