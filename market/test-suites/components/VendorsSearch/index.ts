'use strict';

import {makeCase, makeSuite} from 'ginny';

/**
 * @param {Object} params
 * @param {string} params.expectedCount - количество вендоров в выпадашке саджеста
 * @param {string} params.checkPlaceholder - признак, нужно ли проверить плейсхолдер саджеста
 * @param {string} params.withElementClick - признак, нужно ли кликнуть по вендору в выпадашке саджеста
 */
export default makeSuite('Саджест в шапке приложения.', {
    feature: 'Саджест',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При клике на инпут саджеста': {
            'открывается список доступных для выбора брендов': makeCase({
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
                    });

                    const {expectedCount, checkPlaceholder, withElementClick} = this.params;

                    await this.search.isVisible().should.eventually.be.equal(true, 'Саджест отображается на странице');

                    if (checkPlaceholder) {
                        await this.searchInput.placeholder.should.eventually.be.equal(
                            'Поиск',
                            'Плейсхолдер в саджесте корректный',
                        );
                    }

                    await this.searchInput.setFocus();

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

                    await this.search
                        .getVendorsItemsCount()
                        .should.eventually.be.equal(expectedCount, 'Отображается корректное количество вендоров');

                    if (withElementClick) {
                        await this.allure.runStep('Кликаем по первому найденному вендору из списка', () =>
                            this.browser
                                .vndWaitForChangeUrl(() => this.search.getVendorsItemByIndex(0).click())
                                .should.eventually.be.link('/vendors/2288/products', {
                                    mode: 'match',
                                    skipProtocol: true,
                                    skipHostname: true,
                                    skipQuery: true,
                                }),
                        );
                    }
                },
            }),
        },
    },
});
