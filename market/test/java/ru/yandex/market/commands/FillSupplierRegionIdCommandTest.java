package ru.yandex.market.commands;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

public class FillSupplierRegionIdCommandTest extends FunctionalTest {
    @Autowired
    private Terminal terminal;
    @Autowired
    private FillSupplierRegionIdCommand cmd;
    @Autowired
    private GeoClient geoClient;

    @Test
    @DbUnitDataSet(before = "fillSupplierRegionIdCommand.before.csv",
            after = "fillSupplierRegionIdCommand.after.csv")
    void testExecuteCommand() {
        when(geoClient.findFirst("addr2")).thenReturn(createGeoObject("13"));
        when(geoClient.findFirst("addr3")).thenReturn(createGeoObject("42"));
        when(geoClient.findFirst("addr4")).thenReturn(createGeoObject("unparseable int"));
        CommandInvocation commandInvocation = new CommandInvocation("fill-supplier-region-id",
                new String[]{},
                Collections.emptyMap());
        cmd.executeCommand(commandInvocation, terminal);
    }

    private Optional<GeoObject> createGeoObject(String geoId) {
        return Optional.of(SimpleGeoObject.newBuilder()
                .withToponymInfo(ToponymInfo.newBuilder().withGeoid(geoId).build())
                .withAddressInfo(AddressInfo.newBuilder()
                        .withCountryInfo(CountryInfo.newBuilder().build())
                        .withAreaInfo(AreaInfo.newBuilder().build())
                        .withLocalityInfo(LocalityInfo.newBuilder().build())
                        .build()
                )
                .withBoundary(Boundary.newBuilder().build())
                .build()
        );
    }
}
