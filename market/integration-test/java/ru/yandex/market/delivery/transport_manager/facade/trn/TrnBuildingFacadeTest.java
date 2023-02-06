package ru.yandex.market.delivery.transport_manager.facade.trn;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.queue.task.les.trn.TrnReadyLesProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationPartnerInfoMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.service.trn.TrnTemplaterService;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.ArgumentMatchers.any;

@DatabaseSetup({
    "/repository/facade/trn/transportation_with_outbound_fact.xml",
})
class TrnBuildingFacadeTest extends AbstractContextualTest {
    @Autowired
    private TrnBuildingFacade trnBuildingFacade;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @Autowired
    private TrnReadyLesProducer trnReadyLesProducer;

    @Autowired
    private TrnTemplaterService trnTemplaterService;

    @Autowired
    private TransportationMapper transportationMapper;

    @Autowired
    private RegisterMapper registerMapper;

    @Autowired
    private TransportationPartnerInfoMapper partnerInfoMapper;

    @BeforeEach
    void init() throws MalformedURLException {
        Mockito.doNothing().when(mdsS3Client).upload(any(), any());
        Mockito.when(mdsS3Client.getUrl(resourceLocation("TMU2", "TMR11", "tm_trn.xlsx")))
            .thenReturn(new URL("http://storage.s3.mds.yandex-team.net/bb"));

        clock.setFixed(Instant.parse("2022-04-05T10:00:00Z"), ZoneOffset.UTC);

        Transportation transportation = transportationMapper.getById(1L);
        Register register = registerMapper.getRegisterWithUnits(11L);

        Mockito.doReturn(new byte[]{0, 1}).when(trnTemplaterService).getTrnDocumentAsBytes(transportation, register);
    }

    @Test
    @DatabaseSetup("/repository/transportation_unit_documents/empty_documents.xml")
    void buildTrnSc() {
        TransportationPartnerInfo partnerInfo = new TransportationPartnerInfo()
            .setPartnerId(5L)
            .setPartnerType(PartnerType.SORTING_CENTER)
            .setPartnerName("5")
            .setTransportationId(1L);

        partnerInfoMapper.insert(partnerInfo);

        Transportation transportation = transportationMapper.getById(1L);
        Register register = registerMapper.getRegisterWithUnits(11L);

        trnBuildingFacade.buildTrn(1L, 11L);
        Mockito.verify(trnReadyLesProducer).enqueue(
            5L,
            "ololo",
            List.of("http://storage.s3.mds.yandex-team.net/bb"),
            1L
        );
        Mockito.verify(trnTemplaterService).getTrnDocumentAsBytes(transportation, register);
    }

    @ExpectedDatabase(
        value = "/repository/transportation_unit_documents/after/after_trn_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void buildTrnFF() {
        TransportationPartnerInfo partnerInfo = new TransportationPartnerInfo()
            .setPartnerId(5L)
            .setPartnerType(PartnerType.FULFILLMENT)
            .setPartnerName("5")
            .setTransportationId(1L);

        Transportation transportation = transportationMapper.getById(1L);
        Register register = registerMapper.getRegisterWithUnits(11L);

        partnerInfoMapper.insert(partnerInfo);
        trnBuildingFacade.buildTrn(1L, 11L);
        Mockito.verify(trnReadyLesProducer).enqueue(
            5L,
            "ololo",
            List.of("http://storage.s3.mds.yandex-team.net/bb"),
            1L
        );
        Mockito.verify(trnTemplaterService).getTrnDocumentAsBytes(transportation, register);
    }

    private ResourceLocation resourceLocation(String... path) {
        return resourceLocationFactory.createLocation(
            Stream.of(path).map(String::trim).collect(Collectors.joining("/"))
        );
    }
}
