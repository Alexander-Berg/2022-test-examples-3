// page-objects
import OperationalRatingBadge from '@self/project/src/components/OperationalRating/__pageObject/index.desktop';
import HintWithContent from '@self/project/src/components/HintWithContent/__pageObject';
import SearchSnippetCell from '@self/project/src/components/Search/Snippet/Cell/__pageObject';
// helpers
import {clone} from 'ambar';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {createWishlistItem} from '@self/platform/spec/gemini/fixtures/wishlist/wishlist.mock';
// kadavr
import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import productWithCPADO from '@self/project/src/spec/gemini/fixtures/cpa/mocks/productWithCPADO.mock';
import {
    cpaType3POfferMock,
    shopInfoMock,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import {
    goodOperationalRating,
    mediumOperationalRating,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/operationalRatingFixture.mock';

const goodRatingOfferMock = clone(cpaType3POfferMock);
goodRatingOfferMock.supplier.operationalRating = goodOperationalRating;
const mediumRatingOfferMock = clone(cpaType3POfferMock);
mediumRatingOfferMock.supplier.operationalRating = mediumOperationalRating;

function wishlistSuite(offerMock) {
    const cpaDO = productWithCPADO.generateStateAndDataFromMock(offerMock);

    const rootSnippetSelector = SearchSnippetCell.root;
    const badgeSelector = `${rootSnippetSelector} ${OperationalRatingBadge.root}`;
    const hintContentWithoutShadowSelector = `${HintWithContent.content} > div`;

    return {
        suiteName: 'StarAndPopupShouldBeVisibleOnOfferSnippet',
        url: '/my/wishlist',
        before(actions) {
            createSession.call(actions);
            setState.call(actions, 'Carter.items', []);
            setState.call(actions, 'ShopInfo.collections', shopInfoMock);
            setState.call(actions, 'report', cpaDO.state);
            setState.call(actions, 'persBasket', createWishlistItem(offerMock.wareId));
            // Нужно ставить куки после обновления стейта кадаврика чтобы страница перезагрузилась
            setDefaultGeminiCookies(actions);
            disableAnimations(actions);
        },
        after(actions) {
            deleteSession.call(actions);
        },
        selector: [
            rootSnippetSelector,
            hintContentWithoutShadowSelector,
        ],
        capture(actions) {
            // eslint-disable-next-line no-new-func
            actions.executeJS(new Function(`document.querySelector('${rootSnippetSelector}').scrollIntoView()`));
            actions.mouseMove(badgeSelector);
            actions.waitForElementToShow(hintContentWithoutShadowSelector);
        },
    };
}

export default {
    suiteName: 'OperationalRatingWishlist[KADAVR]',
    childSuites: [
        {
            suiteName: 'GoodRating',
            childSuites: [
                wishlistSuite(goodRatingOfferMock),
            ],
        },
        {
            suiteName: 'MediumRating',
            childSuites: [
                wishlistSuite(mediumRatingOfferMock),
            ],
        },
    ],
};
