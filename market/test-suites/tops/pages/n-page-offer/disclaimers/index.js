import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {createOffer} from '@self/platform/spec/hermione/helpers/shopRating';
import {
    OFFER_ID as CREDIT_OFFER_ID,
    searchResultsWithOfferState as searchResultsWithCreditOfferState,
} from '@self/platform/spec/hermione/fixtures/credit';
// suites
import WarningSuite from '@self/platform/spec/hermione/test-suites/blocks/n-warning';
import ShopInfoCreditDisclaimerSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/creditDisclaimer';
import ShopInfoDrugsDisclaimerSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/drugsDisclaimer';
// page-objects
import OfferVisitCardSpecs from '@self/platform/widgets/content/OfferVisitCardSpecs/__pageObject';
import Warning from '@self/platform/spec/page-objects/n-warning';
import ProductWarning from '@self/project/src/components/ProductWarning/__pageObject/index.desktop';
import GeoSnippet from '@self/platform/spec/page-objects/n-geo-snippet';
import LegalInfo from '@self/platform/spec/page-objects/components/LegalInfo';

const FILTERS = [{
    id: 14871214,
    type: 'enum',
    name: 'Цвет товара',
    subType: 'image_picker',
    kind: 2,
    position: 1,
    values: [{
        checked: true,
        found: 1,
        value: 'золотой',
        id: 15266392,
    }],
}];

