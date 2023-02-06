import Header2 from '@self/platform/spec/page-objects/header2';
import Header2Menu from '@self/platform/spec/page-objects/header2-menu';
import CartEntryPoint from '@self/root/src/components/CartEntryPoint/__pageObject/index.desktop';

export default {
    suiteName: 'Header',
    selector: Header2.mainContent,
    capture: {
        plain() {
        },
        hoveredWishlist(actions, find) {
            actions
                .mouseMove(find(Header2Menu.wishlistItem))
                .wait(1000);
        },
        hoveredOrders(actions, find) {
            actions
                .mouseMove(find(Header2Menu.ordersItem))
                .wait(1000);
        },
        hoveredCart(actions, find) {
            actions
                .mouseMove(find(CartEntryPoint.root))
                .wait(1000);
        },
    },
};
