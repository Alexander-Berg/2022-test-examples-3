'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на поиск сущностей по названию в селекте в модальном окне
 * @param {PageObject.ModalMultiSelect} select Селект сущностей в модальном окне
 * @param {PageObject.Modal} modal Модальное окно
 * @param {PageObject.TextLevitan|PageObject.TextB2b} [notFoundElement] Элемент пустой выдачи списка
 * @param {Object} params
 * @param {number} params.initialItemsCount Начальное количество элементов
 * @param {number} params.expectedItemsCount Отфильтрованное количество элементов
 * @param {string} params.searchText Поисковый запрос
 * @param {string} [params.notFoundText] Текст сообщения в пустой выдаче результатов поиска
 */
export default makeSuite('Поиск сущностей.', {
    params: {
        user: 'Пользователь',
    },
    story: {
        'При вводе текста': {
            'фильтруется список сущностей': makeCase({
                async test() {
                    this.setPageObjects({
                        input() {
                            return this.createPageObject('InputB2b', this.modal.header);
                        },
                    });

                    const {expectedItemsCount, initialItemsCount, notFoundText, searchText} = this.params;

                    await this.select.click();

                    await this.browser.allure.runStep('Ожидаем появления модального окна', () =>
                        this.modal.waitForVisible(),
                    );

                    await this.modal.waitForLoading();

                    await this.modal
                        .getListItemsCount()
                        .should.eventually.be.equal(initialItemsCount, `Отображается ${initialItemsCount} элементов`);

                    await this.input.setValue(searchText);

                    await this.modal.waitForLoading();

                    await this.modal
                        .getListItemsCount()
                        .should.eventually.be.equal(expectedItemsCount, `Отображается ${expectedItemsCount} элементов`);

                    if (notFoundText) {
                        await this.notFoundElement
                            .getText()
                            .should.eventually.be.equal(notFoundText, 'Текст сообщения корректный');
                    }
                },
            }),
        },
    },
});
