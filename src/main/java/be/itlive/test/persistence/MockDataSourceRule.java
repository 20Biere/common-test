package be.itlive.test.persistence;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Arrays;

import javax.sql.DataSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xml.sax.InputSource;

import com.google.common.annotations.Beta;
import com.google.common.base.Supplier;

/**
 * <p>
 * This rule create a mock {@link DataSource} which can be use with hibernate framework during tests.<br/>
 * Typically this rule is used with {@link MockJNDIContextRule} to be registered in a fake JNDI that hibernate will lookup.<br/>
 * </p>
 * <p>
 * Declaration of the rule need two parameters, The list of SQLKeyword for the mocked DB and information about datatypes available in the mocked DB.
 * For ease of use those parameter are available for SQLServer2012 and DB2 in the constants
 * {@link MockDataSourceRule#DB2_KEYWORDS DB2_KEYWORDS},
 * {@link MockDataSourceRule#DB2_TYPE_INFOS DB2_TYPE_INFOS},
 * {@link MockDataSourceRule#SQLSERVER_KEYWORDS SQLSERVER_KEYWORDS},
 * {@link MockDataSourceRule#SQLSERVER_TYPE_INFOS SQLSERVER_TYPE_INFOS}.
 * <br/>
 * </p>
 * <p>For ease of use with {@link MockJNDIContextRule}, this rule implements {@link Supplier} which return the {@link DataSource} mock.<br />
 * Example :
 * <pre>
 * {@literal @}Rule
 * public {@link MockDataSourceRule} datasourceRule = new {@link MockDataSourceRule}({@link MockDataSourceRule#SQLSERVER_KEYWORDS }, {@link MockDataSourceRule#SQLSERVER_TYPE_INFOS });
 *
 * {@literal @}Rule
 * public {@link MockJNDIContextRule} jndiRule = new {@link MockJNDIContextRule}().with("java:/applicationDS", datasourceRule);
 * </pre>
 * </p>
 * <p>
 *
 *
 * The following behaviors is configured by the rule :
 * <ul>
 * <li>The {@link DataSource#getConnection()} and {@link DataSource#getConnection(String, String)} methods are mocked to return a {@link Connection} mock (available through {@link MockDataSourceRule#getConnectionMock()}).</li>
 * <li>The {@link Connection#getMetaData()} method is also mocked to return a {@link DatabaseMetaData} mock (available through {@link MockDataSourceRule#getMetadataMock()}).</li>
 * <li>The {@link DatabaseMetaData#getTypeInfo()} method is mocked to return the types given in rule constructor.</li>
 * <li>The {@link DatabaseMetaData#getSQLKeywords()} method is mocked to return the keywords given in rule constructor.</li>
 * </ul>
 * </p>
 * @author vbiertho
 *
 */
