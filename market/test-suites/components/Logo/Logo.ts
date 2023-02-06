'use strict';

import {mergeSuites, importSuite, PageObject, makeSuite} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';

const LinkLevitan = PageObject.get('LinkLevitan');

export default makeSuite('Логотип.', {
    id: 'vendor_auto-238',
    issue: 'VNDFRONT-1292',
    feature: 'Макет',
    environment: 'all',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.allure.runStep('Ожидаем появления логотипа', () => this.logo.waitForExist());
            },
        },
        importSuite('Link', {
            suiteName: 'Ссылка на Яндекс',
            params: {
                caption: 'Яндекс',
                url: buildUrl('external:yandex'),
                comparison: {
                    skipPathname: true,
                },
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.logo, `${LinkLevitan.root}:first-child`);
                },
            },
        }),
        importSuite('Link', {
            suiteName: 'Ссылка на Маркет',
            params: {
                caption: 'Маркет',
                url: buildUrl('external:market-index'),
                comparison: {
                    skipPathname: true,
                },
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.logo, `${LinkLevitan.root}:last-child`);
                },
            },
        }),
    ),
});
