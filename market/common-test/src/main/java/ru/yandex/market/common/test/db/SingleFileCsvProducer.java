package ru.yandex.market.common.test.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Month;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.ObjectContext;
import org.apache.commons.lang.StringUtils;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultTableMetaData;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.stream.DefaultConsumer;
import org.dbunit.dataset.stream.IDataSetConsumer;
import org.dbunit.dataset.stream.IDataSetProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class SingleFileCsvProducer implements IDataSetProducer {

    private static final Logger log = LoggerFactory.getLogger(SingleFileCsvProducer.class);

    private static final char DEFAULT_COMMENT_MARKER = '#';
    private static final IDataSetConsumer EMPTY_CONSUMER = new DefaultConsumer();
    private IDataSetConsumer consumer = EMPTY_CONSUMER;


    private final InputStream inputStream;

    private final JexlEngine jexl;
    private final JexlContext jexlContext;
    private final Pattern jexlPattern;

    public SingleFileCsvProducer(InputStream inputStream) {
        this.inputStream = inputStream;
        jexl = new JexlBuilder().create();
        jexlContext = new ObjectContext<>(jexl, new Functions());
        jexlPattern = Pattern.compile("^\\$\\{(.*)}$");
    }

    @Override
    public void setConsumer(IDataSetConsumer consumer) throws DataSetException {
        this.consumer = consumer;
    }

    @Override
    public void produce() throws DataSetException {

        try (InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {
            consumer.startDataSet();
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withNullString("null")
                    .withIgnoreEmptyLines(false).withCommentMarker(DEFAULT_COMMENT_MARKER));
            Iterator<CSVRecord> iterator = csvParser.iterator();
            // Для проверки уникальности таблицы в пределах csv-файла
            Set<String> tableNames = new HashSet<>();
            while (true) {
                String tableName = readTableName(iterator);
                if (tableName == null) {
                    break;
                }

                if (!tableNames.add(tableName)) {
                    throw new RuntimeException("Table " + tableName + " already specified in csv file");
                }

                CSVRecord header = iterator.next();
                Column[] columns = new Column[header.size()];
                for (int i = 0; i < columns.length; i++) {
                    columns[i] = new Column(header.get(i), DataType.UNKNOWN);
                }
                ITableMetaData table = new DefaultTableMetaData(tableName, columns);
                consumer.startTable(table);
                log.debug("Reading table {}", tableName);
                while (iterator.hasNext()) {
                    CSVRecord record = iterator.next();
                    log.debug("{}", record);
                    if (isEmpty(record)) {
                        break;
                    }
                    consumer.row(getRow(record));
                }
                consumer.endTable();
            }

            consumer.endDataSet();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Читает название таблицы, пропуская пустые строки.
     *
     * @param iterator ввод
     * @return имя таблицы или <code>null</code>, если достигли конца ввода
     */
    private String readTableName(Iterator<CSVRecord> iterator) {
        while (iterator.hasNext()) {
            CSVRecord tableName = iterator.next();
            if (tableName.size() > 0 && StringUtils.isNotEmpty(tableName.get(0))) {
                return tableName.get(0);
            }
        }
        return null;
    }

    /**
     * Читает название таблицы, пропуская пустые строки.
     *
     * @param reader ввод
     * @return имя таблицы или <code>null</code>, если достигли конца ввода
     */
    @SuppressWarnings("unused")
    private String readTableName(BufferedReader reader) throws IOException {
        while (true) {
            String tableName = reader.readLine();
            if (tableName == null) {
                return null;
            }
            if (StringUtils.isNotBlank(tableName)) {
                return tableName;
            }

        }
    }

    private Object[] getRow(CSVRecord record) {
        Object[] row = new Object[record.size()];
        for (int i = 0; i < row.length; i++) {
            String value = record.get(i);
            Matcher matcher;
            if (value != null && (matcher = jexlPattern.matcher(value)).matches()) {
                row[i] = jexl.createExpression(matcher.group(1)).evaluate(jexlContext);
            } else {
                row[i] = value;
            }
        }
        return row;
    }

    private boolean isEmpty(CSVRecord record) {
        for (int i = 0; i < record.size(); i++) {
            if (StringUtils.isNotBlank(record.get(i))) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    public static class Functions {
        /**
         * Фиксированое время, которое инициализируется один раз на тест, чтобы одну и ту же дату можно было
         * независимо вставлять в разные таблицы.
         */
        private static final ZonedDateTime SINGLETON_NOW = ZonedDateTime.now();

        public static Timestamp sysdate() {
            return sysdate(0);
        }

        public static Timestamp sysdate(int days) {
            return sysdateByDateTime(SINGLETON_NOW, days, 0, 0, 0);
        }

        public static Timestamp sysdate(int days, int hours) {
            return sysdateByDateTime(SINGLETON_NOW, days, hours, 0, 0);
        }

        public static Timestamp sysdate(int days, int hours, int minutes) {
            return sysdateByDateTime(SINGLETON_NOW, days, hours, minutes, 0);
        }

        public static Timestamp sysdate(int days, int hours, int minutes, int seconds) {
            return sysdateByDateTime(SINGLETON_NOW, days, hours, minutes, seconds);
        }

        public static Timestamp shiftedsysdate(int shift, String unitType) {
            ChronoUnit unit = ChronoUnit.valueOf(unitType);
            switch (unit) {
                case YEARS:
                    return sysdateByDateTime(SINGLETON_NOW.plusYears(shift), 0, 0, 0, 0);
                case MONTHS:
                    return sysdateByDateTime(SINGLETON_NOW.plusMonths(shift), 0, 0, 0, 0);
                case DAYS:
                    return sysdateByDateTime(SINGLETON_NOW.plusDays(shift), 0, 0, 0, 0);
                default:
                    return sysdateByDateTime(SINGLETON_NOW, 0, 0, 0, 0);
            }
        }

        public static Timestamp truncedsysdate(int days) {
            return sysdateByDateTime(SINGLETON_NOW.truncatedTo(ChronoUnit.DAYS), days, 0, 0, 0);
        }

        public static Timestamp singletonSysdate(int days) {
            return sysdateByDateTime(SINGLETON_NOW, days, 0, 0, 0);
        }

        public static Timestamp singletonSysdate(int days, int hours) {
            return sysdateByDateTime(SINGLETON_NOW, days, hours, 0, 0);
        }

        /**
         * Округлить текущую дату до указаных единиц.
         * @param unitString Единица для округления. Название поля из ChronoUnit
         * @return Timestamp, округленный до нужных единиц
         */
        public static Timestamp truncedsysdatetounit(String unitString) {
            ChronoUnit unit = ChronoUnit.valueOf(unitString);
            switch (unit) {
                case YEARS: {
                    return Timestamp.from(SINGLETON_NOW.truncatedTo(ChronoUnit.DAYS)
                            .withMonth(Month.JANUARY.getValue()).withDayOfMonth(1).toInstant());
                }
                case MONTHS: {
                    return Timestamp.from(SINGLETON_NOW.truncatedTo(ChronoUnit.DAYS)
                            .withDayOfMonth(1).toInstant());
                }
                default: {
                    return sysdateByDateTime(SINGLETON_NOW.truncatedTo(unit),
                            0, 0, 0, 0);
                }
            }
        }

        public static long epochSecond(Timestamp time) {
            if (time == null) {
                return 0L;
            }

            return time.toInstant().getEpochSecond();
        }

        private static Timestamp sysdateByDateTime(ZonedDateTime dateTime, int days,
                                                   int hours, int minutes, int seconds) {
            return new Timestamp(dateTime
                    .plusDays(days)
                    .plusHours(hours)
                    .plusMinutes(minutes)
                    .plusSeconds(seconds)
                    .toInstant()
                    .toEpochMilli());
        }

        public static String file(String path) throws IOException {
            String s = new String(Files.readAllBytes(new ClassPathResource(path).getFile().toPath()));
            return s.trim();
        }
    }
}
