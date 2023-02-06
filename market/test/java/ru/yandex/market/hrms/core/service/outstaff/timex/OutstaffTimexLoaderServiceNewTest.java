package ru.yandex.market.hrms.core.service.outstaff.timex;

import java.util.List;
import java.util.Optional;

import one.util.streamex.StreamEx;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.AccessLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.outstaff.OutstaffTimexSyncEntity;
import ru.yandex.market.hrms.core.domain.outstaff.repo.OutstaffTimexSyncRepo;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffTimexLoaderServiceNew;
import ru.yandex.market.hrms.core.service.outstaff.client.YaDiskClient;
import ru.yandex.market.hrms.core.service.s3.S3Service;
import ru.yandex.market.hrms.core.service.timex.TimexApiFacadeNew;
import ru.yandex.market.hrms.core.service.timex.client.TimexClient;
import ru.yandex.market.hrms.core.service.timex.dto.TimexResponseDto;
import ru.yandex.market.hrms.core.service.timex.dto.TimexUserDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static ru.yandex.market.hrms.core.service.timex.TimexOperationStatus.SUCCESS;

class OutstaffTimexLoaderServiceNewTest extends AbstractCoreTest {

    @MockBean
    TimexApiFacadeNew timexApiFacade;
    @MockBean
    TimexClient timexClient;
    @MockBean
    YaDiskClient yaDiskClient;
    @MockBean
    S3Service s3Service;

    @Autowired
    private OutstaffTimexSyncRepo outstaffTimexSyncRepo;

    @Autowired
    private OutstaffTimexLoaderServiceNew outstaffTimexService;

    @BeforeEach
    public void beforeEach() throws Throwable {
        when(s3Service.getObject(anyString(), anyString()))
                .thenReturn(Optional.of(new byte[0]));

        var accessLevel = new AccessLevel();
        accessLevel.setName("ФФЦ и СЦ Томилино");

        when(timexApiFacade.getAllAccessLevels())
                .thenReturn(List.of(accessLevel));

        when(timexApiFacade.createEmployee(any(TimexUserDto.class)))
                .thenReturn(new TimexResponseDto(SUCCESS, "test"))
                .thenThrow(new RuntimeException("oops..."));
    }

    @Test
    @DbUnitDataSet(before = "OutstaffTimexLoaderServiceNewTest.before.csv")
    void shouldTryToPushAllSyncRecords() {
        for (int i = 0; i < 2; i++) {
            Assertions.assertThrows(RuntimeException.class,
                    () -> outstaffTimexService.pushOutstaffToTimex());
        }

        Assertions.assertEquals(0,
                StreamEx.of(outstaffTimexSyncRepo.findAll())
                        .filterBy(OutstaffTimexSyncEntity::getUpdatedAt, null)
                        .count(),
                "Количество записей, которые не пробовали запушить"
        );
    }
}