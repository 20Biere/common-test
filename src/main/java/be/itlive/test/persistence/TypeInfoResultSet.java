package be.itlive.test.persistence;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * @author vbiertho
 *
 */
public class TypeInfoResultSet implements ResultSet {

    /**
     * @param resultSet resultset to serialize.
     * @return content of the resultset serialied.
     * @throws SQLException
     */
    public static String serializeInformation(final ResultSet resultSet) throws SQLException {
        StringWriter s = new StringWriter();
        PrintWriter out = new PrintWriter(s);
        ResultSetMetaData m = resultSet.getMetaData();
        for (int i = 1; i < m.getColumnCount(); i++) {
            out.print(m.getColumnName(i) + ",");
            out.print(m.getColumnType(i) + ",");
            out.print(m.getColumnTypeName(i) + ",");
            out.print(m.getColumnClassName(i) + ",");
            out.print(m.getPrecision(i) + ",");
            out.print(m.getScale(i) + ",");
            out.print(';');
        }
        out.println();
        while (resultSet.next()) {
            for (int i = 1; i < m.getColumnCount(); i++) {
                Object o = resultSet.getObject(i);
                if (resultSet.wasNull()) {
                    out.print("NULL;");
                } else {
                    out.print(o + ";");
                }
            }
            out.println();
        }
        return s.getBuffer().toString();
    }

    /**
     * @author vbiertho
     *
     */
    public static class TypeInfoMetadata implements ResultSetMetaData {

        private final List<String> columnsNames = new ArrayList<String>();

        private final List<Integer> columnsTypes = new ArrayList<Integer>();

        private final List<String> columnsTypesNames = new ArrayList<String>();

        private final List<String> columnsClassNames = new ArrayList<String>();

        /**
         * @param headers columns definitions
         */
        public TypeInfoMetadata(final String headers) {
            String[] parts = headers.split(",");
            columnsNames.add(parts[0]);
            columnsTypes.add(Integer.valueOf(parts[1]));
            columnsTypesNames.add(parts[2]);
            columnsClassNames.add(parts[3]);
        }

        public List<String> getColumnsNames() {
            return Collections.unmodifiableList(columnsNames);
        }

        @Override
        public <T> T unwrap(final Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            } else {
                throw new SQLException();
            }
        }

        @Override
        public boolean isWrapperFor(final Class<?> iface) throws SQLException {
            return iface.isInstance(this);
        }

        @Override
        public int getColumnCount() throws SQLException {
            return columnsNames.size();
        }

        @Override
        public boolean isAutoIncrement(final int column) throws SQLException {
            return false;
        }

        @Override
        public boolean isCaseSensitive(final int column) throws SQLException {
            return false;
        }

        @Override
        public boolean isSearchable(final int column) throws SQLException {
            return false;
        }

        @Override
        public boolean isCurrency(final int column) throws SQLException {
            return false;
        }

        @Override
        public int isNullable(final int column) throws SQLException {
            return ResultSetMetaData.columnNullable;
        }

        @Override
        public boolean isSigned(final int column) throws SQLException {
            return true;
        }

        @Override
        public int getColumnDisplaySize(final int column) throws SQLException {
            return 0;
        }

        @Override
        public String getColumnLabel(final int column) throws SQLException {
            return columnsNames.get(column);
        }

        @Override
        public String getColumnName(final int column) throws SQLException {
            return columnsNames.get(column);
        }

        @Override
        public String getSchemaName(final int column) throws SQLException {
            return "";
        }

        @Override
        public int getPrecision(final int column) throws SQLException {
            return 0;
        }

        @Override
        public int getScale(final int column) throws SQLException {
            return 0;
        }

        @Override
        public String getTableName(final int column) throws SQLException {
            return "";
        }

        @Override
        public String getCatalogName(final int column) throws SQLException {
            return "";
        }

        @Override
        public int getColumnType(final int column) throws SQLException {
            return columnsTypes.get(column);
        }

        @Override
        public String getColumnTypeName(final int column) throws SQLException {
            return columnsTypesNames.get(column);
        }

        @Override
        public boolean isReadOnly(final int column) throws SQLException {
            return true;
        }

