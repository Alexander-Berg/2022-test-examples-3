package ru.yandex.market.delivery.transport_manager.converter.ffwf;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.Tag;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.TagCode;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.service.tag.TagReceiver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExternalRequestIdExtractorTest {

    private TagReceiver tagReceiver;
    private ExternalRequestIdExtractor externalRequestIdExtractor;


    @BeforeEach
    void setUp() {
        tagReceiver = mock(TagReceiver.class);
        externalRequestIdExtractor = new ExternalRequestIdExtractor(new IdPrefixConverter(), tagReceiver);
    }

    @Test
    void testExtractWmsOrAxaptaId() {
        TransportationUnit inbound = new TransportationUnit()
            .setId(1000L)
            .setType(TransportationUnitType.INBOUND);
        Transportation transportation = new Transportation()
            .setId(1L)
            .setInboundUnit(inbound)
            .setTransportationType(TransportationType.XDOC_PARTNER_SUPPLY_TO_DISTRIBUTION_CENTER);

        when(tagReceiver.get(1L)).thenReturn(buildTags());
        String externalRequestId = externalRequestIdExtractor.extract(transportation, inbound);

        assertThat(externalRequestId).isEqualTo("2");
    }

    @Test
    void testExtractIdWithPrefix() {
        TransportationUnit inbound = new TransportationUnit()
            .setId(1000L)
            .setType(TransportationUnitType.INBOUND);
        Transportation transportation = new Transportation()
            .setId(1L)
            .setInboundUnit(inbound)
            .setTransportationType(TransportationType.XDOC_TRANSPORT);

        when(tagReceiver.get(1L)).thenReturn(buildTags());
        String externalRequestId = externalRequestIdExtractor.extract(transportation, inbound);

        assertThat(externalRequestId).isEqualTo("TMU1000");
    }

    private List<Tag> buildTags() {
        return List.of(
            new Tag().setTransportationId(1L).setCode(TagCode.FFWF_ROOT_REQUEST_ID).setValue("1"),
            new Tag().setTransportationId(1L).setCode(TagCode.XDOC_PARENT_REQUEST_ID).setValue("2")
        );
    }
}
