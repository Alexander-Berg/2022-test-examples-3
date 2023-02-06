package ru.yandex.market.wms.reporter.reports;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import ru.yandex.market.wms.common.spring.enums.ReportFormat;
import ru.yandex.market.wms.reporter.annotation.WmsReport;
import ru.yandex.market.wms.reporter.dao.entity.ReportParam;
import ru.yandex.market.wms.reporter.dao.entity.ReportTask;
import ru.yandex.market.wms.reporter.enums.ReportParamType;
import ru.yandex.market.wms.reporter.exception.GenerateException;

@WmsReport(code = "TestReport1", group = "Тест", name = "Отчёт для тестов №1")
public class TestOne implements Report {
    private final ObjectMapper mapper;

    public TestOne(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ReportParam> getParams() {
        List<ReportParam> results = new ArrayList<>();

        results.add(ReportParam.builder()
                .code("param1")
                .caption("Параметр 1")
                .paramType(ReportParamType.INTEGER)
                .build());

        results.add(ReportParam.builder()
                .code("param2")
                .caption("Параметр 2")
                .paramType(ReportParamType.FLOAT)
                .required(true)
                .numberValue(42.5)
                .build());

        results.add(ReportParam.builder()
                .code("param3")
                .caption("Параметр 3")
                .paramType(ReportParamType.LIST)
                .required(true)
                .allowedValues(Arrays.asList("Yes", "No"))
                .build());

        results.add(ReportParam.builder()
                .code("param4")
                .caption("Параметр 4")
                .paramType(ReportParamType.MULTILIST)
                .required(true)
                .dynamicList(true)
                .listValue(Collections.singletonList("Value 2"))
                .build());

        return results;
    }

    @Override
    public List<String> searchListValue(String paramCode, String term) {
        if (paramCode.equals("param4")) {
            List<String> list = new ArrayList<>();
            list.add("Value 1");
            list.add("Value 2");
            list.add("Value 3");

            return list.stream().filter(val -> StringUtils.containsIgnoreCase(val, term)).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public ReportOutput generate(List<ReportParam> params) {
        TestOneData result = new TestOneData();
        result.params = params;

        Number param1value = params.stream()
                .filter(p -> p.getCode().equals("param1"))
                .findFirst()
                .map(ReportParam::getNumberValue)
                .orElse(null);

        result.data = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            TestOneData.DataRow row = new TestOneData.DataRow();
            row.col1 = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
            row.col2 = 42;
            row.col3 = "Hello";
            row.col4 = param1value;
            result.data.add(row);
        }
        return new JsonReportOutput(mapper, result);
    }

    @Override
    public List<ReportFormat> getSupportedFormats() {
        List<ReportFormat> result = new ArrayList<>();

        result.add(ReportFormat.JSON);
        result.add(ReportFormat.CSV);
        result.add(ReportFormat.HTML);
        result.add(ReportFormat.ZPL);

        return result;
    }

    @Override
    public String generateHardCopyName(ReportTask data) {
        return "TestOne";
    }

    @Override
    public void createHardCopy(ReportOutput data, ReportFormat format, OutputStream outputStream) {
        if (format.equals(ReportFormat.JSON)) {
            try {
                mapper.writeValue(outputStream, JsonReportOutput.unwrap(data));
            } catch (IOException e) {
                throw new GenerateException("Create json error");
            }
        } else {
            throw new GenerateException("Unsupported format " + format.getCode());
        }
    }

}
