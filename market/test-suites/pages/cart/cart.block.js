import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';

export function snippetSuite(rootSelector) {
    return {
        suiteName: 'Snippet',
        selector: rootSelector,
        capture(actions) {
            disableAnimations(actions);
        },
    };
}

export function addedToCartPopupOpenedSuite(rootSelector) {
    return {
        suiteName: 'AddedToCartPopupOpened',
        selector: CartPopup.cartItems,
        before(actions, find) {
            disableAnimations(actions);
            actions.waitForElementToShow(`${rootSelector} ${CartButton.root}`);
            actions.click(find(`${rootSelector} ${CartButton.root}`));
            actions.waitForElementToShow(CartPopup.root, 5000);
            // Тут может пытаться лениво загрузиться прайсдроп
            actions.wait(2500);
        },
        capture() {
        },
    };
}

export function generateCartSuites(rootSelector) {
    return [
        snippetSuite(rootSelector),
        addedToCartPopupOpenedSuite(rootSelector),
    ];
}
