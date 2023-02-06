'use strict';

import {stringify} from 'query-string';
import {mergeSuites, makeSuite, makeCase} from 'ginny';

/**
 * Тест на проверку работы фильтра-чекбокса
 *
 * @param {PageObject.CheckboxB2b} checkbox - Чекбокс-фильтр
 * @param {PageObject.PagedList} list - Список сущностей
 * @param {Object} params
 * @param {number} params.initialItemsCount - количество без фильтрации
 * @param {number} params.filteredItemsCount - количество элементов после фильтрации
 * @param {string} params.paramName - имя устанавливаемого query-параметра начала периода
 * @param {string} params.paramValue - значение устанавливаемого query-параметра начала периода
 */
export default makeSuite('Фильтрация по чекбоксу.', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления чекбокса с фильтром', () => this.checkbox.waitForExist());
            },
        },
        {
            'При переключении фильтра-чекбокса': {
                'список фильтруется корректно': makeCase({
                    async test() {
                        const {initialItemsCount, filteredItemsCount, paramName, paramValue} = this.params;

                        await this.browser.allure.runStep(`Количество элементов в списке ${initialItemsCount}`, () =>
                            this.list.getItemsCount().should.eventually.be.equal(initialItemsCount),
                        );

                        const query = {
                            [paramName]: paramValue,
                        };

                        await this.browser.vndWaitForChangeUrl(() => this.checkbox.click(), true);

                        await this.browser.allure.runStep(`В URL прокинулись параметры ${stringify(query)}`, () =>
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

                        await this.list.waitForLoading();

                        await this.browser.allure.runStep(
                            `Количество элементов в списке стало ${filteredItemsCount}`,
                            () => this.list.getItemsCount().should.eventually.be.equal(filteredItemsCount),
                        );
                    },
                }),
            },
        },
    ),
});