@Beta
public class MockDataSourceRule implements TestRule, Supplier<DataSource> {
    /**
     * This is the info about datatypes supported by the DB2 database.
     */
    public static final String DB2_TYPE_INFOS = "TYPE_NAME,12,VARCHAR,java.lang.String,128,0,;DATA_TYPE,5,SMALLINT,java.lang.Integer,5,0,;"
            + "PRECISION,4,INTEGER,java.lang.Integer,10,0,;LITERAL_PREFIX,12,VARCHAR,java.lang.String,128,0,;"
            + "LITERAL_SUFFIX,12,VARCHAR,java.lang.String,128,0,;CREATE_PARAMS,12,VARCHAR,java.lang.String,128,0,;"
            + "NULLABLE,5,SMALLINT,java.lang.Integer,5,0,;CASE_SENSITIVE,5,SMALLINT,java.lang.Integer,5,0,;"
            + "SEARCHABLE,5,SMALLINT,java.lang.Integer,5,0,;UNSIGNED_ATTRIBUTE,5,SMALLINT,java.lang.Integer,5,0,;"
            + "FIXED_PREC_SCALE,5,SMALLINT,java.lang.Integer,5,0,;AUTO_INCREMENT,5,SMALLINT,java.lang.Integer,5,0,;"
            + "LOCAL_TYPE_NAME,12,VARCHAR,java.lang.String,128,0,;MINIMUM_SCALE,5,SMALLINT,java.lang.Integer,5,0,;"
            + "MAXIMUM_SCALE,5,SMALLINT,java.lang.Integer,5,0,;SQL_DATA_TYPE,4,INTEGER,java.lang.Integer,10,0,;"
            + "SQL_DATETIME_SUB,4,INTEGER,java.lang.Integer,10,0,;NUM_PREC_RADIX,4,INTEGER,java.lang.Integer,10,0,;\r\n"
            + "BIGINT;-5;20;NULL;NULL;NULL;1;0;2;0;1;0;NULL;0;0;-5;NULL;10;\r\n"
            + "LONG VARCHAR FOR BIT DATA;-4;32700;';';NULL;1;0;0;NULL;0;NULL;NULL;NULL;NULL;-4;NULL;NULL;\r\n"
            + "VARCHAR () FOR BIT DATA;-3;32762;';';LENGTH;1;0;3;NULL;0;NULL;NULL;NULL;NULL;-3;NULL;NULL;\r\n"
            + "CHAR () FOR BIT DATA;-2;254;';';LENGTH;1;0;3;NULL;0;NULL;NULL;NULL;NULL;-2;NULL;NULL;\r\n"
            + "LONG VARCHAR;-1;32700;';';LENGTH;1;1;1;NULL;0;NULL;NULL;NULL;NULL;-1;NULL;NULL;\r\n"
            + "LONG VARGRAPHIC;-1;16350;G';';NULL;1;0;1;NULL;0;NULL;NULL;NULL;NULL;-97;NULL;NULL;\r\n"
            + "CHAR;1;254;';';LENGTH;1;1;3;NULL;0;NULL;NULL;NULL;NULL;1;NULL;NULL;\r\n"
            + "GRAPHIC;1;127;G';';LENGTH;1;0;3;NULL;0;NULL;NULL;NULL;NULL;-95;NULL;NULL;\r\n"
            + "DECIMAL;3;31;NULL;NULL;PRECISION,SCALE;1;0;2;0;0;0;NULL;0;31;3;NULL;10;\r\n"
            + "INTEGER;4;10;NULL;NULL;NULL;1;0;2;0;1;0;NULL;0;0;4;NULL;10;\r\n" + "SMALLINT;5;5;NULL;NULL;NULL;1;0;2;0;1;0;NULL;0;0;5;NULL;10;\r\n"
            + "REAL;7;24;NULL;NULL;NULL;1;0;2;0;0;0;NULL;0;0;7;NULL;2;\r\n" + "DOUBLE;8;53;NULL;NULL;NULL;1;0;2;0;0;0;NULL;0;0;8;NULL;2;\r\n"
            + "VARCHAR;12;4000;';';LENGTH;1;1;3;NULL;0;NULL;NULL;NULL;NULL;12;NULL;NULL;\r\n"
            + "VARGRAPHIC;12;16336;G';';LENGTH;1;0;3;NULL;0;NULL;NULL;NULL;NULL;-96;NULL;NULL;\r\n"
            + "BOOLEAN;16;NULL;NULL;NULL;NULL;1;1;0;NULL;0;NULL;NULL;NULL;NULL;16;NULL;NULL;\r\n"
            + "DATE;91;10;';';NULL;1;0;2;NULL;0;NULL;NULL;NULL;NULL;9;1;NULL;\r\n" + "TIME;92;8;';';NULL;1;0;2;NULL;0;NULL;NULL;0;0;9;2;NULL;\r\n"
            + "TIMESTAMP;93;32;';';NULL;1;0;2;NULL;0;NULL;NULL;0;12;9;3;NULL;\r\n"
            + "DECFLOAT;1111;34;NULL;NULL;PRECISION;1;0;2;0;0;0;NULL;0;0;-360;NULL;10;\r\n"
            + "XML;1111;NULL;NULL;NULL;NULL;1;1;0;NULL;0;NULL;NULL;NULL;NULL;-370;NULL;NULL;\r\n"
            + "DISTINCT;2001;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;\r\n"
            + "ROW;2002;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;19;NULL;NULL;\r\n"
            + "ARRAY;2003;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;NULL;\r\n"
            + "BLOB;2004;2147483647;NULL;NULL;LENGTH;1;0;1;NULL;0;NULL;NULL;NULL;NULL;-98;NULL;NULL;\r\n"
            + "CLOB;2005;2147483647;';';LENGTH;1;1;1;NULL;0;NULL;NULL;NULL;NULL;-99;NULL;NULL;\r\n"
            + "DBCLOB;2005;1073741823;';';LENGTH;1;1;1;NULL;0;NULL;NULL;NULL;NULL;-350;NULL;NULL;";

