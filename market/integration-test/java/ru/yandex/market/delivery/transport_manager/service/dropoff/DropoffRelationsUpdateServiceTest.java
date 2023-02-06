package ru.yandex.market.delivery.transport_manager.service.dropoff;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.yt.DropoffRelationDto;
import ru.yandex.market.delivery.transport_manager.service.yt.YtCommonReader;

public class DropoffRelationsUpdateServiceTest extends AbstractContextualTest {
    @Autowired
    private DropoffRelationsUpdateService updateService;

    @Autowired
    private YtCommonReader<DropoffRelationDto> reader;

    @BeforeEach
    void init() {
        Mockito.doReturn(Set.of(
            new DropoffRelationDto(1L, 2L, 3L, 5L, 7L),
            new DropoffRelationDto(7L, 8L, 9L, 10L, 12L)
            )
        ).when(reader).getTableData(DropoffRelationDto.class, "//path");
    }

    @Test
    @DatabaseSetup("/repository/dropoff/before_update.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdate() {
        updateService.update();
    }


}
