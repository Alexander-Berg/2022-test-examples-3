import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import HeadBannerAbsenceSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/absence';
import HeadBanner from '@self/platform/spec/page-objects/HeadBanner';

import listCatalogPowerSupply from '../fixtures/listCatalogPowerSupply';

export default makeSuite('Баннеры.', {
    environment: 'kadavr',
    story: {
        'Баннер-растяжка.': mergeSuites(
            prepareSuite(HeadBannerAbsenceSuite, {
                suiteName: 'Поисковая выдача.',
                hooks: {
                    async beforeEach() {
                        await this.browser.yaOpenPage('touch:search', routes.search.default);
                    },
                },
                meta: {
                    id: 'm-touch-2662',
                    issue: 'MOBMARKET-11501',
                },
                pageObjects: {
                    headBanner() {
                        return this.createPageObject(HeadBanner);
                    },
                },
            }),

            prepareSuite(HeadBannerAbsenceSuite, {
                suiteName: 'Листовая категория.',
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('Cataloger.tree', listCatalogPowerSupply);
                        await this.browser.yaOpenPage('touch:list', routes.catalog.list);
                    },
                },
                meta: {
                    id: 'm-touch-2664',
                    issue: 'MOBMARKET-11501',
                },
                pageObjects: {
                    headBanner() {
                        return this.createPageObject(HeadBanner);
                    },
                },
            }),

            prepareSuite(HeadBannerAbsenceSuite, {
                suiteName: 'Мультисерч.',
                hooks: {
                    async beforeEach() {
                        await this.browser.yaOpenPage('touch:multisearch', routes.multisearch.default);
                    },
                },
                meta: {
                    id: 'm-touch-2665',
                    issue: 'MOBMARKET-11501',
                },
                pageObjects: {
                    headBanner() {
                        return this.createPageObject(HeadBanner);
                    },
                },
            })
        ),
    },
});
