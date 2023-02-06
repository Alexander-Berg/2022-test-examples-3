package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.UpdateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@DbUnitDataSet(
        before = "SyncLocalRegionWithLmsCommand/testSyncLocalRegionWithLms.before.csv"
)
public class SyncLocalRegionWithLmsCommandTest extends FunctionalTest {
    @Autowired
    private SyncLocalRegionWithLmsCommand command;

    @Autowired
    private LMSClient lmsClient;

    @Test
    @DisplayName("Обновляем значение региона склада в ЛМС")
    void testManagePaymentTypesByShopId() {
        String name = "Warehouse name";
        String readableName = "Warehouse readable name";
        Contact contact = new Contact("name", "surname", "patronymic");
        Set<Phone> phones = Set.of();
        Set<ScheduleDayResponse> schedule = Set.of();
        String externalId = "external-id";
        String comment = "comment";

        when(lmsClient.getBusinessWarehouseForPartner(eq(2L))).thenReturn(
                Optional.of(BusinessWarehouseResponse.newBuilder()
                                .partnerId(2L)
                                .partnerType(PartnerType.DROPSHIP_BY_SELLER)
                                .locationId(2)
                                .name(name)
                                .readableName(readableName)
                                .externalId(externalId)
                                .phones(phones)
                                .schedule(schedule)
                                .contact(contact)
                                .address(
                                        Address.newBuilder()
                                                .comment(comment)
                                                .locationId(2)
                                                .settlement("Питер")
                                                .country("Россия")
                                                .region("Питер")
                                                .longitude(BigDecimal.valueOf(30.315635000000))
                                                .latitude(BigDecimal.valueOf(59.938951000000))
                                                .build()
                                )
                        .build()
        )
        );

        CommandInvocation commandInvocation = new CommandInvocation("sync-local-region-with-lms",
                new String[]{"2000,4000"},
                Collections.emptyMap());
        Terminal terminal = createTerminal();

        command.executeCommand(commandInvocation, terminal);

        ArgumentCaptor<UpdateBusinessWarehouseDto> captor = ArgumentCaptor.forClass(UpdateBusinessWarehouseDto.class);
        Mockito.verify(lmsClient, times(1))
                .updateBusinessWarehouse(any(), captor.capture());

        UpdateBusinessWarehouseDto value = captor.getValue();
        assertEquals(value.getContact(), contact);
        assertEquals(value.getName(), name);
        assertEquals(value.getReadableName(), readableName);
        assertEquals(value.getPhones(), phones);
        assertEquals(value.getSchedule(), schedule);
        assertEquals(value.getExternalId(), externalId);
        assertEquals(value.getAddressComment(), comment);
        Address address = value.getAddress();
        assertEquals(address.getLocationId(), 213);
        assertEquals(address.getLongitude(), BigDecimal.valueOf(37.622504000000));
        assertEquals(address.getLatitude(), BigDecimal.valueOf(55.753215000000));
        assertEquals(address.getCountry(), "Россия");
        assertEquals(address.getRegion(), "Москва");
        assertEquals(address.getSettlement(), "Москва");
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        when(terminal.areYouSure()).thenReturn(true);
        return terminal;
    }
}
