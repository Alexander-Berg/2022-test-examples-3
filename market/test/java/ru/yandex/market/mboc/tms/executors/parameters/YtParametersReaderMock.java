package ru.yandex.market.mboc.tms.executors.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.mboc.common.parameters.Parameter;

public class YtParametersReaderMock implements YtParametersReader {

    private List<Parameter> parameters = new ArrayList<>();

    public void insert(Parameter parameter) {
        parameters.add(parameter);
    }

    public void insert(List<Parameter> parameters) {
        this.parameters.addAll(parameters);
    }

    public void delete(Parameter parameter) {
        parameters.remove(parameter);
    }

    @Override
    public List<Parameter> readParameters(String sessionId, long rowFrom, long rowTo) {
        int from = (int) rowFrom;
        if (from >= parameters.size()) {
            return Collections.emptyList();
        }
        int to = (int) rowTo;
        if (rowTo > parameters.size()) {
            to = parameters.size();
        }
        return parameters.subList(from, to);
    }
}
