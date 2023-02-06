'use strict';

import {makeSuite, mergeSuites, importSuite} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';

const year = new Date().getFullYear();

export default makeSuite('Футер.', {
    feature: 'Футер',
    environment: 'all',
    id: 'vendor_auto-131',
    issue: 'VNDFRONT-1216',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.allure.runStep('Ожидаем появления футера', () => this.footer.waitForExist());
            },
        },
        importSuite('Link', {
            suiteName: 'Ссылка на Яндекс',
            meta: {
                id: 'vendor_auto-643',
                feature: 'Меню',
                environment: 'testing',
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.browser, this.footer.yandexLink);
                },
            },
            params: {
                caption: `© ${year}, Яндекс`,
                url: buildUrl('external:yandex'),
                external: true,
                target: '_blank',
                comparison: {
                    skipPathname: true,
                },
            },
        }),
    ),
});