    /**
     * This is the info about datatypes supported by SQLServer 2012.
     */
    public static final String SQLSERVER_TYPE_INFOS = "TYPE_NAME,-9,nvarchar,java.lang.String,128,0,;"
            + "DATA_TYPE,5,smallint,java.lang.Short,5,0,;PRECISION,4,int,java.lang.Integer,10,0,;"
            + "LITERAL_PREFIX,12,varchar,java.lang.String,32,0,;LITERAL_SUFFIX,12,varchar,java.lang.String,32,0,;"
            + "CREATE_PARAMS,12,varchar,java.lang.String,32,0,;NULLABLE,5,smallint,java.lang.Short,5,0,;"
            + "CASE_SENSITIVE,5,smallint,java.lang.Short,5,0,;SEARCHABLE,5,smallint,java.lang.Short,5,0,;"
            + "UNSIGNED_ATTRIBUTE,5,smallint,java.lang.Short,5,0,;FIXED_PREC_SCALE,5,smallint,java.lang.Short,5,0,;"
            + "AUTO_INCREMENT,5,smallint,java.lang.Short,5,0,;LOCAL_TYPE_NAME,-9,nvarchar,java.lang.String,128,0,;"
            + "MINIMUM_SCALE,5,smallint,java.lang.Short,5,0,;MAXIMUM_SCALE,5,smallint,java.lang.Short,5,0,;"
            + "SQL_DATA_TYPE,5,smallint,java.lang.Short,5,0,;SQL_DATETIME_SUB,5,smallint,java.lang.Short,5,0,;"
            + "NUM_PREC_RADIX,4,int,java.lang.Integer,10,0,;INTERVAL_PRECISION,5,smallint,java.lang.Short,5,0,;\r\n"
            + "sql_variant;-150;8000;NULL;NULL;NULL;1;0;2;NULL;0;NULL;sql_variant;0;0;-150;NULL;10;NULL;\r\n"
            + "uniqueidentifier;1;36;';';NULL;1;0;2;NULL;0;NULL;uniqueidentifier;NULL;NULL;-11;NULL;NULL;NULL;\r\n"
            + "ntext;-16;1073741823;N';';NULL;1;0;1;NULL;0;NULL;ntext;NULL;NULL;-10;NULL;NULL;NULL;\r\n"
            + "xml;-16;1073741823;N';';NULL;1;1;0;NULL;0;NULL;xml;NULL;NULL;-10;NULL;NULL;NULL;\r\n"
            + "nvarchar;-9;4000;N';';max length;1;0;3;NULL;0;NULL;nvarchar;NULL;NULL;-9;NULL;NULL;NULL;\r\n"
            + "sysname;-9;128;N';';NULL;0;0;3;NULL;0;NULL;sysname;NULL;NULL;-9;NULL;NULL;NULL;\r\n"
            + "date;-9;10;';';NULL;1;0;3;NULL;0;NULL;date;NULL;NULL;-9;NULL;NULL;NULL;\r\n"
            + "time;-9;16;';';NULL;1;0;3;NULL;0;NULL;time;NULL;NULL;-9;NULL;NULL;NULL;\r\n"
            + "datetime2;-9;27;';';NULL;1;0;3;NULL;0;NULL;datetime2;NULL;NULL;-9;NULL;NULL;NULL;\r\n"
            + "datetimeoffset;-9;34;';';NULL;1;0;3;NULL;0;NULL;datetimeoffset;NULL;NULL;-9;NULL;NULL;NULL;\r\n"
            + "nchar;-15;4000;N';';length;1;0;3;NULL;0;NULL;nchar;NULL;NULL;-8;NULL;NULL;NULL;\r\n"
            + "bit;-7;1;NULL;NULL;NULL;1;0;2;NULL;0;NULL;bit;0;0;-7;NULL;NULL;NULL;\r\n"
            + "tinyint;-6;3;NULL;NULL;NULL;1;0;2;1;0;0;tinyint;0;0;-6;NULL;10;NULL;\r\n"
            + "booltype;-6;3;NULL;NULL;NULL;1;0;2;1;0;0;booltype;0;0;-6;NULL;10;NULL;\r\n"
            + "tinyint identity;-6;3;NULL;NULL;NULL;0;0;2;1;0;1;tinyint identity;0;0;-6;NULL;10;NULL;\r\n"
            + "bigint;-5;19;NULL;NULL;NULL;1;0;2;0;0;0;bigint;0;0;-5;NULL;10;NULL;\r\n"
            + "bigint identity;-5;19;NULL;NULL;NULL;0;0;2;0;0;1;bigint identity;0;0;-5;NULL;10;NULL;\r\n"
            + "image;-4;2147483647;0x;NULL;NULL;1;0;0;NULL;0;NULL;image;NULL;NULL;-4;NULL;NULL;NULL;\r\n"
            + "varbinary;-3;8000;0x;NULL;max length;1;0;2;NULL;0;NULL;varbinary;NULL;NULL;-3;NULL;NULL;NULL;\r\n"
            + "binary;-2;8000;0x;NULL;length;1;0;2;NULL;0;NULL;binary;NULL;NULL;-2;NULL;NULL;NULL;\r\n"
            + "timestamp;-2;8;0x;NULL;NULL;0;0;2;NULL;0;NULL;timestamp;NULL;NULL;-2;NULL;NULL;NULL;\r\n"
            + "text;-1;2147483647;';';NULL;1;0;1;NULL;0;NULL;text;NULL;NULL;-1;NULL;NULL;NULL;\r\n"
            + "char;1;8000;';';length;1;0;3;NULL;0;NULL;char;NULL;NULL;1;NULL;NULL;NULL;\r\n"
            + "numeric;2;38;NULL;NULL;precision,scale;1;0;2;0;0;0;numeric;0;38;2;NULL;10;NULL;\r\n"
            + "numeric() identity;2;38;NULL;NULL;precision;0;0;2;0;0;1;numeric() identity;0;0;2;NULL;10;NULL;\r\n"
            + "decimal;3;38;NULL;NULL;precision,scale;1;0;2;0;0;0;decimal;0;38;3;NULL;10;NULL;\r\n"
            + "money;3;19;$;NULL;NULL;1;0;2;0;1;0;money;4;4;3;NULL;10;NULL;\r\n"
            + "smallmoney;3;10;$;NULL;NULL;1;0;2;0;1;0;smallmoney;4;4;3;NULL;10;NULL;\r\n"
            + "decimal() identity;3;38;NULL;NULL;precision;0;0;2;0;0;1;decimal() identity;0;0;3;NULL;10;NULL;\r\n"
            + "int;4;10;NULL;NULL;NULL;1;0;2;0;0;0;int;0;0;4;NULL;10;NULL;\r\n"
            + "int identity;4;10;NULL;NULL;NULL;0;0;2;0;0;1;int identity;0;0;4;NULL;10;NULL;\r\n"
            + "smallint;5;5;NULL;NULL;NULL;1;0;2;0;0;0;smallint;0;0;5;NULL;10;NULL;\r\n"
            + "smallint identity;5;5;NULL;NULL;NULL;0;0;2;0;0;1;smallint identity;0;0;5;NULL;10;NULL;\r\n"
            + "float;8;53;NULL;NULL;NULL;1;0;2;0;0;0;float;NULL;NULL;6;NULL;2;NULL;\r\n"
            + "real;7;24;NULL;NULL;NULL;1;0;2;0;0;0;real;NULL;NULL;7;NULL;2;NULL;\r\n"
            + "varchar;12;8000;';';max length;1;0;3;NULL;0;NULL;varchar;NULL;NULL;12;NULL;NULL;NULL;\r\n"
            + "datetime;93;23;';';NULL;1;0;3;NULL;0;NULL;datetime;3;3;9;3;NULL;NULL;\r\n"
            + "smalldatetime;93;16;';';NULL;1;0;3;NULL;0;NULL;smalldatetime;0;0;9;3;NULL;NULL;\r\n" + "";

