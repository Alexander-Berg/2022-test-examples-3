package steps.dbfsteps;

import java.util.ArrayList;
import java.util.List;

import com.linuxense.javadbf.DBFDataType;
import com.linuxense.javadbf.DBFField;

public class DBFReaderSteps {
    public static final String ELM_CHECK_TRUE = "ДА";
    public static final String ELM_CHECK_FALSE = "НЕТ";
    public static final String RPO1 = "123";
    public static final String RPO2 = "234";
    public static final String RPO3 = "345";
    public static final String RPO4 = "456";
    public static final String RPO5 = "567";
    public static final Double PAYSUM1 = 123.00;
    public static final Double PAYSUM2 = 234.00;
    public static final Double PAYSUM3 = 345.00;
    private static final String ELM_CHECK_FIELD = "ELCHECK";
    private static final String PAY_SUM_FIELD = "PAYSUM";
    private static final String MSG_FIELD = "MSG";
    private static final String RPO_PROPERTY = "RPO";
    private static final Integer FIELD_LN = 20;

    private DBFReaderSteps() {
        throw new UnsupportedOperationException();
    }

    public static List<Object[]> createRows() {
        List<Object[]> list = new ArrayList<>();

        list.add(List.of(ELM_CHECK_TRUE, PAYSUM1, (RPO_PROPERTY + "=" + RPO1), "test").toArray());
        list.add(List.of(ELM_CHECK_FALSE, PAYSUM2, (RPO_PROPERTY + "=" + RPO2), "test").toArray());
        list.add(List.of("АГА", PAYSUM3, RPO3, "test").toArray());
        list.add(List.of("", PAYSUM2, (RPO_PROPERTY + "=" + RPO4), "test").toArray());
        list.add(List.of("", PAYSUM2, (RPO_PROPERTY + "=" + RPO5), "test").toArray());

        return list;
    }

    public static List<DBFField> createFields() {
        List<DBFField> fields = new ArrayList<>();

        List<String> data = List.of(ELM_CHECK_FIELD, PAY_SUM_FIELD, MSG_FIELD, "test");
        data.forEach(f -> {
            final DBFField field = new DBFField();
            field.setName(f);
            field.setType(DBFDataType.CHARACTER);
            field.setLength(FIELD_LN);
            fields.add(field);
        });

        return fields;
    }
}
