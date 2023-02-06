package ru.yandex.market.mbo.tms.model;

import java.util.Optional;

import Market.DataCamp.DataCampOfferMeta;
import com.google.protobuf.Timestamp;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static Market.DataCamp.DataCampContentMarketParameterValue.MarketParameterValue;
import static Market.DataCamp.DataCampContentMarketParameterValue.MarketValue;
import static Market.DataCamp.DataCampOfferMarketContent.MarketParameterValues;
import static Market.DataCamp.DataCampOfferMarketContent.MarketSpecificContent;
import static ru.yandex.market.mbo.tms.model.OffersChangeForFusedParametersService.OfferMapper;
import static ru.yandex.market.mbo.tms.model.OffersChangeForFusedParametersService.ParameterMigrationCache;

@SuppressWarnings("checkstyle:MagicNumber")
public class OffersChangeForFusedParametersServiceTest {
    @Test
    public void ignoreDataWithoutParametersFromLog() {
        ParameterMigrationCache log = Mockito.mock(ParameterMigrationCache.class);
        Mockito.when(log.containsSourceParameterId(123L, 234L)).thenReturn(false);
        Mockito.when(log.containsSourceParameterId(123L, 235L)).thenReturn(false);
        OfferMapper mapper = new OfferMapper(log);
        MarketParameterValues values = MarketParameterValues.newBuilder()
            .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder().build())
            .addParameterValues(MarketParameterValue.newBuilder()
                .setParamId(234L)
                .setValue(MarketValue.newBuilder().setNumericValue("100").build())
                .build())
            .addParameterValues(MarketParameterValue.newBuilder()
                .setParamId(235L)
                .setValue(MarketValue.newBuilder().setNumericValue("200").build())
                .build())
            .build();
        MarketSpecificContent content = MarketSpecificContent.newBuilder().setParameterValues(values).build();
        Assert.assertFalse(
            "MarketSpecificContent without globalized parameters shouldn't be updated",
            mapper.processOfferData(123L, content).isPresent()
        );
    }

    @Test
    public void updateDataWithoutParametersFromLog() {
        ParameterMigrationCache log = Mockito.mock(ParameterMigrationCache.class);
        Mockito.when(log.containsSourceParameterId(123L, 234L)).thenReturn(true);
        Mockito.when(log.containsSourceParameterId(123L, 235L)).thenReturn(false);
        Mockito.when(log.containsSourceParameterId(123L, 345L)).thenReturn(false);
        Mockito.when(log.getTargetParameterId(123L, 234L)).thenReturn(345L);
        OfferMapper mapper = new OfferMapper(log);
        Timestamp timestamp = Timestamp.newBuilder()
            .setSeconds(100L)
            .setNanos(1000000)
            .build();
        MarketParameterValues values = MarketParameterValues.newBuilder()
            .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                .setTimestamp(timestamp)
                .build())
            .addParameterValues(MarketParameterValue.newBuilder()
                .setParamId(234L)
                .setValue(MarketValue.newBuilder().setNumericValue("100").build())
                .build())
            .addParameterValues(MarketParameterValue.newBuilder()
                .setParamId(235L)
                .setValue(MarketValue.newBuilder().setNumericValue("200").build())
                .build())
            .build();
        MarketSpecificContent content = MarketSpecificContent.newBuilder().setParameterValues(values).build();
        Timestamp expectedTimestamp = Timestamp.newBuilder()
            .setSeconds(100L)
            .setNanos(2000000)
            .build();
        MarketParameterValues expectedValues = MarketParameterValues.newBuilder()
            .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                .setTsMs(100002L)
                .setTimestamp(expectedTimestamp)
                .build())
            .addParameterValues(MarketParameterValue.newBuilder()
                .setParamId(345L)
                .setValue(MarketValue.newBuilder().setNumericValue("100").build())
                .build())
            .addParameterValues(MarketParameterValue.newBuilder()
                .setParamId(235L)
                .setValue(MarketValue.newBuilder().setNumericValue("200").build())
                .build())
            .build();
        MarketSpecificContent expected = MarketSpecificContent.newBuilder().setParameterValues(expectedValues).build();
        Assert.assertEquals(Optional.of(expected), mapper.processOfferData(123L, content));
    }

    @Test
    public void dontUpdateDataWithTargetParametersFromLog() {
        ParameterMigrationCache log = Mockito.mock(ParameterMigrationCache.class);
        Mockito.when(log.containsSourceParameterId(123L, 234L)).thenReturn(true);
        Mockito.when(log.containsSourceParameterId(123L, 235L)).thenReturn(false);
        Mockito.when(log.getTargetParameterId(123L, 234L)).thenReturn(345L);
        OfferMapper mapper = new OfferMapper(log);
        Timestamp timestamp = Timestamp.newBuilder()
            .setSeconds(100L)
            .setNanos(1000000)
            .build();
        MarketParameterValues values = MarketParameterValues.newBuilder()
            .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                .setTimestamp(timestamp)
                .build())
            .addParameterValues(MarketParameterValue.newBuilder()
                .setParamId(234L)
                .setValue(MarketValue.newBuilder().setNumericValue("100").build())
                .build())
            .addParameterValues(MarketParameterValue.newBuilder()
                .setParamId(235L)
                .setValue(MarketValue.newBuilder().setNumericValue("200").build())
                .build())
            .addParameterValues(MarketParameterValue.newBuilder()
                .setParamId(345L)
                .setValue(MarketValue.newBuilder().setNumericValue("100").build())
                .build())
            .build();
        MarketSpecificContent content = MarketSpecificContent.newBuilder().setParameterValues(values).build();
        Assert.assertEquals(Optional.empty(), mapper.processOfferData(123L, content));
    }
}
