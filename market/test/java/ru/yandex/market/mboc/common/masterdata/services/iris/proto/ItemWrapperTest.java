package ru.yandex.market.mboc.common.masterdata.services.iris.proto;


import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

/**
 * @author masterj
 * @since 19 Jun 2019
 */
public class ItemWrapperTest {

    private static final long SEED = 19517;
    private static final int RANDOM_BATCH_SIZE = 13;
    private static final int VALID_SUPPLIER_ID = 12345;

    private EnhancedRandom random;

    @Before
    public void setUp() throws Exception {
        random = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenCopiesShouldCreateEqualInstance() {
        random.objects(ItemWrapperStub.class, RANDOM_BATCH_SIZE)
            .forEach(randomItem -> {
                ItemWrapperStub item = new ItemWrapperStub();

                Assertions.assertThat(item).isNotEqualTo(randomItem);
                item.copyFieldsFrom(randomItem);
                Assertions.assertThat(item).isEqualTo(randomItem);
            });
    }

    @Test
    public void whenSerializeItemSouldDeserializeToEqual() {
        random.objects(MdmIrisPayload.Item.class, RANDOM_BATCH_SIZE)
            .forEach(this::checkJsonSerializer);
    }

    @Test
    public void whenSerializeShippingUnitSouldDeserializeToEqual() {
        random.objects(MdmIrisPayload.ShippingUnit.class, RANDOM_BATCH_SIZE)
            .forEach(this::checkJsonSerializer);
    }

    private void checkJsonSerializer(Message message) {
        try {
            String serialized = JsonFormat.printer()
                .includingDefaultValueFields()
                .print(message);

            Message.Builder builder = message.getDefaultInstanceForType().newBuilderForType();
            JsonFormat.parser().merge(serialized, builder);

            Message deserialized = builder.build();

            Assertions.assertThat(deserialized).isEqualTo(message);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void whenCopiesShouldCreateIndependentInstances() {
        ItemWrapperStub item1 = random.nextObject(ItemWrapperStub.class);
        ItemWrapperStub item2 = new ItemWrapperStub();
        item2.copyFieldsFrom(item1);
        item1.setSingleInformationItem(random.nextObject(ItemWrapperStub.class).getItem());
        Assertions.assertThat(item1).isNotEqualTo(item2);
    }

    @Test
    public void whenCompareDataShouldExcludeTimestamps() {
        MdmIrisPayload.Item.Builder item = random.nextObject(MdmIrisPayload.Item.class).toBuilder();
        item.setItemId(item.getItemIdBuilder().setSupplierId(VALID_SUPPLIER_ID));

        item.getInformationBuilder(0).getNameBuilder().setUpdatedTs(1);
        ItemWrapperStub item1 = new ItemWrapperStub(item.build());

        item.getInformationBuilder(0).getNameBuilder().setUpdatedTs(2);
        ItemWrapperStub item2 = new ItemWrapperStub(item.build());

        Assertions.assertThat(item1.equalsByDataExcludingTsAndRsl(item2, false)).isTrue();
    }

    @Test
    public void whenCompareDataShouldNotExcludeMeaningfulData() {
        MdmIrisPayload.Item.Builder item = random.nextObject(MdmIrisPayload.Item.class).toBuilder();
        item.setItemId(item.getItemIdBuilder().setSupplierId(VALID_SUPPLIER_ID));

        item.getInformationBuilder(0).getNameBuilder().setValue("1");
        ItemWrapperStub item1 = new ItemWrapperStub(item.build());

        item.getInformationBuilder(0).getNameBuilder().setValue("2");
        ItemWrapperStub item2 = new ItemWrapperStub(item.build());

        Assertions.assertThat(item1.equalsByDataExcludingTsAndRsl(item2, false)).isFalse();
    }

    @Test
    public void whenGetMaxLastUpdatedTsShouldReturnMaxUpdatedTs() {
        MdmIrisPayload.Item.Builder item = MdmIrisPayload.Item.newBuilder();
        item.setItemId(random.nextObject(MdmIrisPayload.MdmIdentifier.class));

        MdmIrisPayload.ReferenceInformation.Builder referenceInformation = item.addInformationBuilder()
            .setSource(random.nextObject(MdmIrisPayload.Associate.class))
            .setTarget(random.nextObject(MdmIrisPayload.Associate.class));

        int i = 0;
        Assertions.assertThat(new ItemWrapperStub(item.build()).getMaxLastUpdatedTs()).isEqualTo(i);

        referenceInformation.setBoxCapacity(MdmIrisPayload.Int32Value.newBuilder().setUpdatedTs(++i));
        Assertions.assertThat(new ItemWrapperStub(item.build()).getMaxLastUpdatedTs()).isEqualTo(i);

        referenceInformation.setBoxCount(MdmIrisPayload.Int32Value.newBuilder().setUpdatedTs(++i));
        Assertions.assertThat(new ItemWrapperStub(item.build()).getMaxLastUpdatedTs()).isEqualTo(i);

        referenceInformation.addCargotypeId(MdmIrisPayload.Int32Value.newBuilder().setUpdatedTs(++i));
        Assertions.assertThat(new ItemWrapperStub(item.build()).getMaxLastUpdatedTs()).isEqualTo(i);

        referenceInformation.setLifetime(MdmIrisPayload.Lifetime.newBuilder().setUpdatedTs(++i));
        Assertions.assertThat(new ItemWrapperStub(item.build()).getMaxLastUpdatedTs()).isEqualTo(i);

        referenceInformation.addShippingUnit(MdmIrisPayload.ShippingUnit.newBuilder()
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(++i)));
        Assertions.assertThat(new ItemWrapperStub(item.build()).getMaxLastUpdatedTs()).isEqualTo(i);

        referenceInformation.addShippingUnit(MdmIrisPayload.ShippingUnit.newBuilder()
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(++i)));
        Assertions.assertThat(new ItemWrapperStub(item.build()).getMaxLastUpdatedTs()).isEqualTo(i);

        referenceInformation.setItemShippingUnit(MdmIrisPayload.ShippingUnit.newBuilder()
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(++i)));
        Assertions.assertThat(new ItemWrapperStub(item.build()).getMaxLastUpdatedTs()).isEqualTo(i);

        referenceInformation.addMinInboundLifetimeDay(MdmIrisPayload.RemainingLifetime.newBuilder()
            .setValue(1).setStartDate(0L).setUpdatedTs(++i));
        Assertions.assertThat(new ItemWrapperStub(item.build()).getMaxLastUpdatedTs()).isEqualTo(i);
    }
}
