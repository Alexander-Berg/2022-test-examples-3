'use strict';

import url from 'url';

import {makeCase, makeSuite} from 'ginny';
/**
 * @param {Object} params
 * @param {string} params.userName
 * @param {string} params.page - название страницы для отображения
 * @param {Object} params.user - объект "Пользователь"
 * @param {Object} params.user.alias
 * @param {Object} params.route
 * @param {string} params.route.routeName - имя роуа
 * @param {Object} params.route.routeParams - параметры роута
 * */
export default makeSuite('Страница', {
    id: 'vendor_auto-130',
    issue: 'VNDFRONT-1208',
    feature: 'Доступность страницы',
    environment: 'testing',
    params: {
        userName: 'Пользователь',
    },
    story: {
        недоступна: makeCase({
            test() {
                const {route, user} = this.params;
                const {routeName, routeParams} = route;
                const expectedUrlPromise = this.browser.yaBuildURL(routeName, routeParams);

                const browserUrlPromise = this.browser
                    .vndProfile(user, routeName, routeParams)
                    .then(() => this.browser.getUrl());

                return Promise.all([expectedUrlPromise, browserUrlPromise]).then(([expectedUrl, browserUrl]) =>
                    this.browser.allure.runStep(
                        'Проверяем, что url текущей страницы изменился и не равен предыдущему',
                        // @ts-expect-error(TS2531) найдено в рамках VNDFRONT-4580
                        () => url.parse(browserUrl).pathname.should.be.not.equal(expectedUrl),
                    ),
                );
            },
        }),
    },
});
