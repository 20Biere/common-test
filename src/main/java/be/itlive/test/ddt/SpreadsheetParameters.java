package be.itlive.test.ddt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.google.common.base.Preconditions;

/**
 *  This class allow to load data for parameterized test from a excel spreadsheet.
 *  Each line is a test. And value are put in a map. The test need to specify the name to given to value from each column (null can be used to skip a column)
 *
 *
 * @author vbiertho
 *
 */
public class SpreadsheetParameters {

    private final int headerSize;

    private final List<String> parametersNames;

    private final String sheetName;

    private final int nameIndex;

    private List<Parameters> testParameters = null;

    /**
     *
     * @param headerSize number of line to skip at the start of the spreadsheet.
     * @param sheetName name of the sheet to use in the workbook. null means first one.
     * @param parametersNames name to given to value of different columns, add null item in the list to skip columns.
     * @param nameIndex index of the property that will be returned as toString() of the returned {@link Parameters}
     */
    public SpreadsheetParameters(final int headerSize, final String sheetName, final List<String> parametersNames, final int nameIndex) {
        Preconditions.checkArgument(parametersNames.size() > nameIndex);
        this.headerSize = headerSize;
        this.sheetName = sheetName;
        this.parametersNames = parametersNames;
        this.nameIndex = nameIndex;
    }

