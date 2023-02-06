import _ from 'lodash';
import {clone} from 'ambar';
import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from '@yandex-market/ginny';
import {mergeState, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {
    catalogerMock,
    cpaType1POfferMock,
    cpaType3POfferMock,
    cpaTypeDSBSOfferMock,
    shopInfoMock,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import {
    goodOperationalRating,
    mediumOperationalRating,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/operationalRatingFixture.mock';

// Suites
import CartUpsalePopupSuite from '@self/platform/spec/hermione2/test-suites/blocks/CartUpsalePopup/index.screens';
import SearchSnippetSuite from '@self/platform/spec/hermione2/test-suites/blocks/SearchSnippet/index.screens';
import RatingHintSuite from '@self/platform/spec/hermione2/test-suites/blocks/OperationalRating/index.screens';

// PageObjects
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import SearchSnippetCard from '@self/project/src/components/Search/Snippet/Card/__pageObject';
import SearchSnippetCell from '@self/project/src/components/Search/Snippet/Cell/__pageObject';
import HintWithContent from '@self/project/src/components/HintWithContent/__pageObject';
import OperationalRatingBadge from '@self/project/src/components/OperationalRating/__pageObject/index.desktop';

const offerMocks = [
    {name: '1P Оффер', offerMock: cpaType1POfferMock},
    {name: '3P Оффер', offerMock: cpaType3POfferMock},
    {name: 'DSBS Оффер', offerMock: cpaTypeDSBSOfferMock},
];

const viewTypes = ['grid', 'list'];
const SnippetAndCartPopupSuiteDataSet = viewTypes.flatMap(viewType => offerMocks.map(({name, offerMock}) => ({
    offerMock,
    viewType,
    description: `${name} ${viewType}`,
})));

const goodRatingOfferMock = clone(cpaType3POfferMock);
goodRatingOfferMock.supplier.operationalRating = goodOperationalRating;
const mediumRatingOfferMock = clone(cpaType3POfferMock);
mediumRatingOfferMock.supplier.operationalRating = mediumOperationalRating;

const ratingMocks = [
    {name: 'Хороший рейтинг', offerMock: goodRatingOfferMock},
    {name: 'Средний рейтинг', offerMock: mediumRatingOfferMock},
];
const offerRatingSuiteDataSet = viewTypes.flatMap(viewType => ratingMocks.map(({name, offerMock}) => ({
    offerMock,
    viewType,
    description: `${name} ${viewType}`,
})));

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Каталог', {
    environment: 'kadavr',
    feature: 'catalog',
    story: _.merge(
        createStories(
            SnippetAndCartPopupSuiteDataSet,
            ({offerMock, viewType}) => {
                const suiteParams = {
                    meta: {
                        issue: 'MARKETFRONT-85068',
                    },
                    hooks: {
                        async beforeEach() {
                            await beforeEach.call(this, offerMock, viewType);
                        },
                    },
                    pageObjects: {
                        cartPopup() {
                            return this.browser.createPageObject(CartPopup);
                        },
                        popupContent() {
                            return this.browser.createPageObject(CartPopup, {
                                parent: this.cartPopup,
                                root: `${CartPopup.popupContent}`,
                            });
                        },
                        cartButton() {
                            return this.browser.createPageObject(CartButton);
                        },
                        searchSnippet() {
                            return viewType === 'grid'
                                ? this.browser.createPageObject(SearchSnippetCell)
                                : this.browser.createPageObject(SearchSnippetCard);
                        },
                    },
                };

                return mergeSuites(
                    prepareSuite(SearchSnippetSuite, suiteParams),
                    prepareSuite(CartUpsalePopupSuite, suiteParams)
                );
            }
        ),
        createStories(
            offerRatingSuiteDataSet,
            ({offerMock, viewType}) => {
                const suiteParams = {
                    meta: {
                        issue: 'MARKETFRONT-85068',
                    },
                    hooks: {
                        async beforeEach() {
                            await beforeEach.call(this, offerMock, viewType);
                        },
                    },
                    pageObjects: {
                        searchSnippet() {
                            return viewType === 'grid'
                                ? this.browser.createPageObject(SearchSnippetCell)
                                : this.browser.createPageObject(SearchSnippetCard);
                        },
                        operationalRatingBadge() {
                            return this.browser.createPageObject(OperationalRatingBadge);
                        },
                        operationalRatingHintContent() {
                            return this.browser.createPageObject(HintWithContent, {
                                root: `${HintWithContent.content}`,
                            });
                        },
                    },
                };

                return mergeSuites(
                    prepareSuite(RatingHintSuite, suiteParams)
                );
            }
        )
    ),
});

async function beforeEach(offerMock, viewType) {
    await this.browser.setState('Cataloger.tree', catalogerMock);
    await this.browser.setState('Carter.items', []);
    await this.browser.setState('ShopInfo.collections', shopInfoMock);
    await this.browser.setState('report', mergeState([
        createOffer(offerMock, offerMock.wareId),
        {
            data: {
                search: {
                    total: 1,
                    totalOffers: 1,
                    view: viewType,
                },
            },
        },
    ]));

    await this.browser.yaOpenPage('market:list', {
        'nid': offerMock.categories[0].nid,
        'hid': offerMock.categories[0].id,
        'slug': 'naushniki-i-bluetooth-garnitury',
        'onstock': 1,
        'local-offers-first': 0,
        'viewtype': viewType,
    });
}
