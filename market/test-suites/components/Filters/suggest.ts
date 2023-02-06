'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на фильтрацию элементов списка саджестом.
 * @param {PageObject.Suggest} suggest - саджест
 * @param {PageObject.PopupB2b} popup - попап для саджеста
 * @param {PageObject.ListContainer} list - список элементов
 * @param {Object} params
 * @param {number} params.searchText - искомая подстрока
 * @param {number} params.initialCount - изначальное количество элементов в списке
 * @param {number} params.expectedCount - ожидаемое количество элементов в списке после фильтрации
 * @param {string} params.queryParamName - имя query-параметра фильтра
 * @param {string|number} params.queryParamValue - значение query-параметра фильтра
 * @param {number} [params.popupItemsCount] - количество позиций в выпадающем окне после ввода искомой подстроки
 * @param {number} [params.popupSelectionIndex] - индекс позиции выбираемого значения
 */
export default makeSuite('Саджест.', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При выборе найденного значения': {
            'фильтрует элементы списка': makeCase({
                async test() {
                    const {
                        searchText,
                        initialCount,
                        expectedCount,
                        queryParamName,
                        queryParamValue,
                        popupItemsCount = 1,
                        popupSelectionIndex = 0,
                    } = this.params;

                    await this.browser.allure.runStep('Ожидаем появления саджеста', () => this.suggest.waitForExist());

                    await this.list
                        .getItemsCount()
                        .should.eventually.be.equal(initialCount, `Отображается элементов: "${initialCount}"`);

                    await this.suggest.setFocus();

                    await this.suggest.setText(searchText);

                    await this.popup.waitForPopupShown();

                    await this.suggest.waitForPopupItemsCount(popupItemsCount);

                    await this.browser
                        .vndWaitForChangeUrl(() => this.suggest.selectItem(popupSelectionIndex))
                        .should.eventually.be.link(
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
                        );

                    await this.list.waitForLoading();

                    await this.list
                        .getItemsCount()
                        .should.eventually.be.equal(expectedCount, `Отображается элементов: "${expectedCount}"`);
                },
            }),
        },
    },
});
