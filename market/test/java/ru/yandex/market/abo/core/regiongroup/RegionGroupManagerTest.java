package ru.yandex.market.abo.core.regiongroup;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.core.regiongroup.model.AboRegionGroupFailDTO;
import ru.yandex.market.abo.core.regiongroup.model.AboRegionGroupFailureReasonType;
import ru.yandex.market.abo.core.regiongroup.model.AboRegionGroupStatus;
import ru.yandex.market.abo.core.regiongroup.service.RegionGroupFailureReasonService;
import ru.yandex.market.abo.core.regiongroup.service.RegionGroupService;
import ru.yandex.market.abo.core.regiongroup.tarifficator.TarifficatorService;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.RegionGroupStatus;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupStatusDto;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 31.05.18.
 */
public class RegionGroupManagerTest extends EmptyTestWithTransactionTemplate {

    private static final long REG_GROUP_ID = 1L;
    private static final long USER_ID = 1L;
    private static final Long SHOP_ID = 774L;
    private static final String USER_COMMENT = "Комментарий пользователя";
    private static final String BODY = "Текст письма магазину";
    private static final String REG_GROUP_NAME = "mbiRegionGroupName";

    @InjectMocks
    private RegionGroupManager regionGroupManager;
    @Mock
    private RegionGroupService regionGroupService;
    @Mock
    private TarifficatorService tarifficatorService;
    @Mock
    private RegionGroupFailureReasonService regionGroupFailureReasonService;
    @Mock
    private RegionGroupStCreator regionGroupStCreator;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testTurnOnRegionGroups() {
        var passedRegionGroups = List.of(REG_GROUP_ID);
        regionGroupManager.turnOnRegionGroups(passedRegionGroups, SHOP_ID, USER_ID);
        verify(tarifficatorService).saveRegionGroupStatus(
                SHOP_ID, USER_ID, null,
                List.of(new RegionGroupStatusDto()
                        .regionGroupId(REG_GROUP_ID)
                        .status(RegionGroupStatus.SUCCESS)
                )
        );
        verify(regionGroupService).updateStatusByTariffIds(List.of(REG_GROUP_ID), AboRegionGroupStatus.SUCCESS);
    }

    @Test
    public void testTurnOffRegionGroups() {
        var aboRegionGroupFailDTO = mock(AboRegionGroupFailDTO.class);
        when(aboRegionGroupFailDTO.getTarifficatorId()).thenReturn(REG_GROUP_ID);
        when(aboRegionGroupFailDTO.getComment()).thenReturn(USER_COMMENT);

        List<AboRegionGroupFailDTO> dtoList = Collections.singletonList(
                new AboRegionGroupFailDTO(
                        REG_GROUP_ID,
                        new String[]{
                                AboRegionGroupFailureReasonType.NO_DELIVERY.name(),
                                AboRegionGroupFailureReasonType.COURIER_CARD.name()
                        },
                        BODY)
        );
        regionGroupManager.turnOffRegionGroups(SHOP_ID, dtoList, BODY, USER_ID, null, Collections.emptyMap());
        var moderationResult = regionGroupManager.buildFailModerationResults(dtoList, Map.of());
        verify(tarifficatorService).saveRegionGroupStatus(SHOP_ID, USER_ID, BODY, moderationResult);
        verify(regionGroupFailureReasonService).save(regionGroupManager.buildAboRegionGroupFailureReasons(dtoList,
                SHOP_ID, null));
        dtoList.forEach(dto -> {
            Long regionGroupId = dto.getTarifficatorId();
            String userComment = dto.getComment();
            verify(regionGroupService).updateStatusByTariffId(regionGroupId, AboRegionGroupStatus.FAIL, userComment);
        });
    }
}
