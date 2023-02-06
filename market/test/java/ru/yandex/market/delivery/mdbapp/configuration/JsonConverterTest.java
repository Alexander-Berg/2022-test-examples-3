package ru.yandex.market.delivery.mdbapp.configuration;

import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import steps.LocationSteps;
import steps.TrackSteps;
import steps.logisticsPointSteps.LogisticPointSteps;
import steps.outletSteps.OutletSteps;
import steps.shopSteps.ShopSteps;

import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPoint;
import ru.yandex.market.mbi.api.client.entity.outlets.Outlet;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(value = Parameterized.class)
public class JsonConverterTest {
    private final AppConfiguration appConfiguration = new AppConfiguration();
    private final ObjectMapper mapper = appConfiguration.dsmClientMessageConverter().getObjectMapper();

    @Parameter
    public Class<?> clazz;

    @Parameter(1)
    public JSONObject json;

    @Parameter(2)
    public Object object;

    @Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() throws JSONException {
        return Arrays.asList(new Object[][]{
            {Location.class, LocationSteps.getLocationJson(), LocationSteps.getLocation()},
            {Outlet.class, OutletSteps.getOutletJson(), OutletSteps.getDefaultOutlet()},
            {LogisticsPoint.class, LogisticPointSteps.getLogisticsPointJson(),
                LogisticPointSteps.getDefaultOutlet()},
            {Shop.class, ShopSteps.getShopJson(), ShopSteps.getDefaultShop()},
            {Track.class, TrackSteps.getTrackJson(), TrackSteps.getTrack()}
        });
    }

    @Test
    public void canSerializeTest() {
        String message = clazz + " can't be serialize";
        Assert.assertTrue(message, mapper.canSerialize(clazz));
    }

    @Test
    public void converterTest() throws Exception {
        Object actualObject = mapper.readValue(json.toString(), clazz);

        String message = clazz + " was converted with errors";

        assertThat(actualObject)
            .as(message)
            .usingRecursiveComparison()
            .isEqualTo(object);
    }
}