    /**
     * This is the reserved keyword for SQLServer 2012.
     */
    public static final String SQLSERVER_KEYWORDS = "BACKUP,BREAK,BROWSE,BULK,CHECKPOINT,CLUSTERED,COMPUTE,CONTAINS,CONTAINSTABLE"
            + ",DATABASE,DBCC,DENY,DISK,DISTRIBUTED,DUMMY,DUMP,ERRLVL,EXIT,FILE,FILLFACTOR,FREETEXT,FREETEXTTABLE,FUNCTION,"
            + "HOLDLOCK,IDENTITY_INSERT,IDENTITYCOL,IF,KILL,LINENO,LOAD,NOCHECK,NONCLUSTERED,OFF,OFFSETS,OPENDATASOURCE,OPENQUERY"
            + ",OPENROWSET,OPENXML,OVER,PERCENT,PLAN,PRINT,PROC,RAISERROR,READTEXT,RECONFIGURE,REPLICATION,RESTORE,RETURN,ROWCOUNT"
            + ",ROWGUIDCOL,RULE,SAVE,SETUSER,SHUTDOWN,STATISTICS,TEXTSIZE,TOP,TRAN,TRIGGER,TRUNCATE,TSEQUAL,UPDATETEXT,USE,WAITFOR"
            + ",WHILE,WRITETEXT";

