'use strict';

import {makeCase, makeSuite} from 'ginny';

export default makeSuite('Поиск в саджесте в шапке приложения.', {
    feature: 'Саджест',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При вводе в поиск названия недоступного пользователю вендора': {
            'отображается пустая выдача': makeCase({
                environment: 'testing',
                async test() {
                    this.setPageObjects({
                        search() {
                            return this.createPageObject('Search');
                        },
                        searchInput() {
                            return this.createPageObject('InputB2b');
                        },
                        searchSpinner() {
                            return this.createPageObject('SpinnerLevitan', this.searchInput);
                        },
                        searchPopup() {
                            return this.createPageObject('PopupB2b');
                        },
                    });

                    await this.search.isVisible().should.eventually.be.equal(true, 'Саджест отображается на странице');

                    await this.searchInput.setValue('НЕрезиденты');

                    await this.allure.runStep('Ожидаем завершения поиска', () =>
                        this.browser.waitUntil(
                            async () => {
                                const isVisible = await this.searchSpinner.isVisible();

                                return isVisible === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Спиннер не скрылся',
                        ),
                    );

                    await this.searchPopup.waitForPopupShown();

                    await this.search
                        .getVendorsItemsCount()
                        .should.eventually.be.equal(0, 'Отображается пустая выдача');

                    await this.searchPopup
                        .getActiveText()
                        .should.eventually.be.equal(
                            'По вашему запросу ничего не нашлось',
                            'Текст сообщения корректный',
                        );
                },
            }),
        },
    },
});
