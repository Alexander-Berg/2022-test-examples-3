'use strict';

import {mergeSuites, makeSuite, makeCase} from 'ginny';

/**
 * Тест на заголовок списка.
 * @param {PageObject.TitleB2b} title
 * @param {PageObject.PagedList} list
 * @param {Object} params
 * @param {string} params.initialText - изначальный текст заголовка
 * @param {string} params.expectedText - текст заголовка после фильтрации
 * @param {Object} params.routeParams - параметры роута
 */
export default makeSuite('Заголовок.', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления заголовка', () => this.title.waitForExist());
            },
        },
        {
            'При изменении списка': {
                'меняется текст': makeCase({
                    async test() {
                        const {vendor, pageRouteName, initialText, expectedText, routeParams} = this.params;

                        await this.list.waitForLoading();
                        await this.title
                            .getText()
                            .should.eventually.to.be.equal(initialText, `Заголовок соответствует "${initialText}"`);
                        await this.browser.vndOpenPage(pageRouteName, {
                            vendor,
                            ...routeParams,
                        });
                        await this.list.waitForLoading();
                        await this.title
                            .getText()
                            .should.eventually.to.be.equal(expectedText, `Заголовок соответствует "${expectedText}"`);
                    },
                }),
            },
        },
    ),
});
