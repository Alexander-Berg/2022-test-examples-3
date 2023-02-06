package ru.yandex.market.logistics.management.controller;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerTransportFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerTransportDto;

@DatabaseSetup("/data/controller/partnerTransport/setup/before.xml")
public class PartnerTransportControllerTest extends AbstractContextualTest {

    @Autowired
    PartnerTransportController partnerTransportController;

    @Test
    @DisplayName("Получили по фильтру")
    void getTransportByFilter() {
        List<PartnerTransportDto> transports = partnerTransportController.get(PartnerTransportFilter.builder()
            .logisticsPointFrom(1L)
            .build()).unwrap();
        softly.assertThat(transports.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("По фильтру ничего нет")
    void getTransportIrrelevantFilter() {
        List<PartnerTransportDto> transports = partnerTransportController.get(PartnerTransportFilter.builder()
            .logisticsPointFrom(2L)
            .build()).unwrap();
        softly.assertThat(transports).isEmpty();
    }

    @Test
    @DisplayName("Пустой фильтр вернёт всё")
    void getTransportNoFilter() {
        partnerTransportController.get(PartnerTransportFilter.builder().build());
        List<PartnerTransportDto> transports = partnerTransportController.get(PartnerTransportFilter.builder()
            .build()).unwrap();
        softly.assertThat(transports.size()).isEqualTo(1);
    }
}
