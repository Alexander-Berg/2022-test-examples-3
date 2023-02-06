'use strict';

import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * Блок подписки по логину на Яндексе.
 * @param {PageObject.Subscribe} subscribe
 * @param {PageObject.Suggest} suggest - поле добавления логинов
 * @param {PageObject.PopupB2b} popup - попап для саджеста
 * @param {PageObject.Tags} tags - список тегов
 * @param {PageObject.Tag} tag - тег
 * @param {PageObject.Link} expander - кнопка "Ещё"
 */
export default makeSuite('Добавление логина.', {
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления блока подписки по логину.', () =>
                    this.subscribe.waitForExist(),
                );
            },
        },
        {
            'При выборе логина из выпадающего меню': {
                'в списке появляется тег': makeCase({
                    id: 'vendor_auto-761',
                    issue: 'VNDFRONT-2382',
                    environment: 'kadavr',
                    async test() {
                        await this.suggest.setFocus();

                        await this.popup.waitForPopupShown();

                        await this.suggest.setText('bari');

                        await this.browser.allure.runStep('Ожидаем сортировки списка по введенному тексту', () =>
                            this.suggest.waitForPopupItemsCount(1),
                        );

                        await this.suggest.selectItem(0);

                        await this.browser.allure.runStep('Ожидаем появления тега в списке', () =>
                            this.tag.waitForExist(),
                        );

                        await this.tag.root
                            .getText()
                            .should.eventually.be.equal('bari-badamshin', 'Тег с выбранным логином появился');
                    },
                }),
            },
            'Более 5 логинов': {
                'скрываются под катом': makeCase({
                    id: 'vendor_auto-762',
                    issue: 'VNDFRONT-2384',
                    environment: 'kadavr',
                    async test() {
                        await this.tags.getItemsCount().should.eventually.be.equal(5, 'Отображается 5 тегов');

                        await this.expander
                            .isVisible()
                            .should.eventually.be.equal(true, 'Ссылка "Ещё n.." отображается');

                        await this.expander.getText().should.eventually.be.equal('Ещё 2', 'Текст ссылки корректный');

                        await this.browser.allure.runStep('Кликаем по ссылке "Ещё n.."', () =>
                            this.expander.root.click(),
                        );

                        await this.tags.getItemsCount().should.eventually.be.equal(7, 'Отображается 7 тегов');

                        await this.expander
                            .getText()
                            .should.eventually.be.equal('Свернуть', 'Текст ссылки изменился на "Свернуть"');

                        await this.browser.allure.runStep('Кликаем по ссылке "Свернуть"', () =>
                            this.expander.root.click(),
                        );

                        await this.tags.getItemsCount().should.eventually.be.equal(5, 'Отображается 5 тегов');

                        await this.expander
                            .getText()
                            .should.eventually.be.equal('Ещё 2', 'Текст ссылки изменился на исходный');
                    },
                }),
            },
            'При успешном удалении логина': {
                'тег пропадает из списка': makeCase({
                    id: 'vendor_auto-763',
                    issue: 'VNDFRONT-2385',
                    environment: 'kadavr',
                    async test() {
                        await this.suggest.setFocus();

                        await this.browser.allure.runStep('Выбранный логин отсутствует в выпадающем меню', () =>
                            this.suggest.waitForPopupItemsCount(0),
                        );

                        await this.tag.isVisible().should.eventually.be.equal(true, 'Тег отображается');

                        await this.tag.remove();

                        await this.browser.waitUntil(
                            async () => {
                                const existing = await this.tag.isExisting();

                                return existing === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Тег скрылся',
                        );

                        await this.suggest.setFocus();

                        await this.suggest.setText('test');

                        await this.browser.allure.runStep('Удаленный логин отображается в выпадающем меню', () =>
                            this.suggest.waitForPopupItemsCount(1),
                        );
                    },
                }),
            },
        },
    ),
});
