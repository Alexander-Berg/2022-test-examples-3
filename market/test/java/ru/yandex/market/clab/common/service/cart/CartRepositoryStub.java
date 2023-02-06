package ru.yandex.market.clab.common.service.cart;

import ru.yandex.market.clab.common.db.good.CartUtils;
import ru.yandex.market.clab.common.service.ObservableRepositoryImpl;
import ru.yandex.market.clab.common.service.PageFilter;
import ru.yandex.market.clab.common.service.SortOrder;
import ru.yandex.market.clab.common.service.Sorting;
import ru.yandex.market.clab.common.service.good.GoodFilter;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Cart;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 19.10.2018
 */
public class CartRepositoryStub extends ObservableRepositoryImpl<Cart> implements CartRepository {

    private final AtomicLong idGenerator = new AtomicLong();
    private final List<Cart> carts = new ArrayList<>();
    private final GoodRepository goodRepository;

    public CartRepositoryStub(GoodRepository goodRepository) {
        this.goodRepository = goodRepository;
    }

    @Override
    public Cart getById(long id) {
        return carts.stream()
            .filter(cart -> cart.getId() == id)
            .findFirst()
            .orElse(null);
    }

    @Override
    public boolean delete(long id) {
        return carts.removeIf(cart -> cart.getId() == id);
    }

    @Override
    public Cart save(Cart cart) {
        Cart updated = new Cart(cart);
        if (CartUtils.isNew(cart)) {
            updated.setId(idGenerator.getAndIncrement());
        } else {
            carts.removeIf(g -> cart.getId().equals(g.getId()));
        }
        carts.add(updated);
        return updated;
    }

    @Override
    public List<Cart> find(CartFilter filter) {
        return carts.stream()
            .filter(filter::test)
            .collect(Collectors.toList());
    }

    @Override
    public List<CartWithStats> findWithStats(CartFilter filter, Sorting<CartSortBy> sort, PageFilter page) {
        Stream<CartWithStats> stream = carts.stream()
            .filter(filter::test)
            .map(movement -> new CartWithStats(movement, 0, null, null))
            .peek(mws -> {
                GoodFilter goodFilter = new GoodFilter().setMovementId(mws.getCart().getId());
                List<Good> goods = goodRepository.find(goodFilter);
                int size = goods.size();
                mws.setItemCount(size);
                AtomicInteger sortedToCartCount = new AtomicInteger();
                AtomicInteger photoCount = new AtomicInteger();
                AtomicInteger photoEditedCount = new AtomicInteger();
                AtomicInteger editingCount = new AtomicInteger();
                AtomicInteger editedCount = new AtomicInteger();
                AtomicInteger verifyingCount = new AtomicInteger();
                AtomicBoolean hasReturnToPhotographerGoods = new AtomicBoolean();
                AtomicBoolean hasReturnToEditorGoods = new AtomicBoolean();
                AtomicBoolean hasReturnToVerifierGoods = new AtomicBoolean();

                goods.forEach(good -> {
                    switch (good.getState()) {
                        case SORTED_TO_CART:
                            sortedToCartCount.getAndIncrement();
                            break;
                        case PHOTO:
                            photoCount.getAndIncrement();
                            if (good.getVerifierComment() != null) {
                                hasReturnToPhotographerGoods.set(true);
                            }
                            break;
                        case PHOTO_EDITED:
                            photoEditedCount.getAndIncrement();
                            if (good.getVerifierComment() != null) {
                                hasReturnToEditorGoods.set(true);
                            }
                            break;
                        case EDITING:
                            editingCount.getAndIncrement();
                            if (good.getVerifierComment() != null) {
                                hasReturnToEditorGoods.set(true);
                            }
                            break;
                        case EDITED:
                            editedCount.getAndIncrement();
                            if (good.getVerifierComment() != null) {
                                hasReturnToVerifierGoods.set(true);
                            }
                            break;
                        case VERIFYING:
                            verifyingCount.getAndIncrement();
                            if (good.getVerifierComment() != null) {
                                hasReturnToVerifierGoods.set(true);
                            }
                            break;
                        default:
                            break;
                    }
                });
                mws.setSortedToCartCount(sortedToCartCount.get());
                mws.setPhotoCount(photoCount.get());
                mws.setEditingRequiredCount(photoEditedCount.get() + editingCount.get());
                mws.setVerifyingRequiredCount(editedCount.get() + verifyingCount.get());
                mws.setHasReturnedToPhotographerGoods(hasReturnToPhotographerGoods.get());
                mws.setHasReturnedToEditorGoods(hasReturnToEditorGoods.get());
                mws.setHasReturnedToVerifierGoods(hasReturnToVerifierGoods.get());

                goods.stream()
                    .max(Comparator.comparing(Good::getModifiedDate))
                    .ifPresent(good -> {
                        mws.setLastItemModifiedUid(good.getModifiedUserId());
                        mws.setLastItemModifiedDate(good.getModifiedDate());
                    });
            });

        if (sort != null) {
            stream = stream.sorted(sortBy(sort));
        }

        if (page != null) {
            stream = stream.skip(page.getOffset()).limit(page.getLimit());
        }

        return stream.collect(Collectors.toList());
    }

    @Override
    public long count(CartFilter filter) {
        return carts.stream()
            .filter(filter::test)
            .count();
    }

    private static Comparator<? super CartWithStats> sortBy(Sorting<CartSortBy> sort) {
        switch (sort.getField()) {
            case ID:
                Comparator<CartWithStats> comparator = Comparator.comparing(cws -> cws.getCart().getId());
                if (sort.getOrder() == SortOrder.DESC) {
                    comparator = comparator.reversed();
                }
                return comparator;
            default:
                throw new IllegalArgumentException();
        }
    }
}

