package ru.yandex.travel.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateAdapter extends XmlAdapter<String, Date> {

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd";

    private final SimpleDateFormat format;

    public DateAdapter() {
        this(DEFAULT_PATTERN);
    }

    public DateAdapter(String pattern) {
        this.format = new SimpleDateFormat(pattern);
    }

    @Override
    public Date unmarshal(String value) throws Exception {
        return this.format.parse(value);
    }

    @Override
    public String marshal(Date value) throws Exception {
        return this.format.format(value);
    }

}
