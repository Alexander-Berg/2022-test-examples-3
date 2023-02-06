// page-objects
import OperationalRatingBadge from '@self/project/src/components/OperationalRating/__pageObject/index.touch';
import OperationalRatingDrawer from '@self/platform/widgets/content/OperationalRatingDrawer/__pageObject';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
// helpers
import COOKIES from '@self/platform/constants/cookie';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {hideScrollbar} from '@self/platform/spec/gemini/helpers/hide';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {setCookies} from '@yandex-market/gemini-extended-actions';
import {clone} from 'ambar';

// kadavr
import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
// mocks
import productWithCPADO from '@self/project/src/spec/gemini/fixtures/cpa/mocks/productWithCPADO.mock';
import {
    cpaType3POfferMock,
    shopInfoMock,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import dataFixture from '@self/project/src/spec/gemini/fixtures/cpa/mocks/dataFixture.mock';
import {
    goodOperationalRating,
    mediumOperationalRating,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/operationalRatingFixture.mock';

const extraCookies = [
    ...DEFAULT_COOKIES,
    {
        name: COOKIES.HEAD_SCROLL_BANNER_HIDDEN,
        value: '1',
    },
];

const goodRatingOfferMock = clone(cpaType3POfferMock);
goodRatingOfferMock.supplier.operationalRating = goodOperationalRating;
const mediumRatingOfferMock = clone(cpaType3POfferMock);
mediumRatingOfferMock.supplier.operationalRating = mediumOperationalRating;


function generateTestSuites(offerMock) {
    const cpaDO = productWithCPADO.generateStateAndDataFromMock(offerMock);

    return [
        {
            suiteName: 'OfferCard',
            url: `/offer/${offerMock.wareId}`,
            before(actions) {
                createSession.call(actions);
                setState.call(actions, 'Carter.items', []);
                setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                setState.call(actions, 'report', mergeState([
                    cpaDO.state,
                    dataFixture,
                    {
                        data: {
                            search: {
                                total: 1,
                                totalOffers: 1,
                            },
                        },
                    },
                ]));
                // Нужно ставить куки после обновления стейта кадаврика чтобы страница перезагрузилась
                setCookies.setCookies.call(actions, extraCookies);
                disableAnimations(actions);
                hideScrollbar(actions);
            },
            after(actions) {
                deleteSession.call(actions);
            },
            childSuites: [
                {
                    suiteName: 'StarShouldBeVisibleNearShopName',
                    selector: MainSuite.selector,
                    capture(actions) {
                        actions.waitForElementToShow(OperationalRatingBadge.root);
                    },
                },
                {
                    suiteName: 'clickOnStarShouldOpenADrawer',
                    before(actions) {
                        actions.click(OperationalRatingBadge.root);
                        actions.waitForElementToShow(OperationalRatingDrawer.content);
                    },
                    selector: OperationalRatingDrawer.content,
                    capture() {
                    },
                },
            ],
        },
    ];
}

export default {
    suiteName: 'OperationalRatingKO[KADAVR]',
    childSuites: [
        {
            suiteName: 'GoodRating',
            childSuites: [
                ...generateTestSuites(goodRatingOfferMock),
            ],
        },
        {
            suiteName: 'MediumRating',
            childSuites: [
                ...generateTestSuites(mediumRatingOfferMock),
            ],
        },
    ],
};
