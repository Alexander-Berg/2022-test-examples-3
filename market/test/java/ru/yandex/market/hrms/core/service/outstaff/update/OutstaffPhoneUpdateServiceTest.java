package ru.yandex.market.hrms.core.service.outstaff.update;

import java.util.List;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.ispring.ISpringService;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffPhoneUpdateService;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffServiceV2;
import ru.yandex.market.ispring.ISpringClient;
import ru.yandex.market.ispring.pojo.UserToGetDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class OutstaffPhoneUpdateServiceTest extends AbstractCoreTest {

    @Autowired
    private OutstaffPhoneUpdateService outstaffPhoneUpdateService;

    @MockBean
    private ISpringClient iSpringClient;

    @MockBean
    private ISpringService iSpringService;

    @SneakyThrows
    @Test
    @DbUnitDataSet(before = "OutstaffUpdatePhoneServiceTest.update.before.csv",
            after = "OutstaffUpdatePhoneServiceTest.update.after.csv")
    public void equalPhoneInIspring() {
        when(iSpringClient.modifyUser(any(), any())).thenReturn(true);
        UserToGetDto userToGetDto = new UserToGetDto();
        userToGetDto.setFields(List.of(new UserToGetDto.UserField("LOGIN", "1")));

        when(iSpringService.getUser(any())).thenReturn(userToGetDto);

        outstaffPhoneUpdateService.updatePhone(outstaffServiceV2.findByIdOrThrow(5L), "+1");
    }

    @Autowired
    private OutstaffServiceV2 outstaffServiceV2;


    @Test
    @DbUnitDataSet(before = "OutstaffUpdatePhoneServiceTest.update.before.csv",
            after = "OutstaffUpdatePhoneServiceTest.update.after.csv")
    public void notEqualPhoneInIspring() {
        when(iSpringClient.modifyUser(any(), any())).thenReturn(true);
        UserToGetDto userToGetDto = new UserToGetDto();
        userToGetDto.setFields(List.of(new UserToGetDto.UserField("LOGIN", "2")));

        when(iSpringService.getUser(any())).thenReturn(userToGetDto);

        outstaffPhoneUpdateService.updatePhone(outstaffServiceV2.findByIdOrThrow(5L), "+1");
    }
}
