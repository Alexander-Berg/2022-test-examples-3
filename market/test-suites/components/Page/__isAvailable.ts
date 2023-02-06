'use strict';

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
    id: 'vendor_auto-129',
    issue: 'VNDFRONT-1207',
    feature: 'Доступность страницы',
    environment: 'testing',
    params: {
        userName: 'Пользователь',
    },
    story: {
        доступна: makeCase({
            test() {
                const {route, user} = this.params;
                const {routeName, routeParams} = route;
                const expectedUrlPromise = this.browser.yaBuildURL(routeName, routeParams);

                const browserUrlPromise = this.browser
                    .vndProfile(user, routeName, routeParams)
                    .then(() => this.browser.getUrl());

                return Promise.all([expectedUrlPromise, browserUrlPromise]).then(([expectedUrl, browserUrl]) =>
                    this.browser.allure.runStep('Проверяем, что url текущей страницы соотвествует ожидаемой', () =>
                        browserUrl.should.be.link(expectedUrl, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                            skipQuery: true,
                        }),
                    ),
                );
            },
        }),
    },
});
