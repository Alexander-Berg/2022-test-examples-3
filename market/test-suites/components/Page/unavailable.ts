'use strict';

import url from 'url';

import {makeCase, makeSuite} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';

export default makeSuite('Страница', {
    id: 'vendor_auto-130',
    issue: 'VNDFRONT-1208',
    feature: 'Права доступа',
    environment: 'all',
    params: {
        user: 'Пользователь',
    },
    story: {
        недоступна: makeCase({
            test() {
                const {browser, params} = this;
                const {pageRouteName, routeParams} = params;

                return browser.allure.runStep(
                    'Проверяем, что url текущей страницы изменился и не равен предыдущему',
                    () =>
                        browser.getUrl().then(currentUrl =>
                            // @ts-expect-error(TS2531) найдено в рамках VNDFRONT-4580
                            url.parse(currentUrl).pathname.should.be.not.equal(buildUrl(pageRouteName, routeParams)),
                        ),
                );
            },
        }),
    },
});
