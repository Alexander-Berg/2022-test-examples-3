import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CartPopup from '@self/platform/spec/page-objects/widgets/content/CartPopup';
import SearchSnippetCartButton from '@self/platform/spec/page-objects/containers/SearchSnippet/CartButton';
import {hideFooter} from '@self/platform/spec/gemini/helpers/hide';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';

const cartPopupSelector = CartPopup.root;

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
    const anyCartButtonSelector = [
        // Старая кнопка "В корзину"
        `${rootSelector} ${CartButton.root}`,
        // Левитановская кнопка "В корзину" на выдаче
        `${rootSelector} ${SearchSnippetCartButton.root}`,
    // Подойдёт любая из двух, поэтому ставим "Или" в селеткоре
    ].join(', ');

    return {
        suiteName: 'AddedToCartPopupOpened',
        selector: cartPopupSelector,
        before(actions, find) {
            disableAnimations(actions);
            // футер скрываем потому, что gemini начинает скроллить страницу, скриншотит не ту область и флапает из-за даты индексации
            hideFooter(actions);
            // Ждём пока появится любая из двух подходящих кнопок "В корзину"
            actions.waitForElementToShow(anyCartButtonSelector);
            // eslint-disable-next-line no-new-func
            actions.executeJS(new Function(`document.querySelector('${rootSelector}').scrollIntoView()`));
            actions.click(find(anyCartButtonSelector));
            actions.waitForElementToShow(cartPopupSelector, 5000);
            actions.wait(500);
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
