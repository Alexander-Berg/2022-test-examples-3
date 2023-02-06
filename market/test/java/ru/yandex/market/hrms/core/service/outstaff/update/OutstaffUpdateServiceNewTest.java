package ru.yandex.market.hrms.core.service.outstaff.update;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.config.TestMockConfig;
import ru.yandex.market.hrms.core.domain.yt.YtTableDto;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffUpdateServiceNew;
import ru.yandex.market.hrms.core.service.outstaff.enums.OutstaffFormEnum;
import ru.yandex.market.hrms.core.service.outstaff.stubs.OutstaffYqlRepoStub;
import ru.yandex.market.hrms.core.service.outstaff.stubs.YaDiskClientStub;
import ru.yandex.market.hrms.core.service.timex.TimexApiFacadeNew;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

public class OutstaffUpdateServiceNewTest extends AbstractCoreTest {

    @Autowired
    private OutstaffUpdateServiceNew service;

    @Autowired
    private ApplicationContext context;

    @MockBean
    private TimexApiFacadeNew timexApiFacadeNew;

    private String getStringFromResource(String resource) throws Exception {
        return IOUtils.toString(TestMockConfig.class.getResourceAsStream(resource),
                StandardCharsets.UTF_8);
    }

    private void configureOutstaffYqlRepo(String ytTable, String formJson, long ytId, long ytUid) throws Exception {
        String json = getStringFromResource(formJson);
        var result = new ArrayList<YtTableDto>();
        result.add(new YtTableDto(ytId, ytUid, json, Instant.now()));

        var stub = (OutstaffYqlRepoStub) context.getBean("outstaffYqlRepo");
        stub.withData(ytTable, result);
    }

    private void configureYaDiskClient(String yaDiskUrl, int statusCode, byte[] file) {
        var yaDiskClientStub = context.getBean(YaDiskClientStub.class);
        yaDiskClientStub.withDownloadLink(statusCode, yaDiskUrl);
        yaDiskClientStub.withPhoto(file);
    }

    @Test
    @DbUnitDataSet(before = "OutstaffUpdateServiceNewTest.load_update_request.before.csv",
            after = "OutstaffUpdateServiceNewTest.load_update_requests.after.csv")
    public void shouldLoadDataWithoutErrors() throws Exception {
        configureYaDiskClient("https://disk.yandex.ru/blabla", 200, new byte[]{0, 1, 2});

        configureOutstaffYqlRepo(OutstaffFormEnum.EXTERNAL.getYtTablePath(),
                "/inputs/outstaff_update_requests_form_external.json", 1, 1);

        configureOutstaffYqlRepo(OutstaffFormEnum.INTERNAL.getYtTablePath(),
                "/inputs/outstaff_update_requests_form_internal.json", 1, 1);



        var errors = service.getUpdateRequests();

        Assertions.assertEquals(0, errors.size());
    }

    @SneakyThrows
    @Test
    @DbUnitDataSet(before = "OutstaffUpdateServiceNewTestNew.process_update_request.before.csv",
            after = "OutstaffUpdateServiceNewTestNew.process_update_request.after.csv")
    public void shouldProcessDataWithoutErrors() {
        mockClock(LocalDate.of(2021, 12, 10));

        doNothing().when(timexApiFacadeNew).removeEmployee(any());
        configureYaDiskClient("https://disk.yandex.ru/blabla", 200, new byte[]{0, 1, 2});

        var errors = service.processUpdateRequests();
    }
}