export default makeSuite('Дисклеймеры.', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Блок предупреждений для лекарств в описании оффера.', {
            story: prepareSuite(WarningSuite, {
                params: {
                    pattern: /^Есть противопоказания, посоветуйтесь с врачом\.$/,
                    type: 'medicine',
                },
                hooks: {
                    async beforeEach() {
                        const offerId = 'uQizLmsYjkLixn5SRhgitQ';
                        const offer = createOffer({
                            filters: FILTERS,
                            shop: {
                                id: 1,
                                name: 'shop',
                                slug: 'shop',
                            },
                            warnings: {
                                common: [
                                    {
                                        type: 'drugs_with_delivery',
                                        value: {
                                            full: 'Есть противопоказания, посоветуйтесь с врачом.',
                                            short: 'Есть противопоказания, уточните у врача.',
                                        },
                                    },
                                ],
                            },
                            urls: {
                                encrypted: '/redir/encrypted',
                                decrypted: '/redir/decrypted',
                                offercard: '/redir/offercard',
                                geo: '/redir/geo',
                            },
                        }, offerId);
                        await this.browser.setState('report', offer);
                        await this.browser.yaOpenPage('market:offer', {offerId});
                    },
                },
                pageObjects: {
                    disclaimer() {
                        return this.createPageObject(
                            ProductWarning,
                            {
                                parent: OfferVisitCardSpecs.root,
                            }
                        );
                    },
                },
                meta: {
                    id: 'marketfront-865',
                    issue: 'MARKETVERSTKA-24993',
                },
            }),
        }),
        makeSuite('Блок предупреждений для лекарств на карте.', {
            story: prepareSuite(WarningSuite, {
                params: {
                    pattern: /^Есть противопоказания, посоветуйтесь с врачом\.$/,
                    type: 'medicine',
                },
                hooks: {
                    async beforeEach() {
                        // eslint-disable-next-line market/ginny/no-skip
                        return this.skip('MARKETFRONT-9428: Починка гео-тестов на КО');

                        /* eslint-disable no-unreachable */
                        const offerId = 'uQizLmsYjkLixn5SRhgitQ';
                        const offer = createOffer({
                            shop: {
                                id: 1,
                                name: 'shop',
                                slug: 'shop',
                                outletsCount: 1,
                            },
                            warnings: {
                                common: [
                                    {
                                        type: 'drugs_with_delivery',
                                        value: {
                                            full: 'Есть противопоказания, посоветуйтесь с врачом.',
                                            short: 'Есть противопоказания, уточните у врача.',
                                        },
                                    },
                                ],
                            },
                            urls: {
                                encrypted: '/redir/encrypted',
                                decrypted: '/redir/decrypted',
                                offercard: '/redir/offercard',
                                geo: '/redir/geo',
                            },
                        }, offerId);
                        await this.browser.setState('report', offer);
                        await this.browser.yaOpenPage('market:offer-geo', {offerId});
                        await this.browser.allure.runStep('Ждем появления предупреждения на карте', () =>
                            this.browser.waitForVisible(GeoSnippet.getRowChild(6))
                        );
                        /* eslint-enable no-unreachable */
                    },
                },
                pageObjects: {
                    disclaimer() {
                        return this.createPageObject(
                            Warning,
                            {
                                parent: GeoSnippet.getRowChild(6),
                            }
                        );
                    },
                },
                meta: {
                    id: 'marketfront-867',
                    issue: 'MARKETVERSTKA-24995',
                },
            }),
        }),
        makeSuite('Блок предупреждений для оружия в описании оффера.', {
            story: prepareSuite(WarningSuite, {
                params: {
                    pattern: /^Конструктивно сходные с оружием изделия\.$/,
                    type: 'default',
                },
                hooks: {
                    async beforeEach() {
                        const offerId = 'uQizLmsYjkLixn5SRhgitQ';
                        const offer = createOffer({
                            filters: FILTERS,
                            shop: {
                                id: 1,
                                name: 'shop',
                                slug: 'shop',
                            },
                            warnings: {
                                common: [
                                    {
                                        type: 'weapons',
                                        value: {
                                            full: 'Конструктивно сходные с оружием изделия.',
                                            short: 'Конструктивно сходные с оружием изделия.',
                                        },
                                    },
                                ],
                            },
                            urls: {
                                encrypted: '/redir/encrypted',
                                decrypted: '/redir/decrypted',
                                offercard: '/redir/offercard',
                                geo: '/redir/geo',
                            },
                        }, offerId);
                        await this.browser.setState('report', offer);
                        await this.browser.yaOpenPage('market:offer', {offerId});
                    },
                },
                pageObjects: {
                    disclaimer() {
                        return this.createPageObject(
                            ProductWarning,
                            {
                                parent: OfferVisitCardSpecs.root,
                            }
                        );
                    },
                },
                meta: {
                    id: 'marketfront-866',
                    issue: 'MARKETVERSTKA-24994',
                },
            }),
        }),
        makeSuite('Блок предупреждений для оружия на карте.', {
            story: prepareSuite(WarningSuite, {
                params: {
                    pattern: /^Конструктивно сходные с оружием изделия\.$/,
                    type: 'default',
                },
                hooks: {
                    async beforeEach() {
                        // eslint-disable-next-line market/ginny/no-skip
                        return this.skip('MARKETFRONT-9428: Починка гео-тестов на КО');

                        /* eslint-disable no-unreachable */
                        const offerId = 'uQizLmsYjkLixn5SRhgitQ';
                        const offer = createOffer({
                            shop: {
                                id: 1,
                                name: 'shop',
                                slug: 'shop',
                                outletsCount: 1,
                            },
                            warnings: {
                                common: [
                                    {
                                        type: 'weapons',
                                        value: {
                                            full: 'Конструктивно сходные с оружием изделия.',
                                            short: 'Конструктивно сходные с оружием изделия.',
                                        },
                                    },
                                ],
                            },
                            urls: {
                                encrypted: '/redir/encrypted',
                                decrypted: '/redir/decrypted',
                                offercard: '/redir/offercard',
                                geo: '/redir/geo',
                            },
                        }, offerId);
                        await this.browser.setState('report', offer);
                        await this.browser.yaOpenPage('market:offer-geo', {offerId});
                        await this.browser.allure.runStep('Ждем появления предупреждения на карте', () =>
                            this.browser.waitForVisible(GeoSnippet.getRowChild(6))
                        );
                        /* eslint-enable no-unreachable */
                    },
                },
                pageObjects: {
                    disclaimer() {
                        return this.createPageObject(
                            Warning,
                            {
                                parent: GeoSnippet.getRowChild(6),
                            }
                        );
                    },
                },
                meta: {
                    id: 'marketfront-868',
                    issue: 'MARKETVERSTKA-24996',
                },
            }),
        }),
        prepareSuite(ShopInfoDrugsDisclaimerSuite, {
            meta: {
                id: 'marketfront-3871',
                issue: 'MARKETFRONT-7776',
            },
            pageObjects: {
                shopsInfo() {
                    return this.createPageObject(LegalInfo);
                },
            },
            hooks: {
                async beforeEach() {
                    const offerId = 'uQizLmsYjkLixn5SRhgitQ';
                    const offer = createOffer({
                        shop: {
                            id: 1,
                            name: 'shop',
                            slug: 'shop',
                            outletsCount: 1,
                        },
                        urls: {
                            encrypted: '/redir/encrypted',
                            decrypted: '/redir/decrypted',
                            offercard: '/redir/offercard',
                            geo: '/redir/geo',
                        },
                    }, offerId);
                    await this.browser.setState('report', offer);
                    return this.browser.yaOpenPage('market:offer', {offerId});
                },
            },
        }),
        prepareSuite(ShopInfoCreditDisclaimerSuite, {
            meta: {
                id: 'marketfront-3700',
                issue: 'MARKETVERSTKA-35817',
            },
            pageObjects: {
                shopsInfo() {
                    return this.createPageObject(LegalInfo);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', searchResultsWithCreditOfferState);
                    return this.browser.yaOpenPage('market:offer', {offerId: CREDIT_OFFER_ID});
                },
            },
        })
    ),
});
