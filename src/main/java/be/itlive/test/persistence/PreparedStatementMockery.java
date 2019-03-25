package be.itlive.test.persistence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Timestamp;

import org.mockito.MockSettings;
import org.mockito.internal.stubbing.defaultanswers.ForwardsInvocations;

public class PreparedStatementMockery {

    private ResultSet[] resultSet;

    private Integer[] updateCount;

    public static PreparedStatementMockery preparedStatement() {
        return new PreparedStatementMockery();
    }

    public PreparedStatementMockery() {

    }

    public PreparedStatementMockery withResult(final ResultSet... resultSet) {
        this.resultSet = resultSet;
        return this;
    }

    public PreparedStatementMockery withUpdateCount(final Integer... updateCount) {
        this.updateCount = updateCount;
        return this;
    }

    public PreparedStatement createSelectStatementMock() {
        return createSelectStatementMock(withSettings());
    }

    public PreparedStatement createUpdateStatementMock() {
        return createUpdateStatementMock(withSettings());
    }

    public PreparedStatement createSelectStatementMock(final MockSettings settings) {
        return mock(PreparedStatement.class, settings.defaultAnswer(new ForwardsInvocations(new SelectPreparedStatementStub())));
    }

    public PreparedStatement createUpdateStatementMock(final MockSettings settings) {
        return mock(PreparedStatement.class, settings.defaultAnswer(new ForwardsInvocations(new UpdatePraparedStatementStub())));
    }

    public class PreparedStatementStub {

        public void setLong(final int i, final long value) {

        }

        public void setInt(final int i, final int value) {
        }

        public void setString(final int i, final String value) {

        }

        public void setNull(final int i, final int type) {

        }

        public void setBoolean(final int i, final boolean value) {

        }

        public void setDate(final int i, final Date value) {
        }

        public void setTimestamp(final int i, final Timestamp value) {
        }

        public void setObject(final int i, final Object value, final int a) {

        }

        public SQLWarning getWarnings() {
            return null;
        }

        public void clearWarnings() {

        }

        int currentResult = 0;

        public boolean execute() {
            if (resultSet != null && resultSet.length > currentResult && resultSet[currentResult] != null) {
                return true;
            } else {
                return false;
            }
        }

        public boolean getMoreResults() {
            currentResult++;
            if (resultSet != null && resultSet.length > currentResult && resultSet[currentResult] != null) {
                return true;
            } else {
                return false;
            }
        }

        public int getUpdateCount() {
            if (updateCount != null && updateCount.length > currentResult && updateCount[currentResult] != null) {
                return updateCount[currentResult];
            } else {
                return -1;
            }
        }
    }

    public class SelectPreparedStatementStub extends PreparedStatementStub {

        public ResultSet executeQuery() throws SQLException {
            return resultSet[currentResult];
        }
    }

    public class UpdatePraparedStatementStub extends PreparedStatementStub {

        private int currentResult = 0;

        @Override
        public boolean execute() {
            return false;
        }

        public int executeUpdate() {
            return updateCount[currentResult];
        }

        public ResultSet getResultSet() throws SQLException {
            return resultSet[currentResult];
        }
    }
}
