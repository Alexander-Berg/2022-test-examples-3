// page-objects
import SearchResult from '@self/platform/spec/page-objects/SearchResult';
import ShopInfo from '@self/platform/spec/page-objects/containers/SearchSnippet/ShopInfo';
import OperationalRatingDrawer from '@self/platform/widgets/content/OperationalRatingDrawer/__pageObject';

// helpers
import {clone} from 'ambar';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import deleteCookie from '@yandex-market/gemini-extended-actions/actions/deleteCookie';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
// kadavr
import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';
import {
    cpaType3POfferMock,
    shopInfoMock,
    catalogerGridMock,
    catalogerListMock,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import {
    goodOperationalRating,
    mediumOperationalRating,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/operationalRatingFixture.mock';


const goodRatingOfferMock = clone(cpaType3POfferMock);
goodRatingOfferMock.supplier.operationalRating = goodOperationalRating;
const mediumRatingOfferMock = clone(cpaType3POfferMock);
mediumRatingOfferMock.supplier.operationalRating = mediumOperationalRating;

function generateTestSuites(offerMock, viewtype) {
    const snippetRootSelector = SearchResult.root;
    const catalogerMock = viewtype === 'list' ? catalogerListMock
        : catalogerGridMock;

    return [
        {
            suiteName: 'StarAndPopupShouldBeVisibleOnOfferSnippet',
            url: {
                pathname: `/catalog--naushniki-i-bluetooth-garnitury/${offerMock.categories[0].nid}`,
                query: {
                    hid: offerMock.categories[0].id,
                    onstock: 1,
                    'local-offers-first': 0,
                },
            },
            before(actions) {
                createSession.call(actions);
                setState.call(actions, 'Cataloger.tree', catalogerMock);
                setState.call(actions, 'Carter.items', []);
                setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                setState.call(actions, 'report', mergeState([
                    createOffer(offerMock, offerMock.wareId),
                    {
                        data: {
                            search: {
                                total: 1,
                                totalOffers: 1,
                            },
                        },
                    },
                ]));
                setDefaultGeminiCookies(actions);
                disableAnimations(actions);
            },
            after(actions) {
                deleteSession.call(actions);
                // Грязный хак - иначе редиректит в кривую категорию при попытке открыть урл в том же браузере
                deleteCookie.call(actions, 'kadavr_session_id');
                deleteCookie.call(actions, 'kadavr_host_port');
            },
            selector: snippetRootSelector,
            capture(actions) {
                actions.waitForElementToShow(`${snippetRootSelector} ${ShopInfo.operationalRationgBadge}`);
            },
        },
        {
            suiteName: 'clickOnStarShouldOpenADrawer',
            url: {
                pathname: `/catalog--naushniki-i-bluetooth-garnitury/${offerMock.categories[0].nid}`,
                query: {
                    hid: offerMock.categories[0].id,
                    onstock: 1,
                    'local-offers-first': 0,
                },
            },
            before(actions) {
                createSession.call(actions);
                setState.call(actions, 'Cataloger.tree', catalogerMock);
                setState.call(actions, 'Carter.items', []);
                setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                setState.call(actions, 'report', mergeState([
                    createOffer(offerMock, offerMock.wareId),
                    {
                        data: {
                            search: {
                                total: 1,
                                totalOffers: 1,
                            },
                        },
                    },
                ]));
                setDefaultGeminiCookies(actions);
                disableAnimations(actions);
                actions.click(`${snippetRootSelector} ${ShopInfo.operationalRationgBadge}`);
                actions.waitForElementToShow(OperationalRatingDrawer.content);
            },
            after(actions) {
                deleteSession.call(actions);
                // Грязный хак - иначе редиректит в кривую категорию при попытке открыть урл в том же браузере
                deleteCookie.call(actions, 'kadavr_session_id');
                deleteCookie.call(actions, 'kadavr_host_port');
            },
            selector: OperationalRatingDrawer.content,
            capture() {
            },
        },
    ];
}

export default {
    suiteName: 'OperationalRatingCatalog[KADAVR]',
    childSuites: [
        {
            suiteName: 'List',
            childSuites: [
                {
                    suiteName: 'GoodRating',
                    childSuites: [
                        ...generateTestSuites(goodRatingOfferMock, 'list'),
                    ],
                },
                {
                    suiteName: 'MediumRating',
                    childSuites: [
                        ...generateTestSuites(mediumRatingOfferMock, 'list'),
                    ],
                },
            ],
        },
        {
            suiteName: 'Grid',
            childSuites: [
                {
                    suiteName: 'GoodRating',
                    childSuites: [
                        ...generateTestSuites(goodRatingOfferMock, 'grid'),
                    ],
                },
                {
                    suiteName: 'MediumRating',
                    childSuites: [
                        ...generateTestSuites(mediumRatingOfferMock, 'grid'),
                    ],
                },
            ],
        },
    ],
};
