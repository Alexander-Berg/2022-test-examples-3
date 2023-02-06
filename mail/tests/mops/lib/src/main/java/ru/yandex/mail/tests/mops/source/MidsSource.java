package ru.yandex.mail.tests.mops.source;

import com.google.common.base.Joiner;
import lombok.val;

import java.util.List;

public class MidsSource implements Source {
    private final String[] mids;

    public MidsSource(String... mids) {
        this.mids = mids;
    }

    public MidsSource(List<String> mids) {
        this.mids = mids.toArray(new String[mids.size()]);
    }

    @Override
    public <T> void fill(T obj) throws Exception {
        val method = obj.getClass().getMethod("withMids", String.class);
        val midsString = Joiner.on(",").join(mids);
        method.invoke(obj, midsString);
    }
}