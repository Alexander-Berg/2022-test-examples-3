package ru.yandex.market.wms.common.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.service.LineSelectionService;


public class LineSelectionServiceTest extends IntegrationTest {

    @Autowired
    private LineSelectionService lineSelectionService;

    @Test
    @DatabaseSetup("/db/dao/task-detail/before.xml")
    public void getLineTest() {
        var line = lineSelectionService.getLineIfSomeContainersIntoThisLine("CART002");
        assertions.assertThat(line).isPresent();
        assertions.assertThat(line.get()).isEqualTo("SORT-CONS");
    }

    @Test
    @DatabaseSetup("/db/dao/task-detail/several-waves-with-same-id.xml")
    public void getLineTestSeveralPicksCase() {
        var line = lineSelectionService.getLineIfSomeContainersIntoThisLine("CART002");
        assertions.assertThat(line).isPresent();
        assertions.assertThat(line.get()).isEqualTo("CONS-1");

        var line2 = lineSelectionService.getLineIfSomeContainersIntoThisLine("CART004");
        assertions.assertThat(line2).isPresent();
        assertions.assertThat(line2.get()).isEqualTo("CONS-2");
    }

    @Test
    public void getEmptyLineTest() {
        var line = lineSelectionService.getLineIfSomeContainersIntoThisLine("NOTEXIST");
        assertions.assertThat(line).isEmpty();
    }
}
