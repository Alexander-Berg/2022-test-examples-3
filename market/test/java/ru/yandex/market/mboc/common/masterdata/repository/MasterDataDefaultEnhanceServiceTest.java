package ru.yandex.market.mboc.common.masterdata.repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.SupplyEvent;

/**
 * @author amaslak
 */
public class MasterDataDefaultEnhanceServiceTest {

    private static final int DATASET_SIZE = 1000;

    private static final int MAX_RANDOM_INT_BOUND = 1000;

    private EnhancedRandom enhancedRandom;

    @Before
    @SuppressWarnings("checkstyle:magicNumber")
    public void setUp() {
        enhancedRandom = new EnhancedRandomBuilder()
            .seed(765)
            .stringLengthRange(0, 20)
            .build();
    }

    @Test
    public void withDefaultFieldValues() {

        List<MasterData> dataset = enhancedRandom.objects(MasterData.class, DATASET_SIZE, "categoryId")
            .collect(Collectors.toList());

        for (MasterData source : dataset) {
            source.setSupplySchedule(Collections.emptyList());
            source.setDeliveryTime(MasterData.NO_VALUE);
            source.setQuantumOfSupply(MasterData.NO_VALUE);
            source.setMinShipment(MasterData.NO_VALUE);
            source.setTransportUnitSize(MasterData.NO_VALUE);

            MasterData withDefaultFieldValues = MasterDataDefaultEnhanceService.withDefaultFieldValues(source);

            SoftAssertions.assertSoftly(s -> {
                s.assertThat(withDefaultFieldValues.getDeliveryTime())
                    .isEqualTo(MasterDataDefaultEnhanceService.DEFAULT_DELIVERY_TIME);
                s.assertThat(withDefaultFieldValues.getQuantumOfSupply())
                    .isEqualTo(MasterDataDefaultEnhanceService.DEFAULT_QUANTUM_OF_SUPPLY);
                s.assertThat(withDefaultFieldValues.getMinShipment())
                    .isEqualTo(MasterDataDefaultEnhanceService.DEFAULT_MIN_SHIPMENT);
                s.assertThat(withDefaultFieldValues.getTransportUnitSize())
                    .isEqualTo(MasterDataDefaultEnhanceService.DEFAULT_TRANSPORT_UNIT_SIZE);
                s.assertThat(withDefaultFieldValues.getSupplySchedule())
                    .extracting(SupplyEvent::getDayOfWeek)
                    .isEqualTo(MasterDataDefaultEnhanceService.DEFAULT_SUPPLY_SCHEDULE_DAYS);

                s.assertThat(withDefaultFieldValues).isEqualToIgnoringGivenFields(source,
                    "supplySchedule", "minShipment", "quantumOfSupply", "transportUnitSize", "deliveryTime"
                );

            });
        }
    }

    @Test
    public void withOverridedFieldValues() {
        List<MasterData> dataset = enhancedRandom.objects(MasterData.class, DATASET_SIZE, "categoryId")
            .collect(Collectors.toList());

        for (MasterData source : dataset) {
            // fill master data fields with non empty values
            source.setSupplySchedule(
                enhancedRandom.objects(SupplyEvent.class, 1 + enhancedRandom.nextInt(MAX_RANDOM_INT_BOUND))
                    .collect(Collectors.toList())
            );
            source.setDeliveryTime(1 + enhancedRandom.nextInt(MAX_RANDOM_INT_BOUND));
            source.setQuantumOfSupply(1 + enhancedRandom.nextInt(MAX_RANDOM_INT_BOUND));
            source.setMinShipment(1 + enhancedRandom.nextInt(MAX_RANDOM_INT_BOUND));
            source.setTransportUnitSize(1 + enhancedRandom.nextInt(MAX_RANDOM_INT_BOUND));

            MasterData withDefaultFieldValues = MasterDataDefaultEnhanceService.withDefaultFieldValues(source);

            SoftAssertions.assertSoftly(s ->
                s.assertThat(withDefaultFieldValues).isEqualToComparingFieldByFieldRecursively(source)
            );
        }
    }
}
