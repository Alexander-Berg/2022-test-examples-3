package ru.yandex.market.mbi.partner.registration.services;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.partner.registration.AbstractFunctionalTest;
import ru.yandex.market.mbi.partner.registration.model.AvailableRegionWithModel;
import ru.yandex.market.mbi.partner.registration.placement.type.AvailablePlacementTypesService;
import ru.yandex.market.mbi.partner.registration.placement.type.AvailablePlacementTypesUpdateExecutor;
import ru.yandex.market.mbi.partner.registration.placement.type.yt.AvailablePlacementTypesYtDao;
import ru.yandex.mj.generated.server.model.AvailablePartnerPlacementType;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AvailablePlacementTypesUpdateTest extends AbstractFunctionalTest {

    private AvailablePlacementTypesUpdateExecutor availablePlacementTypesUpdateExecutor;

    @Autowired
    private AvailablePlacementTypesService availablePlacementTypesService;

    private AvailablePlacementTypesYtDao availablePlacementTypesYtDao = mock(AvailablePlacementTypesYtDao.class);


    @BeforeEach
    void init() {
        availablePlacementTypesUpdateExecutor = new AvailablePlacementTypesUpdateExecutor(
                availablePlacementTypesService,
                availablePlacementTypesYtDao
        );
    }

    @Test
    @DisplayName("данные в YT отсутсвуют")
    @DbUnitDataSet(before = "AvailableRegionByModelUpdateTest.csv", after = "AvailableRegionByModelUpdateTest.csv")
    public void emptyYtTable() {
        when(availablePlacementTypesYtDao.getAvailableRegionByModelFromYt()).thenReturn(Collections.emptyList());
        assertThrows(
                IllegalArgumentException.class,
                () -> availablePlacementTypesUpdateExecutor.doJob(null),
                "actualRegions from YT should not be empty"
        );
    }

    @Test
    @DisplayName("данные в YT не изменились")
    @DbUnitDataSet(before = "AvailableRegionByModelUpdateTest.csv", after = "AvailableRegionByModelUpdateTest.csv")
    public void withoutChanges() {
        when(availablePlacementTypesYtDao.getAvailableRegionByModelFromYt()).thenReturn(regionFromYt());
        availablePlacementTypesUpdateExecutor.doJob(null);
    }

    //данные изменились, нужно удалить лишние и добавить новые
    @Test
    @DisplayName("данные в YT изменились")
    @DbUnitDataSet(
            before = "AvailableRegionByModelUpdateTest.before.csv",
            after = "AvailableRegionByModelUpdateTest.after.csv"
    )
    public void changeData() {
        when(availablePlacementTypesYtDao.getAvailableRegionByModelFromYt()).thenReturn(regionFromYt());
        availablePlacementTypesUpdateExecutor.doJob(null);
    }

    private List<AvailableRegionWithModel> regionFromYt() {
        return List.of(
                new AvailableRegionWithModel(114765, AvailablePartnerPlacementType.valueOf("FBY")),
                new AvailableRegionWithModel(118515, AvailablePartnerPlacementType.valueOf("FBY")),
                new AvailableRegionWithModel(118936, AvailablePartnerPlacementType.valueOf("FBY")),
                new AvailableRegionWithModel(2, AvailablePartnerPlacementType.valueOf("FBS")),
                new AvailableRegionWithModel(4, AvailablePartnerPlacementType.valueOf("FBS")),
                new AvailableRegionWithModel(5, AvailablePartnerPlacementType.valueOf("FBS")),
                new AvailableRegionWithModel(6, AvailablePartnerPlacementType.valueOf("FBS"))
        );
    }
}