    /**
     *
     * @param stream workbook stream.
     * @throws IOException
     * @throws InvalidFormatException
     * @throws EncryptedDocumentException
     */
    private void load(final InputStream stream) throws IOException, EncryptedDocumentException, InvalidFormatException {

        List<Parameters> testParameters = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(stream);) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = sheetName == null ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
            int lastRowIndex = sheet.getLastRowNum();
            for (int rowIndex = headerSize; rowIndex < lastRowIndex; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                Parameters data = new Parameters(parametersNames.get(nameIndex));
                for (int colIndex = 0; colIndex < parametersNames.size(); colIndex++) {
                    String paramName = parametersNames.get(colIndex);
                    if (StringUtils.isNotBlank(paramName)) {
                        Cell cell = row.getCell(colIndex, Row.RETURN_BLANK_AS_NULL);
                        cell = evaluator.evaluateInCell(cell);
                        Object value = getCellValue(cell);
                        data.put(paramName, value);
                    }
                }
                testParameters.add(data);
            }
        }

        this.testParameters = Collections.unmodifiableList(testParameters);

    }

    /**
     *
     * @param cell cell from which to take the value
     * @return value in the cell
     */
    private static Object getCellValue(final Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
        case Cell.CELL_TYPE_BLANK:
            return null;
        case Cell.CELL_TYPE_BOOLEAN:
            return cell.getBooleanCellValue();
        case Cell.CELL_TYPE_STRING:
            return cell.getStringCellValue();
        case Cell.CELL_TYPE_NUMERIC:
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            } else {
                return cell.getNumericCellValue();
            }
        default:
            return null;
        }
    }

    /**
     * @return list of parameters, one for each test.
     */
    public List<Parameters> getTestParameters() {
        return testParameters;
    }

    /**
     * @return list of pair test-name, test-parameters.
     */
    public List<Object[]> getTestNamesAndParameters() {
        List<Object[]> list = new ArrayList<>();
        for (Parameters p : testParameters) {
            list.add(new Object[] {p.getName(), p});
        }
        return list;
    }

    /**
     * @return a new instance of {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     *
     * @author vbiertho
     *
     */
    public static class Builder {

        private int headerSize = 1;

        private String nameParameter;

        private Integer nameIndex = 0;

        private String sheetName;

        private Path sourcePath;

        private URL sourceURL;

        private InputStream sourceStream;

        private String sourceResource;

        private Class<?> sourceResouceBase;

        private List<String> parameterNames = new ArrayList<>();

        /**
         * @param names name of parameters to append to the list of parameters
         * @return this
         */
        public Builder addParameters(final String... names) {
            for (String name : names) {
                parameterNames.add(name);
            }
            return this;
        }

        /**
         * @param name name of the parameter to use as "name" for the test and that is returned as {@link Parameters#toString()}.
         * @return this
         */
        public Builder nameParameter(final String name) {
            nameParameter = name;
            nameIndex = null;
            return this;
        }

        /**
         * @param index index of the parameter to use as "name" for the test and that is returned as {@link Parameters#toString()}.
         * @return this
         */
        public Builder nameParameter(final int index) {
            nameParameter = null;
            nameIndex = index;
            return this;
        }

        /**
         * @param sheetName name of the spreadsheet to use in the workbook.
         * @return this
         */
        public Builder sheetName(final String sheetName) {
            this.sheetName = sheetName;
            return this;
        }

        /**
         * @param path path of the workbook to use
         * @return this
         */
        public Builder source(final Path path) {
            Preconditions.checkArgument(path != null, "Source can not be null");
            sourcePath = path;
            sourceURL = null;
            sourceStream = null;
            sourceResouceBase = null;
            sourceResource = null;
            return this;
        }

        /**
         * @param url url of the workbook to use
         * @return this
         */
        public Builder source(final URL url) {
            Preconditions.checkArgument(url != null, "Source can not be null");
            sourcePath = null;
            sourceURL = url;
            sourceStream = null;
            sourceResouceBase = null;
            sourceResource = null;
            return this;
        }

        /**
         * @param stream stream of the workbook to use
         * @return this
         */
        public Builder source(final InputStream stream) {
            Preconditions.checkArgument(stream != null, "Source can not be null");
            sourcePath = null;
            sourceURL = null;
            sourceStream = stream;
            sourceResouceBase = null;
            sourceResource = null;
            return this;
        }

        /**
         * @param resource resource path of the workbook to use
         * @param base class on which {@link Class#getResourceAsStream(String)} will be called.
         * @return this
         */
        public Builder source(final String resource, final Class<?> base) {
            Preconditions.checkArgument(resource != null, "Source can not be null");
            sourcePath = null;
            sourceURL = null;
            sourceStream = null;
            sourceResouceBase = base;
            sourceResource = resource;
            return this;
        }

        /**
         * @param headerSize number of row to skip at the begining of the file. (default: 1)
         * @return this
         */
        public Builder headerSize(final int headerSize) {
            this.headerSize = headerSize;
            return this;
        }

        /**
         * @return new SpreadsheetParameters instance.
         * @throws IOException if the url/path/resource is not found or can not be opened.
         */
        public SpreadsheetParameters build() throws IOException {
            if (nameParameter != null) {
                Preconditions.checkState(parameterNames.contains(nameParameter));
                this.nameIndex = parameterNames.indexOf(nameParameter);
                this.nameParameter = null;
            } else if (this.nameIndex != null) {
                Preconditions.checkState(parameterNames.size() > nameIndex);
            } else {
                Preconditions.checkState(!parameterNames.isEmpty());
                this.nameIndex = 0;
            }

            SpreadsheetParameters spreadsheetParameters = new SpreadsheetParameters(headerSize, sheetName, parameterNames, nameIndex);
            try (InputStream stream = openInputStream()) {
                spreadsheetParameters.load(stream);
            } catch (EncryptedDocumentException | InvalidFormatException e) {
                throw new IOException(e);
            }
            return spreadsheetParameters;
        }

        /**
         *
         * @return source intput stream.
         * @throws IOException if the url/path/resource is not found or can not be opened.
         */
        private InputStream openInputStream() throws IOException {
            InputStream stream = sourceStream;
            if (stream == null && sourcePath != null) {
                stream = Files.newInputStream(sourcePath, StandardOpenOption.READ);
            }
            if (stream == null && sourceURL != null) {
                stream = sourceURL.openStream();
            }
            if (stream == null && sourceResource != null) {
                Class<?> base = (sourceResouceBase != null) ? sourceResouceBase : SpreadsheetParameters.class;
                stream = base.getResourceAsStream(sourceResource);
                if (stream == null) {
                    throw new FileNotFoundException("resource not found : '" + sourceResource + "' relative to '" + base + "'");
                }
            }
            if (stream == null) {
                throw new IllegalStateException("No source configured");
            }
            return stream;
        }

    }

    /**
     * This class is mainly a map with string key, but with getter helping to cast values to the correct type.
     *
     * @author vbiertho
     *
     */
    public static class Parameters extends HashMap<String, Object> {

        /**
         *
         */
        private static final long serialVersionUID = 89031030951670867L;

        private final String nameProperty;

        /**
         * @param nameProperty name of the property that is used to generate toString()
         */
        public Parameters(final String nameProperty) {
            this.nameProperty = nameProperty;
        }

        public Object getName() {
            return get(nameProperty);
        }

        /**
         * @param name name of the property
         * @return value of the property
         */
        public String getString(final String name) {
            return (String) get(name);
        }

        /**
         * @param name name of the property
         * @return value of the property
         */
        public Date getDate(final String name) {
            return (Date) get(name);
        }

        /**
         * @param name name of the property
         * @return value of the property
         */
        public Number getNumber(final String name) {
            return (Number) get(name);
        }

        /**
         * @param name name of the property
         * @return value of the property
         */
        public Long getLong(final String name) {
            Number n = getNumber(name);
            if (n == null) {
                return null;
            } else {
                return n.longValue();
            }
        }

        /**
         * @param name name of the property
         * @return value of the property
         */
        public Integer getInteger(final String name) {
            Number n = getNumber(name);
            if (n == null) {
                return null;
            } else {
                return n.intValue();
            }
        }

        /**
         * @param name name of the property
         * @return value of the property
         */
        public Double getDouble(final String name) {
            Number n = getNumber(name);
            if (n == null) {
                return null;
            } else {
                return n.doubleValue();
            }
        }

        /**
         * @param name name of the property
         * @return value of the property
         */
        public Boolean getBoolean(final String name) {
            return (Boolean) get(name);
        }

        @Override
        public String toString() {
            return String.valueOf(getName());
        }
    }
}
