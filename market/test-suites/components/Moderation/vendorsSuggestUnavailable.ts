'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ModerationList} list – список заявок
 */
export default makeSuite('Редактирование заявки.', {
    feature: 'Модерация',
    environment: 'testing',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При просмотре страницы модерации': {
            'саджест с вендорами недоступен': makeCase({
                issue: 'VNDFRONT-3869',
                id: 'vendor_auto-649',
                async test() {
                    this.setPageObjects({
                        layout() {
                            return this.createPageObject('LayoutBase');
                        },
                        vendorsSearch() {
                            return this.createPageObject('Search');
                        },
                    });

                    await this.browser.allure.runStep('Ожидаем появления заголовка страницы', () =>
                        this.browser.waitUntil(
                            () => this.layout.header.vndIsExisting(),
                            this.browser.options.waitforTimeout,
                            'Заголовок страницы не появился',
                        ),
                    );

                    await this.vendorsSearch
                        .isVisible()
                        .should.eventually.be.equal(false, 'Саджест с поиском по вендорам не отображается');
                },
            }),
        },
    },
});
