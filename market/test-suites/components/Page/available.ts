'use strict';

import {makeCase, makeSuite} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';

export default makeSuite('Страница', {
    id: 'vendor_auto-129',
    issue: 'VNDFRONT-1207',
    feature: 'Права доступа',
    environment: 'all',
    params: {
        user: 'Пользователь',
    },
    story: {
        доступна: makeCase({
            test() {
                const {browser, params} = this;
                const {pageRouteName, routeParams} = params;

                return browser.allure.runStep('Проверяем, что url текущей страницы соотвествует ожидаемой', () =>
                    browser.getUrl().should.eventually.be.link(buildUrl(pageRouteName, routeParams), {
                        mode: 'match',
                        skipProtocol: true,
                        skipHostname: true,
                        skipQuery: true,
                    }),
                );
            },
        }),
    },
});
