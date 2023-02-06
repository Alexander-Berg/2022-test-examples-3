package ru.yandex.market.clab.common.service.cart;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.clab.common.service.ProcessingErrorType;
import ru.yandex.market.clab.common.service.ProcessingException;
import ru.yandex.market.clab.common.service.category.CategoryService;
import ru.yandex.market.clab.common.service.good.GoodRepositoryStub;
import ru.yandex.market.clab.common.service.good.GoodService;
import ru.yandex.market.clab.common.service.good.GoodServiceImpl;
import ru.yandex.market.clab.common.service.goodtype.GoodTypeRepositoryStub;
import ru.yandex.market.clab.common.service.goodtype.GoodTypeServiceImpl;
import ru.yandex.market.clab.common.service.movement.MovementRepositoryStub;
import ru.yandex.market.clab.common.service.movement.MovementService;
import ru.yandex.market.clab.common.service.movement.MovementServiceImpl;
import ru.yandex.market.clab.common.service.ssbarcode.SsBarcodeRepositoryStub;
import ru.yandex.market.clab.common.service.warehouse.WarehouseRepository;
import ru.yandex.market.clab.common.service.warehouse.WarehouseServiceImpl;
import ru.yandex.market.clab.db.jooq.generated.enums.CartState;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementDirection;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Cart;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Movement;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class CartServiceImplTest {

    private GoodRepositoryStub goodRepositoryStub;
    private GoodService goodService;
    private WarehouseRepository warehouseRepository;
    private CartRepositoryStub cartRepositoryStub;
    private CartService cartService;
    private MovementService movementService;

    @Before
    public void setUp() {
        goodRepositoryStub = new GoodRepositoryStub();
        goodService = new GoodServiceImpl(goodRepositoryStub, new SsBarcodeRepositoryStub());
        cartRepositoryStub = new CartRepositoryStub(goodRepositoryStub);
        warehouseRepository = Mockito.mock(WarehouseRepository.class);
        movementService = new MovementServiceImpl(new MovementRepositoryStub(goodRepositoryStub, warehouseRepository),
            goodService, new WarehouseServiceImpl(warehouseRepository, 1L));
        setUpService();
    }

    private void setUpService() {
        cartService = new CartServiceImpl(cartRepositoryStub, goodService,
            new GoodTypeServiceImpl(new GoodTypeRepositoryStub(), Mockito.mock(CategoryService.class)),
            movementService);
    }

    private Pair<Cart, List<Good>> prepareCart(String... goodIds) {
        String externalId = "test-cart";
        Movement movement = movementService.createMovement(new Movement()
            .setDirection(MovementDirection.INCOMING)
            .setExternalId(externalId));
        List<Good> goods = Stream.of(goodIds).map(s ->
            goodService.createGood(new Good()
                .setWhBarcode(s)
                .setModifiedDate(LocalDateTime.now())
                .setIncomingMovementId(movement.getId()))).collect(Collectors.toList());
        goods.forEach(good -> goodService.updateGoodState(good.getId(), GoodState.ACCEPTED));
        movementService.acceptMovement(movement.getId());
        Cart cart = cartService.createCart(externalId);
        Long cartId = cart.getId();
        goods = goods.stream()
            .map(good -> cartService.addGoodToCart(good.getWhBarcode(), cartId))
            .collect(Collectors.toList());
        cartService.setCartState(cartId, CartState.ASSEMBLY);
        return Pair.of(cart, goods);
    }


    @Test
    public void shouldNotGoodStateIfFlagDisabled() {
        Pair<Cart, List<Good>> cartGood = prepareCart("test-good");

        cartService.setCartPhotoStarted(cartGood.getKey().getId());

        Assertions.assertThat(cartRepositoryStub.getById(cartGood.getKey().getId()).getState())
            .isEqualTo(CartState.PHOTO);
        Assertions.assertThat(goodRepositoryStub.getByIds(
            cartGood.getValue().stream().map(Good::getId).collect(Collectors.toList())))
            .extracting(Good::getState)
            .containsExactly(GoodState.SORTED_TO_CART);
    }

    @Test
    public void cancelPhotoShouldRevertGoodsStateIfFlagEnabled() {
        Pair<Cart, List<Good>> cartGood = prepareCart("test-good");

        cartService.setCartPhotoStarted(cartGood.getKey().getId());
        cartService.cancelPhotoStarted(cartGood.getKey().getId());

        Assertions.assertThat(cartRepositoryStub.getById(cartGood.getKey().getId()).getState())
            .isEqualTo(CartState.ASSEMBLY);
        Assertions.assertThat(goodRepositoryStub.getByIds(
            cartGood.getValue().stream().map(Good::getId).collect(Collectors.toList())))
            .extracting(Good::getState)
            .containsExactly(GoodState.SORTED_TO_CART);
    }


    @Test
    public void shouldUpdateGoodStateWithPhoto() {
        Pair<Cart, List<Good>> cartGood = prepareCart("test-good1", "test-good2", "test-good3");

        cartService.setCartPhotoStarted(cartGood.getKey().getId());
        Good good = cartGood.getValue().get(0);
        cartService.setGoodPhotoStarted(cartGood.getKey().getId(), good.getWhBarcode());

        CartWithStats cart = cartService.getCartWithStats(cartGood.getKey().getId());
        Assertions.assertThat(cart.getItemCount()).isEqualTo(3);
        Assertions.assertThat(cart.getSortedToCartCount()).isEqualTo(2);
        Assertions.assertThat(cart.getPhotoCount()).isEqualTo(1);
    }

    @Test
    public void shouldThrowExceptionIfGoodIsNotInSortedToCartState() {
        Pair<Cart, List<Good>> cartGood = prepareCart("test-good1");

        cartService.setCartPhotoStarted(cartGood.getKey().getId());
        Good good = cartGood.getValue().get(0);
        cartService.setGoodPhotoStarted(cartGood.getKey().getId(), good.getWhBarcode());

        Assertions.assertThatThrownBy(() ->
            cartService.setGoodPhotoStarted(cartGood.getKey().getId(), good.getWhBarcode()))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.GOOD_STATE_CHANGE_INVALID);
    }

    @Test
    public void testGetCartByGoodWh() {
        String goodWhBarcode = "WhBarcode";
        Pair<Cart, List<Good>> cartGood = prepareCart(goodWhBarcode);

        List<CartWithStats> result = cartService.searchCartsByGood(goodWhBarcode);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).extracting(CartWithStats::getCart).isEqualTo(cartGood.getKey());
        assertThat(result.get(0)).extracting(CartWithStats::getItemCount).isEqualTo(1);
    }

    @Test
    public void testGetMovementByGoodMskuId() {
        long goodMskuId = 12345678L;
        Pair<Cart, List<Good>> cartGood = prepareCart("test-good1");
        goodService.saveGood(cartGood.getValue().get(0).setMskuId(goodMskuId));

        List<CartWithStats> result = cartService.searchCartsByGood(String.valueOf(goodMskuId));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).extracting(CartWithStats::getCart).isEqualTo(cartGood.getKey());
        assertThat(result.get(0)).extracting(CartWithStats::getItemCount).isEqualTo(1);
    }

    @Test
    public void testHandleMissingGoodByKeyParameter() {
        Assertions.assertThatThrownBy(() ->
            cartService.searchCartsByGood(String.valueOf(12345678L)))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.GOOD_MISSING);
    }
}
