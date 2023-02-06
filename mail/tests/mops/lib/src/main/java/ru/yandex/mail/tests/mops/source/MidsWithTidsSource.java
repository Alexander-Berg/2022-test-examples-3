package ru.yandex.mail.tests.mops.source;

import com.google.common.base.Joiner;
import lombok.val;

import java.util.List;

import static java.util.Arrays.asList;

public class MidsWithTidsSource implements Source {
    private final List<String> mids;
    private final List<String> tids;

    public MidsWithTidsSource(List<String> mids, List<String> tids) {
        this.mids = mids;
        this.tids = tids;
    }

    public MidsWithTidsSource(String mid, String tid) {
        this(asList(mid), asList(tid));
    }

    @Override
    public <T> void fill(T obj) throws Exception {
        fill(obj, "withMids", mids);
        fill(obj, "withTids", tids);
    }

    private <T> void fill(T obj, String methodName, List<?> values) throws Exception {
        val method = obj.getClass().getMethod(methodName, String.class);
        val valuesString = Joiner.on(",").join(values);
        method.invoke(obj, valuesString);
    }
}