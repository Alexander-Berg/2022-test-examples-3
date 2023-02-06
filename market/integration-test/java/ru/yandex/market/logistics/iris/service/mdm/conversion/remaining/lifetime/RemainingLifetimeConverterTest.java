package ru.yandex.market.logistics.iris.service.mdm.conversion.remaining.lifetime;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetime;
import ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetimes;
import ru.yandex.market.logistics.iris.core.index.field.Field;
import ru.yandex.market.logistics.iris.core.index.field.FieldValue;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.service.mdm.conversion.field.lifetime.RemainingLifetimeConverter;
import ru.yandex.market.logistics.iris.util.UtcTimestampProvider;

public class RemainingLifetimeConverterTest extends AbstractContextualTest {

    private static final ZonedDateTime VALUE_DATE_TIME = ZonedDateTime.of(
            LocalDate.of(1970, 1, 2).atStartOfDay(),
            ZoneOffset.UTC
    );

    private static final ZonedDateTime UPDATED_DATE_TIME = ZonedDateTime.of(
            LocalDate.of(1975, 6, 2).atStartOfDay(),
            ZoneOffset.UTC
    );

    @Autowired
    private ChangeTrackingReferenceIndexer indexer;

    @SpyBean
    private UtcTimestampProvider utcTimestampProvider;

    private RemainingLifetimeConverter remainingLifetimeConverter;

    @Before
    public void init() {
        remainingLifetimeConverter = new RemainingLifetimeConverter(utcTimestampProvider);
    }

    @Test
    public void shouldSuccessConvertToInboundRemainingLifetimeDay() {
        List<RemainingLifetime> remainingLifetimes = Collections.singletonList(
                createRemainingLifetime(20, UPDATED_DATE_TIME.toInstant().toEpochMilli())
        );

        ChangeTrackingReferenceIndex index = createReferenceIndex(
                PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_DAY_FIELD,
                remainingLifetimes,
                VALUE_DATE_TIME
        );

        Collection<MdmIrisPayload.RemainingLifetime> result = remainingLifetimeConverter.toInboundInDay(index);

        Assert.assertEquals(1, result.size());

        MdmIrisPayload.RemainingLifetime actualRemainingLifeTime = result.iterator().next();
        Assert.assertEquals(20, actualRemainingLifeTime.getValue());
        Assert.assertEquals(UPDATED_DATE_TIME.toInstant().toEpochMilli(), actualRemainingLifeTime.getUpdatedTs());
    }

    @Test
    public void shouldSuccessConvertToOutboundRemainingLifetimeDay() {
        List<RemainingLifetime> remainingLifetimes = Collections.singletonList(
                createRemainingLifetime(15, UPDATED_DATE_TIME.toInstant().toEpochMilli())
        );

        ChangeTrackingReferenceIndex index = createReferenceIndex(
                PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_DAY_FIELD,
                remainingLifetimes,
                VALUE_DATE_TIME
        );

        Collection<MdmIrisPayload.RemainingLifetime> result = remainingLifetimeConverter.toOutboundInDay(index);

        Assert.assertEquals(1, result.size());

        MdmIrisPayload.RemainingLifetime actualRemainingLifeTime = result.iterator().next();
        Assert.assertEquals( 15, actualRemainingLifeTime.getValue());
        Assert.assertEquals(UPDATED_DATE_TIME.toInstant().toEpochMilli(), actualRemainingLifeTime.getUpdatedTs());
    }

    @Test
    public void shouldSuccessConvertToInboundRemainingLifetimePercentage() {
        List<RemainingLifetime> remainingLifetimes = Collections.singletonList(
                createRemainingLifetime(10, UPDATED_DATE_TIME.toInstant().toEpochMilli())
        );

        ChangeTrackingReferenceIndex index = createReferenceIndex(
                PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                remainingLifetimes,
                VALUE_DATE_TIME
        );

        Collection<MdmIrisPayload.RemainingLifetime> result = remainingLifetimeConverter.toInboundInPercentage(index);

        Assert.assertEquals(1, result.size());

        MdmIrisPayload.RemainingLifetime actualRemainingLifeTime = result.iterator().next();
        Assert.assertEquals(10, actualRemainingLifeTime.getValue());
        Assert.assertEquals(UPDATED_DATE_TIME.toInstant().toEpochMilli(), actualRemainingLifeTime.getUpdatedTs());
    }

    @Test
    public void shouldSuccessConvertToOutboundRemainingLifetimePercentage() {
        List<RemainingLifetime> remainingLifetimes = Collections.singletonList(
                createRemainingLifetime(5, UPDATED_DATE_TIME.toInstant().toEpochMilli())
        );

        ChangeTrackingReferenceIndex index = createReferenceIndex(
                PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                remainingLifetimes,
                VALUE_DATE_TIME
        );

        Collection<MdmIrisPayload.RemainingLifetime> result = remainingLifetimeConverter.toOutboundInPercentage(index);

        Assert.assertEquals(1, result.size());

        MdmIrisPayload.RemainingLifetime actualRemainingLifeTime = result.iterator().next();
        Assert.assertEquals(5, actualRemainingLifeTime.getValue());
        Assert.assertEquals(UPDATED_DATE_TIME.toInstant().toEpochMilli(), actualRemainingLifeTime.getUpdatedTs());
    }

