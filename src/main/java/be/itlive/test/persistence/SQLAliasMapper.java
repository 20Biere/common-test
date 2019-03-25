package be.itlive.test.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author vbiertho
 *
 */
public class SQLAliasMapper {

    private static final Pattern SQL_PATTERN = Pattern.compile("select +(.*?) +from +(.*?)(?: +where +(.*?))?(?: +order by +(.*))?",
            Pattern.CASE_INSENSITIVE);

    private Map<String, String> tableNamesByAliases = new HashMap<>();

    private Map<String, ColumnReference> columnNamesByAliases = new HashMap<>();

    private static class ColumnReference {
        private final String tableAlias;

        private final String columnName;

        public ColumnReference(final String tableAlias, final String columnName) {
            this.tableAlias = tableAlias;
            this.columnName = columnName;
        }

        public String getTableAlias() {
            return tableAlias;
        }

        public String getColumnName() {
            return columnName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getTableAlias(), getColumnName());
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj instanceof SQLAliasMapper.ColumnReference) {
                SQLAliasMapper.ColumnReference other = (SQLAliasMapper.ColumnReference) obj;
                return Objects.equals(tableAlias, other.tableAlias) && Objects.equals(columnName, other.columnName);
            }
            return false;
        }
    }

    public void initilialiseFromSQL(final String sql) {
        tableNamesByAliases.clear();
        columnNamesByAliases.clear();
        Matcher sqlMatcher = SQL_PATTERN.matcher(sql);
        if (sqlMatcher.matches()) {
            String select = sqlMatcher.group(1);
            Matcher selectMatcher = Pattern.compile(" *(.*?)\\.(.*?)? +as +(.*?)(?:,|$)").matcher(select);
            while (selectMatcher.find()) {
                columnNamesByAliases.put(selectMatcher.group(3), new ColumnReference(selectMatcher.group(1), selectMatcher.group(2)));
            }
            String[] tokens = sqlMatcher.group(2).split(" +");
            int p = 0;
            while (p < tokens.length) {
                String table = tokens[p++];
                String alias = tokens[p++];
                tableNamesByAliases.put(alias, table);
                while (p < tokens.length && !"join".equals(tokens[p])) {
                    p++;
                }
                p++;
            }
        }
        System.out.println("--------------------------------------------------------------------");
        long id = Math.abs(new Random().nextLong());
        System.out.format("-- %019d -- SQL : %s%n", id, sql);
        for (Map.Entry<String, ColumnReference> entry : columnNamesByAliases.entrySet()) {
            System.out.format("-- %019d -- %s = %s%n", id, entry.getKey(),
                    tableNamesByAliases.get(entry.getValue().getTableAlias()) + "." + entry.getValue().getColumnName());
        }
    }

    public String getColumnNameFromAlias(final String alias) {
        ColumnReference columnReference = columnNamesByAliases.get(alias);
        if (columnReference != null) {
            String tableName = tableNamesByAliases.get(columnReference.getTableAlias());
            if (tableName != null) {
                return tableName + "." + columnReference.getColumnName();
            } else {
                return columnReference.getTableAlias() + "." + columnReference.getColumnName();
            }
        } else {
            return alias;
        }
    }

    @Test
    public void test() {
        initilialiseFromSQL(
                "select foreignadd0_.ID_RESIDENTIAL_ADDRESS as ID_RESID1_3_0_, foreignadd0_.CITY_NAME as CITY_NAM2_6_0_, foreignadd0_.STREET_NAME as STREET_N3_6_0_, foreignadd0_.LANGUAGE as LANGUAGE4_0_ from REGISPROXY.RESIDENTIAL_FOREIGN_ADDRESS foreignadd0_ where foreignadd0_.ID_RESIDENTIAL_ADDRESS=?");
        initilialiseFromSQL(
                "select this_.ID as ID2_3_0_, this_.VERSION as VERSION3_3_0_, this_.DB_CREATION_TIMESTAMP as DB_CREAT4_3_0_, this_.APPLICATION_CREATION_USERID as APPLICAT5_3_0_, this_.DB_TIMESTAMP as DB_TIMES6_3_0_, this_.APPLICATION_USERID as APPLICAT7_3_0_, this_.MAILBOX_NUMBER as MAILBOX_8_3_0_, this_.ID_PERSON as ID_PERS11_3_0_, this_.BEGIN_DATE as BEGIN_DA9_3_0_, this_.END_DATE as END_DAT10_3_0_, this_1_.BOX_NUMBER as BOX_NUMB1_5_0_, this_1_.CITY_CODE as CITY_COD2_5_0_, this_1_.COUNTRY_CODE as COUNTRY_3_5_0_, this_1_.HOUSE_NUMBER as HOUSE_NU4_5_0_, this_1_.POSTAL_CODE as POSTAL_C5_5_0_, this_1_.STREET_CODE as STREET_C6_5_0_, this_2_.ADDRESS as ADDRESS1_0_0_, this_2_.COUNTRY_CODE as COUNTRY_2_0_0_, this_2_.ID_DIPLOMATIC_POST_ADDRESS as ID_DIPLO6_0_0_, this_2_.POST_CODE as POST_COD3_0_0_, this_2_.POST_COUNTRY_CODE as POST_COU4_0_0_, this_.TYPE as TYPE1_3_0_ from REGISPROXY.REGISTER_ADDRESS this_ with(nolock)  left outer join REGISPROXY.RESIDENTIAL_ADDRESS this_1_ on this_.ID=this_1_.ID left outer join REGISPROXY.DIPLOMATIC_ADDRESS this_2_ on this_.ID=this_2_.ID where this_.ID_PERSON=? order by this_.BEGIN_DATE asc");
        initilialiseFromSQL(
                "select diplomatic0_.ID as ID1_2_0_, diplomatic0_.ADDRESS as ADDRESS2_2_0_, diplomatic0_.COUNTRY_CODE as COUNTRY_3_2_0_ from REGISPROXY.DIPLOMATIC_POST_ADDRESS diplomatic0_ with(nolock)  where diplomatic0_.ID=?");
    }

    public <T> Answer<T> captureSQLAndReturn(final int argumentPosition, final T ps1) {
        return new Answer<T>() {

            @Override
            public T answer(final InvocationOnMock invocation) throws Throwable {
                String sql = invocation.getArgument(argumentPosition);
                initilialiseFromSQL(sql);
                return ps1;
            }
        };
    }
}
