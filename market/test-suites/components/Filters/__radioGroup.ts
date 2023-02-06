'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ListContainer} list - список элементов
 * @param {PageObject.RadioGroup} radioGroup
 * @param {Object} params
 * @param {boolean} params.user - пользователь
 * @param {number} params.initialItemsCount - количесто без фильтрации
 * @param {number} params.filteredItemsCount - количество элементов после фильтрации
 * @param {string} params.tabName - Имя таба, на который нужно кликнуть
 * @param {string} params.queryParamName - Имя query-параметра фильтра
 * @param {string} params.queryParamValue - Ожидаемое значение query-параметра
 */
export default makeSuite('Фильтр radioGroup', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При выборе элемента': {
            'Фильтрует список': makeCase({
                async test() {
                    const {
                        tabName,
                        tabIndex,
                        tabDisplayName,
                        initialItemsCount,
                        filteredItemsCount,
                        queryParamValue,
                        queryParamName,
                    } = this.params;

                    await this.list.waitForLoading();
                    await this.list
                        .getItemsCount()
                        .should.eventually.be.equal(initialItemsCount, `Отображается элементов: ${initialItemsCount}`);

                    const setFilterTask = tabIndex
                        ? () => this.radioGroup.clickItemByIndex(tabIndex)
                        : () => this.radioGroup.clickItemByTitle(tabName);

                    const valueName = tabDisplayName || queryParamValue;

                    const setFilter = this.browser.allure.runStep(
                        `Устанавливаем фильтр "${queryParamName}" в значение "${valueName}"`,
                        setFilterTask,
                    );

                    const url = await this.browser.vndWaitForChangeUrl(() => setFilter);

                    await this.browser.allure.runStep(
                        `Проверяем что в урл прокинулся query-параметр &${queryParamName}=${queryParamValue}`,
                        () =>
                            Promise.resolve(url).should.eventually.be.link(
                                {
                                    query: {
                                        [queryParamName]: queryParamValue,
                                    },
                                },
                                {
                                    mode: 'match',
                                    skipProtocol: true,
                                    skipHostname: true,
                                    skipPathname: true,
                                },
                            ),
                    );

                    await this.list.waitForLoading();
                    await this.list
                        .getItemsCount()
                        .should.eventually.be.equal(filteredItemsCount, 'Список отфильтрован');
                },
            }),
        },
    },
});