    @Test
    public void shouldConvertEmptyReferenceIndexToEmptyList() {
        ChangeTrackingReferenceIndex index = indexer.createEmptyIndex();

        Collection<MdmIrisPayload.RemainingLifetime> result = remainingLifetimeConverter.toOutboundInPercentage(index);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldNotConvertEmptyReferenceIndex() {
        ChangeTrackingReferenceIndex index = indexer.createEmptyIndex();

        Collection<MdmIrisPayload.RemainingLifetime> result = remainingLifetimeConverter.toOutboundInPercentage(index);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldConvertEmptyReferenceIndexWithNullRemainingLifetimesToEmptyList() {
        ChangeTrackingReferenceIndex index = createReferenceIndex(
                PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_DAY_FIELD,
                null,
                VALUE_DATE_TIME
        );

        Collection<MdmIrisPayload.RemainingLifetime> result = remainingLifetimeConverter.toInboundInDay(index);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldSuccessConvertOnesFromOutboundRemainingLifetimeDay() {
        MdmIrisPayload.RemainingLifetime remainingLifetime = MdmIrisPayload.RemainingLifetime.newBuilder()
                .setValue(20)
                .setUpdatedTs(UPDATED_DATE_TIME.toInstant().toEpochMilli())
                .build();

        Optional<FieldValue<?>> result = remainingLifetimeConverter.fromInboundInDay(Collections.singletonList(remainingLifetime));

        Assert.assertTrue(result.isPresent());

        FieldValue<?> fieldValue = result.get();
        Assert.assertEquals(UPDATED_DATE_TIME, fieldValue.getUtcTimestamp());

        RemainingLifetimes remainingLifeTimes = (RemainingLifetimes) fieldValue.getValue();
        Assert.assertNotNull(remainingLifeTimes);

        List<RemainingLifetime> remainingLifetimeList = remainingLifeTimes.getRemainingLifetimes();
        Assert.assertEquals(1, remainingLifetimeList.size());

        RemainingLifetime actualRemainingLifetime = remainingLifetimeList.get(0);
        Assert.assertEquals(20, actualRemainingLifetime.getValue().intValue());
    }

    @Test
    public void shouldSelectFirstElementOfRemainingLifetimes() {
        List<MdmIrisPayload.RemainingLifetime> remainingLifetimes = createProtoRemainingLifetimes();
        Optional<FieldValue<?>> result = remainingLifetimeConverter.fromInboundInDay(remainingLifetimes);

        Assert.assertTrue(result.isPresent());

        FieldValue<?> fieldValue = result.get();
        Assert.assertEquals(UPDATED_DATE_TIME, fieldValue.getUtcTimestamp());

        RemainingLifetimes remainingLifeTimes = (RemainingLifetimes) fieldValue.getValue();
        Assert.assertNotNull(remainingLifeTimes);

        List<RemainingLifetime> remainingLifetimeList = remainingLifeTimes.getRemainingLifetimes();
        Assert.assertEquals(1, remainingLifetimeList.size());

        RemainingLifetime actualRemainingLifetime = remainingLifetimeList.get(0);
        Assert.assertEquals(15, actualRemainingLifetime.getValue().intValue());
    }

    @Test
    public void shouldNotConvertEmptyListOfOutboundRemainingLifetimeDay() {
        Mockito.doReturn(UPDATED_DATE_TIME).when(utcTimestampProvider).getCurrentUtcTimestamp();

        Optional<FieldValue<?>> result = remainingLifetimeConverter.fromInboundInDay(Collections.emptyList());

        Assert.assertFalse(result.isPresent());
    }

    private ChangeTrackingReferenceIndex createReferenceIndex(
            Field<RemainingLifetimes> remainingLifeTimeField,
            List<RemainingLifetime> remainingLifetimes,
            ZonedDateTime updateTs
    ) {
        ChangeTrackingReferenceIndex index = indexer.createEmptyIndex();
        index.set(remainingLifeTimeField,
                RemainingLifetimes.of(remainingLifetimes),
                updateTs
        );

        return index;
    }

    private RemainingLifetime createRemainingLifetime(int remainingLifeTimeAmount, long updatedDate) {
        return RemainingLifetime.of(remainingLifeTimeAmount, updatedDate);
    }

    private List<MdmIrisPayload.RemainingLifetime> createProtoRemainingLifetimes() {
        return Arrays.asList(
                MdmIrisPayload.RemainingLifetime.newBuilder()
                        .setValue(15)
                        .setUpdatedTs(UPDATED_DATE_TIME.toInstant().toEpochMilli())
                        .build()
                ,
                MdmIrisPayload.RemainingLifetime.newBuilder()
                        .setValue(10)
                        .setUpdatedTs(UPDATED_DATE_TIME.toInstant().toEpochMilli())
                        .build()
        );
    }
}
