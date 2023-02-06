'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ListContainer} list - список элементов
 * @param {PageObject.ResetFilters} resetFilters - кнопка "Сбросить фильтры"
 * @param {Object} params
 * @param {boolean} params.user - пользователь
 * @param {number} params.initialItemsCount - количесто без фильтрации
 * @param {number} params.filteredItemsCount - количество элементов после фильтрации
 * @param {string} params.setFilters - функция, которая установит фильтры
 */
export default makeSuite('Кнопка "Сбросить фильтры"', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'при клике': {
            'сбрасывает фильтры': makeCase({
                async test() {
                    const {browser, allure, params} = this;

                    // устанавливаем фильтры
                    await browser.vndWaitForChangeUrl(params.setFilters.bind(this), true);

                    await allure.runStep('Ожидаем применение фильтров и появления кнопки', async () => {
                        await this.resetFilters.waitForExist();
                        await this.list.waitForLoading();
                    });

                    await allure
                        .runStep('Сверяем количество после фильтрации', () => this.list.getItemsCount())
                        .should.eventually.equal(params.filteredItemsCount, 'Список отфильтровался');

                    // кликаем по кнопке
                    await this.resetFilters.click();

                    await allure.runStep('Ожидаем обновление списка', () => this.list.waitForLoading());

                    await allure
                        .runStep('Сверяем количество без фильтров', () => this.list.getItemsCount())
                        .should.eventually.be.equal(params.initialItemsCount, 'Фильтры сбросились');

                    // кнопка скрылась
                    await this.resetFilters.isExisting().should.eventually.equal(false, 'Кнопка скрылась');
                },
            }),
        },
    },
});