    /**
     * This is the reserved keyword for DB2.
     */
    public static final String DB2_KEYWORDS = "AFTER,ALIAS,ALLOW,APPLICATION,ASSOCIATE,ASUTIME,AUDIT,AUX,AUXILIARY,BEFORE,BINARY,"
            + "BUFFERPOOL,CACHE,CALL,CALLED,CAPTURE,CARDINALITY,CCSID,CLUSTER,COLLECTION,COLLID,COMMENT,CONCAT,CONDITION,CONTAINS,"
            + "COUNT_BIG,CURRENT_LC_CTYPE,CURRENT_PATH,CURRENT_SERVER,CURRENT_TIMEZONE,CYCLE,DATA,DATABASE,DAYS,DB2GENERAL,"
            + "DB2GENRL,DB2SQL,DBINFO,DEFAULTS,DEFINITION,DETERMINISTIC,DISALLOW,DO,DSNHATTR,DSSIZE,DYNAMIC,EACH,EDITPROC,ELSEIF,"
            + "ENCODING,END-EXEC1,ERASE,EXCLUDING,EXIT,FENCED,FIELDPROC,FILE,FINAL,FREE,FUNCTION,GENERAL,GENERATED,GRAPHIC,HANDLER,"
            + "HOLD,HOURS,IF,INCLUDING,INCREMENT,INDEX,INHERIT,INOUT,INTEGRITY,ISOBID,ITERATE,JAR,JAVA,LABEL,LC_CTYPE,LEAVE,"
            + "LINKTYPE,LOCALE,LOCATOR,LOCATORS,LOCK,LOCKMAX,LOCKSIZE,LONG,LOOP,MAXVALUE,MICROSECOND,MICROSECONDS,MINUTES,MINVALUE,"
            + "MODE,MODIFIES,MONTHS,NEW,NEW_TABLE,NOCACHE,NOCYCLE,NODENAME,NODENUMBER,NOMAXVALUE,NOMINVALUE,NOORDER,NULLS,NUMPARTS,"
            + "OBID,OLD,OLD_TABLE,OPTIMIZATION,OPTIMIZE,OUT,OVERRIDING,PACKAGE,PARAMETER,PART,PARTITION,PATH,PIECESIZE,PLAN,PRIQTY,"
            + "PROGRAM,PSID,QUERYNO,READS,RECOVERY,REFERENCING,RELEASE,RENAME,REPEAT,RESET,RESIGNAL,RESTART,RESULT,"
            + "RESULT_SET_LOCATOR,RETURN,RETURNS,ROUTINE,ROW,RRN,RUN,SAVEPOINT,SCRATCHPAD,SECONDS,SECQTY,SECURITY,SENSITIVE,SIGNAL,"
            + "SIMPLE,SOURCE,SPECIFIC,SQLID,STANDARD,START,STATIC,STAY,STOGROUP,STORES,STYLE,SUBPAGES,SYNONYM,SYSFUN,SYSIBM,SYSPROC,"
            + "SYSTEM,TABLESPACE,TRIGGER,TYPE,UNDO,UNTIL,VALIDPROC,VARIABLE,VARIANT,VCAT,VOLUMES,WHILE,WLM,YEARS";

