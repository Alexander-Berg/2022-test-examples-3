package ru.yandex.mail.tests.mops.source;

import com.google.common.base.Joiner;
import lombok.val;

import java.util.List;

public class TidsSource implements Source {
    private final String[] tids;

    public TidsSource(String... tids) {
        this.tids = tids;
    }

    public TidsSource(List<String> tids) {
        this.tids = tids.toArray(new String[tids.size()]);
    }

    @Override
    public <T> void fill(T obj) throws Exception {
        val method = obj.getClass().getMethod("withTids", String.class);
        val tidsString = Joiner.on(",").join(tids);
        method.invoke(obj, tidsString);
    }
}