'use strict';

import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * Блок подписки по электронной почте.
 * @param {PageObject.Subscribe} subscribe
 * @param {PageObject.MultiTextInput} multiTextInput - поле добавления почт
 * @param {PageObject.PopupB2b} popup - тултип
 * @param {PageObject.Tags} tags - список тегов
 * @param {PageObject.Tag} tag - тег
 * @param {PageObject.Link} expander - кнопка "Ещё"
 */
export default makeSuite('Добавление почты.', {
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления блока подписки по электронной почте.', () =>
                    this.subscribe.waitForExist(),
                );
            },
        },
        {
            'При вводе невалидного адреса': {
                'появлется подсказка о неправильном формате': makeCase({
                    id: 'vendor_auto-757',
                    issue: 'VNDFRONT-2333',
                    async test() {
                        await this.multiTextInput.setValue('spbtester');

                        await this.browser.waitUntil(
                            () => this.popup.activeBodyPopup.isVisible(),
                            this.browser.options.waitforTimeout,
                            'Тултип с ошибкой появился',
                        );

                        await this.popup
                            .getActiveText()
                            .should.eventually.be.equal('Неверный формат эл. почты', 'Текст тултипа корректный');

                        await this.multiTextInput.button
                            .isEnabled()
                            .should.eventually.be.equal(false, 'Кнопка добавления значения заблокирована');
                    },
                }),
            },
            'При вводе более 255 символов': {
                'появлется подсказка с ошибкой': makeCase({
                    id: 'vendor_auto-757',
                    issue: 'VNDFRONT-2333',
                    async test() {
                        await this.multiTextInput.setValue(`${'a'.repeat(127)}@${'b'.repeat(128)}`);

                        await this.browser.waitUntil(
                            () => this.popup.activeBodyPopup.isVisible(),
                            this.browser.options.waitforTimeout,
                            'Тултип с ошибкой появился',
                        );

                        await this.popup
                            .getActiveText()
                            .should.eventually.be.equal(
                                'Должно содержать не более 255 символов',
                                'Текст тултипа корректный',
                            );

                        await this.multiTextInput.button
                            .isEnabled()
                            .should.eventually.be.equal(false, 'Кнопка добавления значения заблокирована');
                    },
                }),
            },
            'При успешном добавлении почты': {
                'в списке появляется тег': makeCase({
                    id: 'vendor_auto-758',
                    issue: 'VNDFRONT-2347',
                    environment: 'kadavr',
                    async test() {
                        const email = 'почта@домен.рф';

                        await this.multiTextInput.setValue(email);

                        await this.browser.allure.runStep('Кликаем по кнопке добавления почты', () =>
                            this.multiTextInput.button.click(),
                        );

                        await this.browser.allure.runStep('Ожидаем появления тега в списке', () =>
                            this.tag.waitForExist(),
                        );

                        await this.tag.root
                            .getText()
                            .should.eventually.be.equal(email, 'Тег с указанной почтой появился');
                    },
                }),
            },
            'Более 5 подписчиков': {
                'скрываются под катом': makeCase({
                    id: 'vendor_auto-759',
                    issue: 'VNDFRONT-2380',
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
            'При успешном удалении почты': {
                'тег пропадает из списка': makeCase({
                    id: 'vendor_auto-760',
                    issue: 'VNDFRONT-2381',
                    environment: 'kadavr',
                    async test() {
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
                    },
                }),
            },
            'При повторном добавлении почты': {
                'отображается тост с ошибкой': makeCase({
                    id: 'vendor_auto-764',
                    issue: 'VNDFRONT-3404',
                    environment: 'kadavr',
                    async test() {
                        this.setPageObjects({
                            message() {
                                return this.createPageObject('Messages');
                            },
                            messageCloseButton() {
                                return this.createPageObject('IconLevitan', this.message);
                            },
                        });

                        const email = 'почта@домен.рф';

                        await this.multiTextInput.setValue(email);

                        await this.browser.allure.runStep('Кликаем по кнопке добавления почты', () =>
                            this.multiTextInput.button.click(),
                        );

                        await this.browser.allure.runStep('Ожидаем появления тега в списке', () =>
                            this.tag.waitForExist(),
                        );

                        await this.tag.root
                            .getText()
                            .should.eventually.be.equal(email, 'Тег с указанной почтой появился');

                        await this.browser.allure.runStep('Закрываем сообщение об успешном добавлении почты', () =>
                            this.messageCloseButton.click(),
                        );

                        await this.multiTextInput.setValue(email);

                        await this.browser.allure.runStep('Добавляем уже добавленную почту ещё раз', () =>
                            this.multiTextInput.button.click(),
                        );

                        await this.allure.runStep('Ожидаем появления сообщения с ошибкой', () =>
                            this.message.waitForVisible(),
                        );

                        await this.message
                            .getMessageText()
                            .should.eventually.be.equal(
                                'Ошибка добавления почты',
                                'В сообщении отображается корректный текст',
                            );
                    },
                }),
            },
        },
    ),
});
