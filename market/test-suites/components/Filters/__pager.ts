'use strict';

import {stringify} from 'query-string';
import {mergeSuites, makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.PagerB2b} pager - пейджер
 * @param {PageObject.PagedList} list - список
 * @param {Object} params
 * @param {number} params.expectedPage - номер страницы для перехода
 * @param {number} params.initialItemsCount - изначальное кол-во элементов в списке
 * @param {number} params.expectedItemsCount - ожидаемое кол-во элементов в списке
 * @param {Function} [params.waitForLoading] - функция ожидания загрузки списка
 * @param {Function} [params.afterFiltration] - функция дополнительных проверок после фильтрации
 */
export default makeSuite('Пагинация.', {
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления пагинации', () => this.pager.waitForExist());
            },
        },
        {
            'При выборе страницы': {
                'список обновляется': makeCase({
                    async test() {
                        const {expectedPage, initialItemsCount, expectedItemsCount, waitForLoading, afterFiltration} =
                            this.params;

                        await this.browser.allure.runStep(`Количество элементов в списке ${initialItemsCount}`, () =>
                            this.list.getItemsCount().should.eventually.be.equal(initialItemsCount),
                        );

                        await this.pager.setPage(expectedPage);

                        if (waitForLoading) {
                            await this.browser.allure.runStep('Ожидаем загрузки списка', () =>
                                waitForLoading.call(this),
                            );
                        } else {
                            await this.list.waitForLoading();
                        }

                        const query = {page: expectedPage};

                        await this.browser.allure.runStep(`В URL прокинулся параметр ${stringify(query)}`, () =>
                            this.browser.getUrl().should.eventually.be.link(
                                {query},
                                {
                                    mode: 'match',
                                    skipProtocol: true,
                                    skipHostname: true,
                                    skipPathname: true,
                                },
                            ),
                        );

                        await this.browser.allure.runStep(
                            `Количество элементов в списке стало ${expectedItemsCount}`,
                            () => this.list.getItemsCount().should.eventually.be.equal(expectedItemsCount),
                        );

                        if (afterFiltration) {
                            await afterFiltration.call(this);
                        }
                    },
                }),
            },
        },
    ),
});
