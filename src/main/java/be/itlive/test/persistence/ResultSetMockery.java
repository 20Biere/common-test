package be.itlive.test.persistence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.mockito.MockSettings;
import org.mockito.internal.stubbing.defaultanswers.ForwardsInvocations;
import org.mockito.stubbing.Answer;

/**
 *
 * @author vbiertho
 *
 */
public class ResultSetMockery {

    private final List<String> columns;

    private final List<List<String>> rows;

    private final SQLAliasMapper aliasMapper;

    public static ResultSetMockery resultSet(final String... columns) {
        return new ResultSetMockery(columns);
    }

    public static ResultSetMockery fromCSVLines(final String header, final String nullValue, final String... lines) {
        ResultSetMockery resultSetMockery = new ResultSetMockery(header);
        for (String line : lines) {
            resultSetMockery.parseRow(line, nullValue);
        }
        return resultSetMockery;
    }

    public ResultSetMockery(final String... columns) {
        this.aliasMapper = new SQLAliasMapper();

        if (columns.length == 1 && columns[0].contains(";")) {
            this.columns = Arrays.asList(columns[0].split(";"));
        } else {
            this.columns = Arrays.asList(columns);
        }
        rows = new ArrayList<List<String>>();
    }

    public ResultSetMockery parseRow(final String values, final String nullValue) {
        List<String> row = new ArrayList<String>();
        for (String s : values.split(";")) {
            if (nullValue != null && nullValue.equals(s)) {
                row.add(null);
            } else {
                row.add(s);
            }
        }
        rows.add(row);
        return this;
    }

    public ResultSetMockery addRow(final String... values) {
        rows.add(Arrays.asList(values));
        return this;
    }

    public ResultSet createMock() {
        return mock(ResultSet.class, withSettings().defaultAnswer(new ForwardsInvocations(new ResultSetStub())));
    }

    public ResultSet createMock(final MockSettings settings) {
        return mock(ResultSet.class, settings.defaultAnswer(new ForwardsInvocations(new ResultSetStub())));
    }

    private ResultSetMetaData createMetaData() {
        return mock(ResultSetMetaData.class, withSettings().defaultAnswer(new ForwardsInvocations(new ResultSetMetaDataStub())));
    }

    public Answer<PreparedStatement> answerPreparedStatement() {
        PreparedStatementMockery ps = new PreparedStatementMockery().withResult(createMock());
        return aliasMapper.captureSQLAndReturn(0, ps.createSelectStatementMock());
    }

    public SQLAliasMapper getAliasMapper() {
        return aliasMapper;
    }

    public class ResultSetMetaDataStub {

        public int getColumnCount() {
            return columns.size();
        }

    }

    private String translateAlias(final String alias) {
        final String requestedColumn;
        if (aliasMapper == null) {
            requestedColumn = alias;
        } else {
            requestedColumn = aliasMapper.getColumnNameFromAlias(alias);
        }
        return requestedColumn;
    }

    private int findColumnIndex(final String alias) {
        final String requestedColumn = translateAlias(alias);
        for (int i = 0; i < columns.size(); i++) {
            String columnName = columns.get(i);
            if (Pattern.matches(".*(?:^|\\.)" + columnName, requestedColumn)) {
                return i;
            }
        }
        return -1;
    }

    public class ResultSetStub {

        private int currentrow = -1;

        private boolean lastWasNull = false;

        private ResultSetMetaData metadata = createMetaData();

        public ResultSetMetaData getMetaData() {
            return metadata;
        }

        public void close() {
        }

        public boolean next() {
            if (currentrow + 1 >= rows.size()) {
                return false;
            } else {
                currentrow++;
                return true;
            }
        }

        public String getString(final String alias) {
            int colIndex = findColumnIndex(translateAlias(alias));
            if (colIndex == -1) {
                System.out.println("Missing columns in ResultSet Mock : " + translateAlias(alias));
                lastWasNull = true;
                return null;
            }
            return getString(colIndex + 1);
        }

        public String getString(final int position) {
            String s = rows.get(currentrow).get(position - 1);
            if (s == null) {
                lastWasNull = true;
                return null;
            } else {
                lastWasNull = false;
                return s;
            }
        }

        public long getLong(final String value) {
            String s = getString(value);
            if (s == null) {
                return 0;
            } else {
                return Long.valueOf(s);
            }
        }

        public long getLong(final int position) {
            String s = getString(position);
            if (s == null) {
                return 0;
            } else {
                return Long.valueOf(s);
            }
        }

        public int getInt(final String value) {
            String s = getString(value);
            if (s == null) {
                return 0;
            } else {
                return Integer.valueOf(s);
            }
        }

        public Timestamp getTimestamp(final String colName) throws ParseException {
            String s = getString(colName);
            if (s == null) {
                return null;
            } else {
                return new Timestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s).getTime());
            }
        }

        public java.sql.Date getDate(final String colName) throws ParseException {
            String s = getString(colName);
            if (s == null) {
                return null;
            } else {
                return new java.sql.Date(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s).getTime());
            }
        }

        public boolean getBoolean(final String colName) {
            String s = getString(colName);
            if (s == null) {
                return false;
            } else {
                return Boolean.parseBoolean(s);
            }
        }

        public boolean wasNull() {
            return lastWasNull;
        }

    }
}
