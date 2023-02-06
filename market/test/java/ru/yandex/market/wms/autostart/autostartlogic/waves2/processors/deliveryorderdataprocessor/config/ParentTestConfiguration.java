package ru.yandex.market.wms.autostart.autostartlogic.waves2.processors.deliveryorderdataprocessor.config;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStation;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.enums.LinkedToDsType;
import ru.yandex.market.wms.common.spring.service.time.WarehouseDateTimeService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfigurations.class)
public abstract class ParentTestConfiguration extends IntegrationTest {
    @Autowired
    private WarehouseDateTimeService dateTimeService;

    protected LocalDateTime currentTime;

    public static List<LinkedToDsType> modeList() {
        return List.of(LinkedToDsType.NO_LINK_TO_DS, LinkedToDsType.TO_UNLINKED_DS, LinkedToDsType.TO_LINKED_DS);
    }

    @BeforeEach
    public void init() {
        currentTime = dateTimeService.getWarehouseDateTimeNow();
    }

    protected List<String> stationNames(Set<CandidateSortStation> stations) {
        return stations.stream().map(CandidateSortStation::getName).toList();
    }
}
