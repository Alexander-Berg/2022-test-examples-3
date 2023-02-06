package ru.yandex.market.mbo.statistic;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.aliasmaker.AliasMakerService;
import ru.yandex.market.ir.http.MarkupService;
import ru.yandex.market.mbo.user.UserManager;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author kravchenko-aa
 * @date 07/06/2019
 */
public class UserWorkloadServiceTest {
    private static final long TEST_HID = 42L;

    private UserWorkloadService userWorkloadService;
    private AliasMakerService aliasMakerService = Mockito.mock(AliasMakerService.class);
    private MarkupService markupService = Mockito.mock(MarkupService.class);

    @Before
    public void init() {
        userWorkloadService =
            new UserWorkloadService(aliasMakerService, Mockito.mock(UserManager.class), markupService);
    }

    @Test
    public void testServiceIsNotAvailable() {
        when(aliasMakerService.getSupplierOffersCategories(any())).thenThrow(new RuntimeException());
        when(markupService.getTaskConfig(any())).thenThrow(new RuntimeException());
        userWorkloadService.getCategoryWorkload(TEST_HID);
        assertFalse(userWorkloadService.getServiceAvailability().isMarkupWorkerAvailability());
        assertFalse(userWorkloadService.getServiceAvailability().isAliasMakerAvailability());
    }
}