    private DataSource dataSource;

    private Connection connection;

    private DatabaseMetaData metadata;

    private String sqlKeywords;

    private ResultSet typeInfo;

    /**
     * @param sqlKeywords comma separated list of keywords.
     * @param typeInfo info about types supported by the database. as generated by {@link TypeInfoResultSet#serializeInformation(ResultSet)}
     */
    public MockDataSourceRule(final String sqlKeywords, final ResultSet typeInfo, final MockSettings settings) {
        this.sqlKeywords = sqlKeywords;
        this.typeInfo = typeInfo;
        dataSource = mock(DataSource.class, settings);
        connection = mock(Connection.class, Mockito.withSettings().defaultAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                if ("prepareStatement".equals(invocation.getMethod().getName())) {
                    System.out.println("UnStubbed call to {" + invocation.getMethod() + "} with " + Arrays.toString(invocation.getArguments()));
                } else if ("prepareCall".equals(invocation.getMethod().getName())) {
                    System.out.println("UnStubbed call to {" + invocation.getMethod() + "} with " + Arrays.toString(invocation.getArguments()));
                }
                return Mockito.RETURNS_DEFAULTS.answer(invocation);
            }
        }));

        metadata = mock(DatabaseMetaData.class, settings);
    }

    /**
     * @param sqlKeywords comma separated list of keywords.
     * @param typeInfo info about types supported by the database. as generated by {@link TypeInfoResultSet#serializeInformation(ResultSet)}
     */
    public MockDataSourceRule(final String sqlKeywords, final ResultSet typeInfo) {
        this(sqlKeywords, typeInfo, Mockito.withSettings());
    }

    /**
     * @param sqlKeywords comma separated list of keywords.
     * @param typeInfo info about types supported by the database. as generated by {@link TypeInfoResultSet#serializeInformation(ResultSet)}
     */
    public MockDataSourceRule(final String sqlKeywords, final String typeInfo, final MockSettings settings) {
        this(sqlKeywords, new TypeInfoResultSet(typeInfo), settings);
    }

    /**
     * @param sqlKeywords comma separated list of keywords.
     * @param typeInfo info about types supported by the database. as generated by {@link TypeInfoResultSet#serializeInformation(ResultSet)}
     */
    public MockDataSourceRule(final String sqlKeywords, final String typeInfo) {
        this(sqlKeywords, new TypeInfoResultSet(typeInfo), Mockito.withSettings());
    }

    @Override
    public org.junit.runners.model.Statement apply(final org.junit.runners.model.Statement base, final Description description) {
        return new org.junit.runners.model.Statement() {

            @Override
            public void evaluate() throws Throwable {
                try {
                    when(dataSource.getConnection()).thenReturn(connection);
                    when(dataSource.getConnection(anyString(), anyString())).thenReturn(connection);
                    when(connection.getMetaData()).thenReturn(metadata);
                    when(metadata.getTypeInfo()).thenReturn(typeInfo);
                    when(metadata.getSQLKeywords()).thenReturn(sqlKeywords);
                    base.evaluate();
                } finally {
                    reset(dataSource, connection, metadata);
                }
            }

        };
    }

    @Override
    public DataSource get() {
        return dataSource;
    }

    public Connection getConnectionMock() {
        return connection;
    }

    public DatabaseMetaData getMetadataMock() {
        return metadata;
    }

}
