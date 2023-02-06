import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {routes} from '@self/platform/spec/hermione/configs/routes';

// suites
import AdultBooksSuite from '@self/platform/spec/hermione/test-suites/blocks/n-geo__panel-snippets/adult_books';
import AdultWarningVisibleTrueSuite from
    '@self/platform/spec/hermione/test-suites/blocks/n-geo__panel-snippets/adult-warning_visible_true';
import GeoPanelHeaderSuite from '@self/platform/spec/hermione/test-suites/blocks/n-geo__panel-header';
import PanelFooterSuite from '@self/platform/spec/hermione/test-suites/blocks/n-geo__panel-footer';
import ProductWarningMedicineSuite from '@self/platform/spec/hermione/test-suites/blocks/n-geo__panel-snippets/medicine_warning';
import CartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton';
import CartButtonCounterSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton/counter';
import GeoSnippetClickout from '@self/platform/spec/hermione/test-suites/blocks/n-geo-snippet/clickout.js';
import ItemCounterCartButtonSuite from '@self/project/src/spec/hermione/test-suites/blocks/ItemCounterCartButton';

// page-objects
import ShopsInfoLink from '@self/platform/widgets/content/geo/GeoPanel/components/ShopsInfoLink/__pageObject';
import GeoPanelPageObject from '@self/platform/widgets/content/geo/GeoPanel/components/GeoPanel/__pageObject__';
import GeoFilterGroupPageObject from '@self/platform/components/GeoFilterGroup/__pageObject__';
import HintWithContentPageObject from '@self/project/src/components/HintWithContent/__pageObject';
import GeoSnippetPageObject from '@self/platform/widgets/content/geo/GeoPanel/components/GeoSnippet/__pageObject__';
import ProductWarningsPageObject from '@self/project/src/components/ProductWarnings/__pageObject__';
import ShopName from '@self/project/src/components/ShopName/__pageObject';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CounterCartButton from '@self/project/src/components/CounterCartButton/__pageObject';
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';

import {
    pickupType,
    clickoutButton,
    cpaButton,
    medicineWarnings,
    adultWarnings,
    mobileCategory,
    createOfferMock,
    encryptedUrl,
} from './mocks/offer';


const ADULT_COOKIE_NAME = 'adult';
const ADULT_COOKIE_VALUE = '1';

const pickupOfferWithAdultWarnings = createOfferMock(
    pickupType,
    adultWarnings
);

const mobilePickupOffer = createOfferMock(
    pickupType,
    mobileCategory,
    clickoutButton
);

const mobilePickupOfferCPA = createOfferMock(
    pickupType,
    mobileCategory,
    cpaButton
);

const stroeOfferSnippet = createOfferMock(
    pickupType
);