        @Override
        public boolean isWritable(final int column) throws SQLException {
            return false;
        }

        @Override
        public boolean isDefinitelyWritable(final int column) throws SQLException {
            return false;
        }

        @Override
        public String getColumnClassName(final int column) throws SQLException {
            return columnsClassNames.get(column);
        }

    }

    private final List<List<String>> values;

    private final int numberOfRecord;

    private int currentRecord = 0;

    private boolean lastWasNull = false;

    private int direction;

    private int fetchSize;

    private TypeInfoMetadata metadata;

    /**
     * @param csvResultSet information on datatypes
     */
    public TypeInfoResultSet(final String csvResultSet) {
        try (StringReader stringReader = new StringReader(csvResultSet); BufferedReader bufferedReader = new BufferedReader(stringReader);) {
            List<List<String>> values = new ArrayList<List<String>>();
            String line = bufferedReader.readLine();
            if (line.endsWith(";")) {
                line = line.substring(0, line.length() - 1);
            }
            metadata = new TypeInfoMetadata(line);
            int count = 0;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.endsWith(";")) {
                    line = line.substring(0, line.length() - 1);
                }
                List<String> row = new ArrayList<String>();
                row.addAll(Arrays.asList(line.split(";")));
                values.add(row);
                count++;
            }
            this.numberOfRecord = count;
            this.values = Collections.unmodifiableList(values);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        } else {
            throw new SQLException();
        }
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    @Override
    public int getHoldability() throws SQLException {
        return HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public void close() throws SQLException {
        // Nothing todo ?
    }

    @Override
    public boolean isClosed() throws SQLException {
        return false;
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return currentRecord < 1;
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return currentRecord > numberOfRecord;
    }

    @Override
    public boolean isFirst() throws SQLException {
        return currentRecord == 1;
    }

    @Override
    public boolean isLast() throws SQLException {
        return currentRecord == numberOfRecord;
    }

    @Override
    public void beforeFirst() throws SQLException {
        currentRecord = 0;
    }

    @Override
    public void afterLast() throws SQLException {
        currentRecord = numberOfRecord + 1;
    }

    @Override
    public boolean next() throws SQLException {
        return ++currentRecord <= numberOfRecord;
    }

    @Override
    public boolean previous() throws SQLException {
        currentRecord--;
        return (currentRecord > 0 || currentRecord <= numberOfRecord);
    }

    @Override
    public boolean first() throws SQLException {
        currentRecord = 1;
        return currentRecord <= numberOfRecord;
    }

    @Override
    public boolean last() throws SQLException {
        currentRecord = numberOfRecord;
        return currentRecord > 0;
    }

    @Override
    public int getRow() throws SQLException {
        return (currentRecord > 0 || currentRecord <= numberOfRecord) ? currentRecord : 0;
    }

    @Override
    public boolean absolute(final int row) throws SQLException {
        currentRecord = row;
        return (currentRecord > 0 || currentRecord <= numberOfRecord);
    }

    @Override
    public boolean relative(final int rows) throws SQLException {
        currentRecord += rows;
        return (currentRecord > 0 || currentRecord <= numberOfRecord);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public void setFetchDirection(final int direction) throws SQLException {
        this.direction = direction;

    }

    @Override
    public int getFetchDirection() throws SQLException {
        return direction;
    }

    @Override
    public void setFetchSize(final int rows) throws SQLException {
        this.fetchSize = rows;

    }

    @Override
    public int getFetchSize() throws SQLException {
        return fetchSize;
    }

    @Override
    public int getType() throws SQLException {
        return TYPE_SCROLL_INSENSITIVE;
    }

    @Override
    public int getConcurrency() throws SQLException {
        return CONCUR_READ_ONLY;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public boolean rowInserted() throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void insertRow() throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateRow() throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void deleteRow() throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void refreshRow() throws SQLException {
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void moveToCurrentRow() throws SQLException {

    }

    @Override
    public Statement getStatement() throws SQLException {
        return null;
    }

    @Override
    public String getCursorName() throws SQLException {
        return "CUR_TYPE_INFOS";
    }

    @Override
    public boolean wasNull() throws SQLException {
        return lastWasNull;
    }

    @Override
    public TypeInfoMetadata getMetaData() throws SQLException {
        return metadata;
    }

    private List<String> getCurrentRowValues() {
        return values.get(currentRecord - 1);
    }

    @Override
    public int findColumn(final String columnLabel) throws SQLException {
        ListIterator<String> iterator = getMetaData().getColumnsNames().listIterator();
        int index = 0;
        while (iterator.hasNext()) {
            index++;
            if (iterator.next().equals(columnLabel)) {
                return index;
            }
        }
        throw new SQLException("Columns " + columnLabel + " not found");
    }

    @Override
    public String getString(final int columnIndex) throws SQLException {
        String string = getCurrentRowValues().get(columnIndex - 1);
        if (string.equals("NULL")) {
            lastWasNull = true;
            return null;
        } else {
            lastWasNull = false;
        }
        return string;
    }

    @Override
    public String getString(final String columnLabel) throws SQLException {
        return getCurrentRowValues().get(findColumn(columnLabel));
    }

    @Override
    public boolean getBoolean(final int columnIndex) throws SQLException {
        return Boolean.valueOf(getString(columnIndex)).booleanValue();
    }

    @Override
    public byte getByte(final int columnIndex) throws SQLException {
        return Byte.valueOf(getString(columnIndex)).byteValue();
    }

    @Override
    public short getShort(final int columnIndex) throws SQLException {
        return Short.valueOf(getString(columnIndex)).shortValue();
    }

    @Override
    public int getInt(final int columnIndex) throws SQLException {
        return Integer.valueOf(getString(columnIndex)).intValue();
    }

    @Override
    public long getLong(final int columnIndex) throws SQLException {
        return Long.valueOf(getString(columnIndex)).longValue();
    }

    @Override
    public float getFloat(final int columnIndex) throws SQLException {
        return Float.valueOf(getString(columnIndex)).floatValue();
    }

    @Override
    public double getDouble(final int columnIndex) throws SQLException {
        return Double.valueOf(getString(columnIndex)).doubleValue();
    }

    @Override
    public BigDecimal getBigDecimal(final int columnIndex, final int scale) throws SQLException {
        return new BigDecimal(getString(columnIndex)).round(new MathContext(scale));
    }

    @Override
    public byte[] getBytes(final int columnIndex) throws SQLException {
        return getString(columnIndex).getBytes();
    }

    @Override
    public Date getDate(final int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public Time getTime(final int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public Timestamp getTimestamp(final int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public InputStream getAsciiStream(final int columnIndex) throws SQLException {
        try {
            return new ByteArrayInputStream(getString(columnIndex).getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public InputStream getUnicodeStream(final int columnIndex) throws SQLException {
        try {
            return new ByteArrayInputStream(getString(columnIndex).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public InputStream getBinaryStream(final int columnIndex) throws SQLException {
        return new ByteArrayInputStream(getString(columnIndex).getBytes());
    }

    @Override
    public boolean getBoolean(final String columnLabel) throws SQLException {
        return getBoolean(findColumn(columnLabel));
    }

    @Override
    public byte getByte(final String columnLabel) throws SQLException {
        return getByte(findColumn(columnLabel));
    }

    @Override
    public short getShort(final String columnLabel) throws SQLException {
        return getShort(findColumn(columnLabel));
    }

    @Override
    public int getInt(final String columnLabel) throws SQLException {
        return getInt(findColumn(columnLabel));
    }

    @Override
    public long getLong(final String columnLabel) throws SQLException {
        return getLong(findColumn(columnLabel));
    }

    @Override
    public float getFloat(final String columnLabel) throws SQLException {
        return getFloat(findColumn(columnLabel));
    }

    @Override
    public double getDouble(final String columnLabel) throws SQLException {
        return getDouble(findColumn(columnLabel));
    }

    @Override
    public BigDecimal getBigDecimal(final String columnLabel, final int scale) throws SQLException {
        return getBigDecimal(findColumn(columnLabel), scale);
    }

    @Override
    public byte[] getBytes(final String columnLabel) throws SQLException {
        return getBytes(findColumn(columnLabel));
    }

    @Override
    public Date getDate(final String columnLabel) throws SQLException {
        return getDate(findColumn(columnLabel));
    }

    @Override
    public Time getTime(final String columnLabel) throws SQLException {
        return getTime(findColumn(columnLabel));
    }

    @Override
    public Timestamp getTimestamp(final String columnLabel) throws SQLException {
        return getTimestamp(findColumn(columnLabel));
    }

    @Override
    public InputStream getAsciiStream(final String columnLabel) throws SQLException {
        return getAsciiStream(findColumn(columnLabel));
    }

    @Override
    public InputStream getUnicodeStream(final String columnLabel) throws SQLException {
        return getUnicodeStream(findColumn(columnLabel));
    }

    @Override
    public InputStream getBinaryStream(final String columnLabel) throws SQLException {
        return getBinaryStream(findColumn(columnLabel));
    }

    @Override
    public Object getObject(final int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    @Override
    public Object getObject(final String columnLabel) throws SQLException {
        return getObject(findColumn(columnLabel));
    }

    @Override
    public Reader getCharacterStream(final int columnIndex) throws SQLException {
        return new StringReader(getString(columnIndex));
    }

    @Override
    public Reader getCharacterStream(final String columnLabel) throws SQLException {
        return getCharacterStream(findColumn(columnLabel));
    }

    @Override
    public BigDecimal getBigDecimal(final int columnIndex) throws SQLException {
        return new BigDecimal(getString(columnIndex));
    }

    @Override
    public BigDecimal getBigDecimal(final String columnLabel) throws SQLException {

        return getBigDecimal(findColumn(columnLabel));
    }

    @Override
    public Object getObject(final int columnIndex, final Map<String, Class<?>> map) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Ref getRef(final int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public Blob getBlob(final int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public Clob getClob(final int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public Array getArray(final int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getObject(final String columnLabel, final Map<String, Class<?>> map) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Ref getRef(final String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return getRef(findColumn(columnLabel));
    }

    @Override
    public Blob getBlob(final String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return getBlob(findColumn(columnLabel));
    }

    @Override
    public Clob getClob(final String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return getClob(findColumn(columnLabel));
    }

    @Override
    public Array getArray(final String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return getArray(findColumn(columnLabel));
    }

    @Override
    public Date getDate(final int columnIndex, final Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public Date getDate(final String columnLabel, final Calendar cal) throws SQLException {

        return getDate(findColumn(columnLabel), cal);
    }

    @Override
    public Time getTime(final int columnIndex, final Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public Time getTime(final String columnLabel, final Calendar cal) throws SQLException {

        return getTime(findColumn(columnLabel), cal);
    }

    @Override
    public Timestamp getTimestamp(final int columnIndex, final Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public Timestamp getTimestamp(final String columnLabel, final Calendar cal) throws SQLException {
        return getTimestamp(findColumn(columnLabel), cal);
    }

    @Override
    public URL getURL(final int columnIndex) throws SQLException {
        try {
            return new URL(getString(columnIndex));
        } catch (MalformedURLException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public URL getURL(final String columnLabel) throws SQLException {
        return getURL(findColumn(columnLabel));
    }

    @Override
    public RowId getRowId(final int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public RowId getRowId(final String columnLabel) throws SQLException {
        return getRowId(findColumn(columnLabel));
    }

    @Override
    public NClob getNClob(final int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public NClob getNClob(final String columnLabel) throws SQLException {
        return getNClob(findColumn(columnLabel));
    }

    @Override
    public SQLXML getSQLXML(final int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public SQLXML getSQLXML(final String columnLabel) throws SQLException {
        return getSQLXML(findColumn(columnLabel));
    }

    @Override
    public String getNString(final int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    @Override
    public String getNString(final String columnLabel) throws SQLException {
        return getNString(findColumn(columnLabel));
    }

    @Override
    public Reader getNCharacterStream(final int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public Reader getNCharacterStream(final String columnLabel) throws SQLException {
        return getNCharacterStream(findColumn(columnLabel));
    }

    @Override
    public <T> T getObject(final int columnIndex, final Class<T> type) throws SQLException {
        // TODO Auto-generated method stub
        throw new SQLException("Not implemented yet");
    }

    @Override
    public <T> T getObject(final String columnLabel, final Class<T> type) throws SQLException {
        return getObject(findColumn(columnLabel), type);
    }

    @Override
    public void updateNull(final int columnIndex) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBoolean(final int columnIndex, final boolean x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateByte(final int columnIndex, final byte x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateShort(final int columnIndex, final short x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateInt(final int columnIndex, final int x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateLong(final int columnIndex, final long x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateFloat(final int columnIndex, final float x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateDouble(final int columnIndex, final double x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBigDecimal(final int columnIndex, final BigDecimal x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateString(final int columnIndex, final String x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBytes(final int columnIndex, final byte[] x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateDate(final int columnIndex, final Date x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateTime(final int columnIndex, final Time x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateTimestamp(final int columnIndex, final Timestamp x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateAsciiStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBinaryStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateCharacterStream(final int columnIndex, final Reader x, final int length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateObject(final int columnIndex, final Object x, final int scaleOrLength) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateObject(final int columnIndex, final Object x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNull(final String columnLabel) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBoolean(final String columnLabel, final boolean x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateByte(final String columnLabel, final byte x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateShort(final String columnLabel, final short x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateInt(final String columnLabel, final int x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateLong(final String columnLabel, final long x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateFloat(final String columnLabel, final float x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateDouble(final String columnLabel, final double x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBigDecimal(final String columnLabel, final BigDecimal x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateString(final String columnLabel, final String x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBytes(final String columnLabel, final byte[] x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateDate(final String columnLabel, final Date x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateTime(final String columnLabel, final Time x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateTimestamp(final String columnLabel, final Timestamp x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateAsciiStream(final String columnLabel, final InputStream x, final int length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBinaryStream(final String columnLabel, final InputStream x, final int length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateCharacterStream(final String columnLabel, final Reader reader, final int length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateObject(final String columnLabel, final Object x, final int scaleOrLength) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateObject(final String columnLabel, final Object x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateRef(final int columnIndex, final Ref x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateRef(final String columnLabel, final Ref x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBlob(final int columnIndex, final Blob x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBlob(final String columnLabel, final Blob x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateClob(final int columnIndex, final Clob x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateClob(final String columnLabel, final Clob x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateArray(final int columnIndex, final Array x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateArray(final String columnLabel, final Array x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateRowId(final int columnIndex, final RowId x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateRowId(final String columnLabel, final RowId x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNString(final int columnIndex, final String nString) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNString(final String columnLabel, final String nString) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNClob(final int columnIndex, final NClob nClob) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNClob(final String columnLabel, final NClob nClob) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateSQLXML(final int columnIndex, final SQLXML xmlObject) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateSQLXML(final String columnLabel, final SQLXML xmlObject) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNCharacterStream(final String columnLabel, final Reader reader, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateAsciiStream(final int columnIndex, final InputStream x, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBinaryStream(final int columnIndex, final InputStream x, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateAsciiStream(final String columnLabel, final InputStream x, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBinaryStream(final String columnLabel, final InputStream x, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateCharacterStream(final String columnLabel, final Reader reader, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBlob(final int columnIndex, final InputStream inputStream, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBlob(final String columnLabel, final InputStream inputStream, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNCharacterStream(final int columnIndex, final Reader x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateAsciiStream(final int columnIndex, final InputStream x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBinaryStream(final int columnIndex, final InputStream x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateCharacterStream(final int columnIndex, final Reader x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateAsciiStream(final String columnLabel, final InputStream x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBinaryStream(final String columnLabel, final InputStream x) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBlob(final int columnIndex, final InputStream inputStream) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateBlob(final String columnLabel, final InputStream inputStream) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateClob(final int columnIndex, final Reader reader) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateClob(final String columnLabel, final Reader reader) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNClob(final int columnIndex, final Reader reader) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

    @Override
    public void updateNClob(final String columnLabel, final Reader reader) throws SQLException {
        throw new SQLException("ResultSet is Readonly");
    }

}
