package org.adbcj.mysql.codec.decoding;

import org.adbcj.PreparedQuery;
import org.adbcj.mysql.codec.BoundedInputStream;
import org.adbcj.mysql.codec.MySqlConnection;
import org.adbcj.mysql.codec.MySqlPreparedStatement;
import org.adbcj.mysql.codec.packets.ErrorResponse;
import org.adbcj.mysql.codec.packets.OkResponse;
import org.adbcj.mysql.codec.packets.PreparedStatementToBuild;
import org.adbcj.mysql.codec.packets.StatementPreparedEOF;
import org.adbcj.support.DefaultDbSessionFuture;
import io.netty.channel.Channel;

import java.io.IOException;

/**
 * @author roman.stoffel@gamlor.info
 */
public class ExpectPreparQuery extends DecoderState {
    private final DefaultDbSessionFuture future;

    public ExpectPreparQuery(DefaultDbSessionFuture<PreparedQuery> future) {
        super();
        this.future = future;
    }

    @Override
    public ResultAndState parse(int length, int packetNumber, BoundedInputStream in, Channel channel) throws IOException {
        int fieldCount = in.read();
        if (fieldCount == ResponseStart.RESPONSE_OK) {
            return handlePrepareQuery(length,packetNumber,OkResponse.interpretAsPreparedStatement(length, packetNumber, in));
        }
        if (fieldCount == ResponseStart.RESPONSE_ERROR) {
            return handleError(ResponseStart.decodeErrorResponse(in, length, packetNumber));
        } else {
            throw new IllegalStateException("Did not expect this response from the server");
        }
    }

    private ResultAndState handlePrepareQuery(int length, int packetNumber,
                                              OkResponse.PreparedStatementOK preparedStatement) {
        final PreparedStatementToBuild statement = new PreparedStatementToBuild(length, packetNumber, preparedStatement);
        final DecoderState decoderState = FinishPrepareStatement.create(statement,future);
        if(decoderState instanceof AcceptNextResponse){
            final StatementPreparedEOF eof = new StatementPreparedEOF(length, packetNumber, statement);
            future.trySetResult(new MySqlPreparedStatement((MySqlConnection) future.getSession(),
                    eof));
            return new ResultAndState(decoderState, eof);
        } else{
            return new ResultAndState(decoderState, preparedStatement);
        }
    }

    private ResultAndState handleError(ErrorResponse errorResponse) {
        future.trySetException(errorResponse.toException());
        return new ResultAndState(new AcceptNextResponse((MySqlConnection) future.getSession()),errorResponse );
    }
}