const routeWithMobilePickupOffer = {
    nid: routes.geo.phones.nid,
    hid: mobilePickupOffer.categories[0].id,
    slug: routes.geo.phones.slug,
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница карты.', {
    feature: 'Карта',
    story: mergeSuites(
        makeSuite('Adult книги.', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    beforeEach() {
                        const reportState = createOffer(pickupOfferWithAdultWarnings);

                        return this.browser
                            .setState('report', reportState)
                            .then(() => this.browser.yaOpenPage('market:geo', routes.geo.adultBook))
                            .then(() => {
                                this.setPageObjects({
                                    geoSnippet: () => this.createPageObject(GeoSnippetPageObject, {
                                        parent: GeoPanelPageObject.root,
                                    }),
                                    productWarnings: () => this.createPageObject(ProductWarningsPageObject, {
                                        parent: GeoSnippetPageObject.root,
                                    }),
                                });
                            });
                    },
                },
                prepareSuite(AdultBooksSuite)
            ),
        }),

        makeSuite('Adult контент.', {
            environment: 'kadavr',
            story: {
                'С установленной кукой adult.': mergeSuites(
                    {
                        async beforeEach() {
                            const reportState = createOffer(pickupOfferWithAdultWarnings);

                            await this.browser.setState('report', reportState);
                            await this.browser.yaSetCookie({name: ADULT_COOKIE_NAME, value: ADULT_COOKIE_VALUE});
                            return this.browser.yaOpenPage('market:geo', routes.geo.adult);
                        },
                    },
                    prepareSuite(AdultWarningVisibleTrueSuite, {
                        pageObjects: {
                            geoSnippet() {
                                return this.createPageObject(GeoSnippetPageObject, {
                                    parent: GeoPanelPageObject.root,
                                });
                            },
                            productWarnings() {
                                return this.createPageObject(ProductWarningsPageObject, {
                                    parent: GeoSnippetPageObject.root,
                                });
                            },
                        },
                    })
                ),
            },
        }),

        makeSuite('Дисклеймер (лекарства).', {
            environment: 'kadavr',
            story: prepareSuite(ProductWarningMedicineSuite, {
                hooks: {
                    async beforeEach() {
                        const offerId = 'uQizLmsYjkLixn5SRhgitQ';
                        const pickupOfferWithMedicineWarnings = createOfferMock(
                            pickupType,
                            medicineWarnings
                        );
                        const offer = createOffer(pickupOfferWithMedicineWarnings, offerId);

                        await this.browser.setState('report', offer);
                        await this.browser.yaOpenPage('market:geo', routes.geo.medicine);
                    },
                },
                pageObjects: {
                    geoSnippet() {
                        return this.createPageObject(GeoSnippetPageObject, {
                            parent: GeoPanelPageObject.root,
                        });
                    },
                    productWarnings() {
                        return this.createPageObject(ProductWarningsPageObject, {
                            parent: GeoSnippetPageObject.root,
                        });
                    },
                },
                meta: {
                    id: 'marketfront-1111',
                    issue: 'MARKETVERSTKA-25320',
                },
            }),
        }),

        makeSuite('Фильтры.', {
            environment: 'testing',
            story: mergeSuites(
                {
                    beforeEach() {
                        return this.browser.yaOpenPage('market:geo', routes.geo.phones);
                    },
                },
                prepareSuite(GeoPanelHeaderSuite, {
                    pageObjects: {
                        geoPanel() {
                            return this.createPageObject(GeoPanelPageObject);
                        },
                        geoSnippet() {
                            return this.createPageObject(
                                GeoSnippetPageObject,
                                {parent: GeoPanelPageObject.root}
                            );
                        },
                        geoFilterGroup() {
                            return this.createPageObject(
                                GeoFilterGroupPageObject,
                                {parent: GeoPanelPageObject.root}
                            );
                        },
                        tooltip() {
                            return this.createPageObject(HintWithContentPageObject);
                        },
                    },
                })
            ),
        }),

        makeSuite('Сниппет CPA-оффера.', {
            issue: 'MARKETFRONT-13258',
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        const reportState = createOffer(mobilePickupOfferCPA, mobilePickupOfferCPA.wareId);

                        await this.browser.setState('report', reportState);
                        await this.browser.setState('Carter.items', []);
                        return this.browser.yaOpenPage('market:geo', routes.geo.phones);
                    },
                },

                prepareSuite(CartButtonSuite),
                prepareSuite(CartButtonCounterSuite),
                prepareSuite(ItemCounterCartButtonSuite, {
                    params: {
                        counterStep: mobilePickupOfferCPA.bundleSettings.quantityLimit.step,
                        offerId: mobilePickupOfferCPA.wareId,
                    },
                    meta: {
                        id: 'marketfront-4195',
                    },
                    pageObjects: {
                        parent() {
                            return this.createPageObject(GeoSnippetPageObject);
                        },
                        cartButton() {
                            return this.createPageObject(CartButton, {
                                parent: GeoSnippetPageObject.root,
                            });
                        },
                        counterCartButton() {
                            return this.createPageObject(CounterCartButton, {
                                parent: GeoSnippetPageObject.root,
                            });
                        },
                        cartPopup() {
                            return this.createPageObject(CartPopup);
                        },
                    },
                })
            ),
        }),
        makeSuite('Панель.', {
            environment: 'kadavr',
            story: prepareSuite(GeoSnippetClickout, {
                hooks: {
                    async beforeEach() {
                        const offer = createOffer(stroeOfferSnippet);
                        const reportState = mergeState([offer, {
                            data: {
                                search: {
                                    groupBy: 'shop',
                                    total: 25,
                                    totalOffers: 25,
                                    shops: 1,
                                    totalShopsBeforeFilters: 27,
                                    shopOutlets: 1,
                                },
                            },
                        }]);

                        await this.browser.setState('report', reportState);

                        return this.browser.yaOpenPage('market:geo', routeWithMobilePickupOffer);
                    },
                },
                pageObjects: {
                    geoSnippet() {
                        return this.createPageObject(GeoSnippetPageObject, {
                            root: GeoPanelPageObject.root,
                        });
                    },
                    shopName() {
                        return this.createPageObject(ShopName, {
                            root: this.geoSnippet,
                        });
                    },
                },
                params: {
                    url: encryptedUrl,
                },
            }),
        }),

        makeSuite('Сниппеты выбранного товара.', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        const reportState = createOffer(mobilePickupOffer, 'dLqD872pXeNYO5g_kmW31w');

                        await this.browser.setState('report', reportState);
                        return this.browser.yaOpenPage('market:geo', routes.geo.offerOutlets);
                    },
                }
            ),
        }),

        // Тест заскипан. Чтобы его починить, нужно поддержать offerShowPlace в кадаврике - сейчас его там нет, кажется
        makeSuite('Футер панели карты.', {
            environment: 'kadavr',
            story: prepareSuite(PanelFooterSuite, {
                pageObjects: {
                    geo() {
                        return this.createPageObject(ShopsInfoLink);
                    },
                },
                hooks: {
                    async beforeEach() {
                        const reportState = createOffer(mobilePickupOffer);

                        await this.browser.setState('report', reportState);
                        return this.browser.yaOpenPage('market:geo', routes.geo.phones);
                    },
                },
            }),
        })
    ),
});
