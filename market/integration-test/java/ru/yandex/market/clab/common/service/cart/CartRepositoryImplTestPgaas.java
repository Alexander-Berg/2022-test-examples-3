package ru.yandex.market.clab.common.service.cart;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.clab.common.service.PageFilter;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Cart;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 22.10.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class CartRepositoryImplTestPgaas extends BasePgaasIntegrationTest {

    @Autowired
    private GoodRepository goodRepository;

    @Autowired
    private CartRepository cartRepository;

    @Test
    public void save() {
        Cart saved = cartRepository.save(new Cart());
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getModifiedDate()).isNotNull();
    }

    @Test
    public void findWithStats() {
        Cart cart = cartRepository.save(RandomTestUtils.randomObject(Cart.class).setId(null));
        Cart cart2 = cartRepository.save(RandomTestUtils.randomObject(Cart.class).setId(null));
        Cart cart3 = cartRepository.save(RandomTestUtils.randomObject(Cart.class).setId(null));
        int itemCount = 13;
        Good lastCart1Good = null;
        LocalDateTime lastChangeDate = RandomTestUtils.randomObject(LocalDateTime.class);
        for (int i = 0; i < itemCount; i++) {
            lastCart1Good = goodRepository.save(RandomTestUtils.randomObject(Good.class, "id", "modifiedDate")
                .setLastChangeDate(lastChangeDate.plusSeconds(i))
                .setCartId(cart.getId()));
        }
        Good lastCart2Good = null;
        for (int i = 0; i < itemCount - 5; i++) {
            lastCart2Good = goodRepository.save(RandomTestUtils.randomObject(Good.class, "id", "modifiedDate")
                .setLastChangeDate(lastChangeDate.plusSeconds(i))
                .setCartId(cart2.getId()));
        }

        List<CartWithStats> withStats = cartRepository.findWithStats(new CartFilter().addId(cart.getId()));

        assertThat(withStats).hasSize(1);
        CartWithStats stat = withStats.get(0);
        assertThat(stat.getCart()).isEqualToComparingFieldByField(cart);
        assertThat(stat.getLastItemModifiedUid()).isEqualTo(lastCart1Good.getModifiedUserId());
        assertThat(stat.getLastItemModifiedDate()).isEqualTo(lastCart1Good.getLastChangeDate());
        assertThat(stat.getItemCount()).isEqualTo(itemCount);

        final int pageSize = 2;
        CartFilter filter = new CartFilter();
        PageFilter pageFilter = PageFilter.page(0, pageSize);

        List<CartWithStats> carts = cartRepository.findWithStats(filter, CartSortBy.ID.asc(), pageFilter);

        assertThat(carts).extracting(CartWithStats::getCart)
            .isSortedAccordingTo(Comparator.comparing(Cart::getId))
            .hasSize(pageSize);

        carts = cartRepository.findWithStats(filter, CartSortBy.CREATE_DATE.asc(), pageFilter);

        assertThat(carts).extracting(CartWithStats::getCart)
            .isSortedAccordingTo(Comparator.comparing(Cart::getCreatedDate))
            .hasSize(pageSize);

        carts = cartRepository.findWithStats(filter, CartSortBy.EXTERNAL_ID.asc(), pageFilter);

        assertThat(carts).extracting(CartWithStats::getCart)
            .isSortedAccordingTo(Comparator.comparing(Cart::getExternalId))
            .hasSize(pageSize);

        carts = cartRepository.findWithStats(filter, CartSortBy.GOOD_LAST_CHANGE_DATE.asc(), pageFilter);

        assertThat(carts)
            .isSortedAccordingTo(Comparator.comparing(CartWithStats::getLastItemModifiedDate))
            .hasSize(pageSize);

        carts = cartRepository.findWithStats(filter, CartSortBy.GOOD_COUNT.asc(), pageFilter);

        assertThat(carts)
            .isSortedAccordingTo(Comparator.comparing(CartWithStats::getItemCount))
            .hasSize(pageSize);
    }

    private Good nextGood(Long cartId, GoodState goodState) {
        return RandomTestUtils.randomObject(Good.class)
            .setId(null)
            .setVerifierComment(null)
            .setCartId(cartId)
            .setState(goodState);
    }

    @Test
    public void goodStateStats() {
        Cart cart = cartRepository.save(RandomTestUtils.randomObject(Cart.class).setId(null));
        Cart cart2 = cartRepository.save(RandomTestUtils.randomObject(Cart.class).setId(null));
        Cart cart3 = cartRepository.save(RandomTestUtils.randomObject(Cart.class).setId(null));

        int itemCount1 = 10;
        // sorted, photo, verified
        goodRepository.save(nextGood(cart.getId(), GoodState.SORTED_TO_CART));
        goodRepository.save(nextGood(cart.getId(), GoodState.SORTED_TO_CART));
        goodRepository.save(nextGood(cart.getId(), GoodState.SORTED_TO_CART));
        goodRepository.save(nextGood(cart.getId(), GoodState.PHOTO));
        goodRepository.save(nextGood(cart.getId(), GoodState.PHOTO));
        goodRepository.save(nextGood(cart.getId(), GoodState.PHOTO_EDITED));
        goodRepository.save(nextGood(cart.getId(), GoodState.EDITING).setVerifierComment("This is a comment!"));
        goodRepository.save(nextGood(cart.getId(), GoodState.EDITED));
        goodRepository.save(nextGood(cart.getId(), GoodState.VERIFYING));
        goodRepository.save(nextGood(cart.getId(), GoodState.VERIFYING).setVerifierComment("This is a comment!"));
        int itemCount2 = 12;
        // photo, verified, sorted
        goodRepository.save(nextGood(cart2.getId(), GoodState.PHOTO));
        goodRepository.save(nextGood(cart2.getId(), GoodState.PHOTO));
        goodRepository.save(nextGood(cart2.getId(), GoodState.PHOTO));
        goodRepository.save(nextGood(cart2.getId(), GoodState.VERIFIED));
        goodRepository.save(nextGood(cart2.getId(), GoodState.VERIFIED));
        goodRepository.save(nextGood(cart2.getId(), GoodState.SORTED_TO_CART));
        goodRepository.save(nextGood(cart2.getId(), GoodState.PHOTOGRAPHED));
        goodRepository.save(nextGood(cart2.getId(), GoodState.PHOTOGRAPHED));
        goodRepository.save(nextGood(cart2.getId(), GoodState.PHOTO_EDITED).setVerifierComment("This is a comment!"));
        goodRepository.save(nextGood(cart2.getId(), GoodState.EDITING));
        goodRepository.save(nextGood(cart2.getId(), GoodState.EDITED).setVerifierComment("This is another comment!"));
        goodRepository.save(nextGood(cart2.getId(), GoodState.VERIFYING));
        //verified, sorted, photo
        int itemCount3 = 9;
        goodRepository.save(nextGood(cart3.getId(), GoodState.VERIFIED));
        goodRepository.save(nextGood(cart3.getId(), GoodState.VERIFIED));
        goodRepository.save(nextGood(cart3.getId(), GoodState.VERIFIED));
        goodRepository.save(nextGood(cart3.getId(), GoodState.SORTED_TO_CART));
        goodRepository.save(nextGood(cart3.getId(), GoodState.SORTED_TO_CART));
        goodRepository.save(nextGood(cart3.getId(), GoodState.PHOTO).setVerifierComment("This is photo comment"));
        goodRepository.save(nextGood(cart3.getId(), GoodState.EDITING));
        goodRepository.save(nextGood(cart3.getId(), GoodState.VERIFYING));
        goodRepository.save(nextGood(cart3.getId(), GoodState.PHOTOGRAPHED)
            .setVerifierComment("this is an unrelated comment"));

        List<CartWithStats> withStats = cartRepository.findWithStats(new CartFilter().addId(cart.getId()));
        assertThat(withStats).hasSize(1);
        CartWithStats stat = withStats.get(0);
        assertThat(stat.getItemCount()).isEqualTo(itemCount1);
        assertThat(stat.getSortedToCartCount()).isEqualTo(3);
        assertThat(stat.getPhotoCount()).isEqualTo(2);
        assertThat(stat.getEditingRequiredCount()).isEqualTo(2);
        assertThat(stat.getVerifyingRequiredCount()).isEqualTo(3);
        assertThat(stat.isHasReturnedToPhotographerGoods()).isFalse();
        assertThat(stat.isHasReturnedToEditorGoods()).isTrue();
        assertThat(stat.isHasReturnedToVerifierGoods()).isTrue();

        withStats = cartRepository.findWithStats(new CartFilter().addId(cart2.getId()));
        assertThat(withStats).hasSize(1);
        stat = withStats.get(0);
        assertThat(stat.getItemCount()).isEqualTo(itemCount2);
        assertThat(stat.getSortedToCartCount()).isEqualTo(1);
        assertThat(stat.getPhotoCount()).isEqualTo(3);
        assertThat(stat.getEditingRequiredCount()).isEqualTo(2);
        assertThat(stat.getVerifyingRequiredCount()).isEqualTo(2);
        assertThat(stat.isHasReturnedToPhotographerGoods()).isFalse();
        assertThat(stat.isHasReturnedToEditorGoods()).isTrue();
        assertThat(stat.isHasReturnedToVerifierGoods()).isTrue();

        withStats = cartRepository.findWithStats(new CartFilter().addId(cart3.getId()));
        assertThat(withStats).hasSize(1);
        stat = withStats.get(0);
        assertThat(stat.getItemCount()).isEqualTo(itemCount3);
        assertThat(stat.getSortedToCartCount()).isEqualTo(2);
        assertThat(stat.getPhotoCount()).isEqualTo(1);
        assertThat(stat.getEditingRequiredCount()).isEqualTo(1);
        assertThat(stat.getVerifyingRequiredCount()).isEqualTo(1);
        assertThat(stat.isHasReturnedToPhotographerGoods()).isTrue();
        assertThat(stat.isHasReturnedToEditorGoods()).isFalse();
        assertThat(stat.isHasReturnedToVerifierGoods()).isFalse();

        final int pageSize = 3;
        CartFilter filter = new CartFilter();
        PageFilter pageFilter = PageFilter.page(0, pageSize);

        List<CartWithStats> carts = cartRepository.findWithStats(filter, CartSortBy.GOOD_SORTED_TO_CART_COUNT.asc(),
            pageFilter);
        assertThat(carts).isSortedAccordingTo(Comparator.comparing(CartWithStats::getSortedToCartCount));

        carts = cartRepository.findWithStats(filter, CartSortBy.GOOD_VERIFYING_REQUIRED_COUNT.asc(), pageFilter);
        assertThat(carts).isSortedAccordingTo(Comparator.comparing(CartWithStats::getVerifyingRequiredCount));

        carts = cartRepository.findWithStats(filter, CartSortBy.GOOD_PHOTO_COUNT.asc(), pageFilter);
        assertThat(carts).isSortedAccordingTo(Comparator.comparing(CartWithStats::getPhotoCount));
    }

}
