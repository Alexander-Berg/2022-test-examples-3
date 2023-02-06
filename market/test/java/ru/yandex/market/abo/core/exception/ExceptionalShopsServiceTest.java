package ru.yandex.market.abo.core.exception;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author antipov93, artemmz, kukabara
 */
public class ExceptionalShopsServiceTest extends EmptyTest {

    public static final ExceptionalShop ACTUAL_EXEPTIONS = new ExceptionalShop();

    static {
        ACTUAL_EXEPTIONS.setDeleted(false);
    }

    @Autowired
    private ExceptionalShopsService exceptionalShopsService;
    @Autowired
    private ExceptionalShopRepo exceptionalShopRepo;

    @Test
    public void testFindAll() {
        exceptionalShopsService.findAll(new ExceptionalShop(), PageRequest.of(0, 100));
        exceptionalShopRepo.findByDeletedFalse();
        exceptionalShopRepo.existsByShopIdAndReasonIdInAndDeletedFalse(774L, List.of(ExceptionalShopReason.CART_DIFF.getId()));
    }

    @Test
    public void testCRUD() {
        long shopId = RND.nextLong();
        ExceptionalShopReason reason = ExceptionalShopReason.CART_DIFF;
        ExceptionalShop saved = exceptionalShopsService.addException(shopId, reason, 1L, "Test");
        assertNotNull(saved);

        assertExistsOne(saved);


        String newManagerComment = "New comment";
        ExceptionalShop updated = exceptionalShopsService.addException(shopId, reason, 1L, newManagerComment);
        assertNotNull(updated);
        assertEquals(newManagerComment, updated.getManagerComment());

        assertExistsOne(updated);

        exceptionalShopsService.deleteException(shopId, reason);
        assertEquals(0L, exceptionalShopsService.findAll(ACTUAL_EXEPTIONS, PageRequest.of(0, 100)).getTotalElements());
        assertFalse(exceptionalShopsService.shopHasException(shopId, reason));
    }

    private void assertExistsOne(ExceptionalShop saved) {
        Page<ExceptionalShop> found = exceptionalShopsService.findAll(ACTUAL_EXEPTIONS, PageRequest.of(0, 100));
        assertEquals(1L, found.getTotalElements());
        assertEquals(saved, found.getContent().stream().findFirst().orElse(null));

        assertTrue(exceptionalShopsService.shopHasException(saved.getShopId(), saved.getReason()));
    }

    @Test
    public void testGetList() {
        assertTrue(exceptionalShopsService.loadShops(ExceptionalShopReason.TEST_SHOP).isEmpty());

        exceptionalShopsService.addException(1L, ExceptionalShopReason.AUTOORDER, 1L, "comment");
        assertFalse(exceptionalShopsService.loadShops(ExceptionalShopReason.AUTOORDER).isEmpty());
    }

}
