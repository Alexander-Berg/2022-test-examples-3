'use strict';

import {PageObject, makeSuite, makeCase} from 'ginny';

const COMMENT_TEXT = 'Я маленькая лошадка';

const LONG_COMMENT_TEXT = new Array(513).fill('a').join('');

const BrandEditRequest = PageObject.get('BrandEditRequest');

/**
 * @param {PageObject.ModerationList} moderationList – список заявок
 * @param {PageObject.BrandEditRequest} item – элемент списка заявок
 * @param {PageObject.CheckboxB2b} checkbox - чекбокс у поля бренда
 * @param {PageObject.ButtonLevitan} saveButton - кнопка сохранения данных заявки
 * @param {PageObject.ButtonLevitan} closeButton - кнопка закрытия заявки
 * @param {PageObject.ButtonLevitan} revertButton - кнопка переоткрытия заявки
 */
export default makeSuite('Редактирование заявки.', {
    feature: 'Модерация',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'Решения и комментарий по заявке': {
            'при нажатии на кнопку "Сохранить"': {
                сохраняются: makeCase({
                    issue: 'VNDFRONT-2813',
                    id: 'vendor_auto-289',
                    async test() {
                        // Открыть заявку
                        await this.moderationList.clickItemByIndex();

                        // Считаем, что заявка открылась, когда появилась кнопка Сохранить
                        await this.browser.allure.runStep('Дожидаемся открытия заявки', () =>
                            this.saveButton.waitForExist(),
                        );

                        await this.browser.allure.runStep(
                            'Проверяем, что заявка открылась и отображается корректно',
                            async () => {
                                /**
                                 * Проверить наличие и состояние кнопок сохранить и закрыть
                                 * Используем yaSafeAction, чтобы не делать дополнительные проверки с isExisting.
                                 * Если элемент не будет найден, тест упадет,
                                 * но ошибка будет корректно обработана как false.
                                 * Если использовать waitUntil, то тест в отчете будет broken,а не failed
                                 */
                                await this.browser
                                    .yaSafeAction(this.saveButton.isDisabled(), false)
                                    .should.eventually.be.equal(false, 'Кнопка [Сохранить] отображается и активна');

                                await this.browser
                                    .yaSafeAction(
                                        this.closeButton.isDisabled(),
                                        /**
                                         * undefined т.к. здесь проверяем кнопку на disabled=true,
                                         * и просто false по умолчанию недостаточно
                                         * иначе при поломке кнопки тест будет ложно-положительный
                                         */
                                        undefined,
                                    )
                                    .should.eventually.be.deep.equal(true, 'Кнопка [Закрыть заявку] задизейблена');
                            },
                        );

                        // Кликаем в чекбокс у поля бренда
                        await this.checkbox.click();

                        await this.browser.allure.runStep('Нажимаем на кнопку "Сохранить"', () =>
                            this.saveButton.click(),
                        );

                        await this.item.waitForSaving();

                        await this.item.isSaveMessageExisting();

                        await this.checkbox.checked
                            .isExisting()
                            .should.eventually.be.equal(true, 'Чекбокс остался в нажатом состоянии');

                        await this.item.setMainCommentValue(COMMENT_TEXT);

                        await this.browser.allure.runStep('Нажимаем на кнопку "Сохранить"', () =>
                            this.saveButton.click(),
                        );

                        await this.item.waitForSaving();

                        await this.item.isSaveMessageExisting();

                        await this.browser.allure.runStep('Проверяем наличие комментария', () =>
                            this.item.mainComment
                                .getText()
                                .should.eventually.be.equal(COMMENT_TEXT, 'Комментарий сохранился'),
                        );
                    },
                }),
            },
        },
        'При вводе в комментарий более 512 символов': {
            'отображается ошибка валидации': makeCase({
                issue: 'VNDFRONT-3869',
                id: 'vendor_auto-771',
                async test() {
                    this.setPageObjects({
                        errorPopup() {
                            return this.createPageObject('PopupB2b');
                        },
                    });

                    await this.moderationList.clickItemByIndex();

                    await this.browser.allure.runStep('Дожидаемся открытия заявки', () =>
                        this.saveButton.waitForExist(),
                    );

                    await this.browser.allure.runStep(
                        'Проверяем, что заявка открылась и отображается корректно',
                        async () => {
                            await this.browser
                                .yaSafeAction(this.saveButton.isDisabled(), true)
                                .should.eventually.be.equal(false, 'Кнопка [Сохранить] отображается и активна');

                            await this.browser
                                .yaSafeAction(this.closeButton.isDisabled(), undefined)
                                .should.eventually.be.deep.equal(true, 'Кнопка [Закрыть заявку] задизейблена');
                        },
                    );

                    await this.browser.allure.runStep(`Вводим в основной комментарий текст ${LONG_COMMENT_TEXT}`, () =>
                        this.item.mainComment.vndSetValue(LONG_COMMENT_TEXT),
                    );

                    await this.browser.allure.runStep('Дожидаемся появления попапа с ошибкой', () =>
                        this.errorPopup.waitForPopupShown(),
                    );

                    await this.errorPopup
                        .getActiveText()
                        .should.eventually.be.equal(
                            'Должно содержать не более 512 символов',
                            'Текст ошибки корректный',
                        );

                    await this.browser.allure.runStep('Очищаем поле основного комментария', () =>
                        this.item.mainComment.vndSetValue(''),
                    );

                    await this.browser.allure.runStep('Дожидаемся скрытия попапа с ошибкой', () =>
                        this.errorPopup.waitForPopupHidden(),
                    );

                    await this.browser.allure.runStep(`Вводим в комментарий к полю текст ${LONG_COMMENT_TEXT}`, () =>
                        this.item.comment.vndSetValue(LONG_COMMENT_TEXT),
                    );

                    await this.browser.allure.runStep('Дожидаемся появления попапа с ошибкой', () =>
                        this.errorPopup.waitForPopupShown(),
                    );

                    await this.errorPopup
                        .getActiveText()
                        .should.eventually.be.equal(
                            'Должно содержать не более 512 символов',
                            'Текст ошибки корректный',
                        );
                },
            }),
        },
        'Для заявки без старых значений': {
            'нельзя подать корректирующую заявку': makeCase({
                issue: 'VNDFRONT-3869',
                id: 'vendor_auto-292',
                async test() {
                    await this.moderationList.clickItemByIndex();

                    await this.browser.allure.runStep('Дожидаемся открытия заявки', () =>
                        this.revertButton.waitForExist(),
                    );

                    await this.browser
                        .yaSafeAction(this.revertButton.isDisabled(), undefined)
                        .should.eventually.be.deep.equal(true, 'Кнопка [Переоткрыть заявку] задизейблена');
                },
            }),
        },
        'При закрытии заявки': {
            'статус заявки': {
                обновляется: makeCase({
                    issue: 'VNDFRONT-3869',
                    id: 'vendor_auto-290',
                    async test() {
                        await this.moderationList.clickItemByIndex();

                        await this.browser.allure.runStep('Дожидаемся открытия заявки', () =>
                            this.saveButton.waitForExist(),
                        );

                        await this.item.status
                            .getText()
                            .should.eventually.be.equal('активна', 'Статус заявки корректный');

                        await this.browser.allure.runStep(
                            'Проверяем, что заявка открылась и отображается корректно',
                            async () => {
                                await this.browser
                                    .yaSafeAction(this.saveButton.isDisabled(), true)
                                    .should.eventually.be.equal(false, 'Кнопка [Сохранить] отображается и активна');

                                await this.browser
                                    .yaSafeAction(this.closeButton.isDisabled(), undefined)
                                    .should.eventually.be.deep.equal(true, 'Кнопка [Закрыть заявку] задизейблена');
                            },
                        );

                        await this.checkbox.click();

                        await this.browser.allure.runStep(`Вводим в комментарий к полю текст ${COMMENT_TEXT}`, () =>
                            this.item.comment.vndSetValue(COMMENT_TEXT),
                        );

                        await this.browser.allure.runStep('Нажимаем на кнопку "Закрыть заявку"', () =>
                            this.closeButton.click(),
                        );

                        await this.item.waitForSaving();

                        await this.item.isSaveMessageExisting();

                        await this.browser
                            .yaSafeAction(this.revertButton.isDisabled(), false)
                            .should.eventually.be.deep.equal(
                                false,
                                'Кнопка [Переоткрыть заявку] отображается и активна',
                            );

                        await this.item.status
                            .getText()
                            .should.eventually.be.equal('закрыта', 'Статус заявки корректный');
                    },
                }),
            },
        },
        'При переоткрытии заявки': {
            'корректирующая заявка': {
                добавляется: makeCase({
                    issue: 'VNDFRONT-3869',
                    id: 'vendor_auto-291',
                    async test() {
                        this.setPageObjects({
                            sourceRequestLink() {
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                return this.createPageObject('Link', BrandEditRequest.sourceRequest);
                            },
                            closedItem() {
                                return this.createPageObject(
                                    'BrandEditRequest',
                                    this.list,
                                    this.list.getItemByIndex(1),
                                );
                            },
                            closedItemRevertButton() {
                                return this.createPageObject(
                                    'ButtonLevitan',
                                    this.closedItem,
                                    this.closedItem.revertButton,
                                );
                            },
                        });

                        await this.moderationList.clickItemByIndex(1);

                        await this.browser.allure.runStep('Дожидаемся открытия заявки', () =>
                            this.closedItemRevertButton.waitForExist(),
                        );

                        await this.closedItem.status
                            .getText()
                            .should.eventually.be.equal('закрыта', 'Статус заявки корректный');

                        await this.closedItem.companyName
                            .getText()
                            .should.eventually.be.equal('KADAVR AC Robin ✓ #14949666', 'Название заявки корректное');

                        await this.browser
                            .yaSafeAction(this.closedItemRevertButton.isDisabled(), true)
                            .should.eventually.be.equal(false, 'Кнопка [Переоткрыть заявку] отображается и активна');

                        await this.browser.allure.runStep('Нажимаем на кнопку "Переоткрыть заявку"', () =>
                            this.closedItemRevertButton.click(),
                        );

                        await this.browser.allure.runStep('Получаем текст подтвержения', () =>
                            this.browser
                                .alertText()
                                .should.eventually.be.equal(
                                    'Вы действительно хотите переоткрыть заявку?',
                                    'Текст подтверждения корректный',
                                ),
                        );

                        await this.browser.allure.runStep('Подтверждаем переоткрытие заявки', () =>
                            this.browser.alertAccept(),
                        );

                        await this.allure.runStep('Ожидаем появления списка заявок', () => this.list.waitForExist());

                        await this.allure.runStep('Ожидаем загрузки списка', () => this.list.waitForLoading());

                        await this.item.status
                            .getText()
                            .should.eventually.be.equal('активна', 'Статус заявки корректный');

                        await this.item.companyName
                            .getText()
                            .should.eventually.be.equal('KADAVR AC Robin ✓ #14949666', 'Название заявки корректное');

                        await this.moderationList.clickItemByIndex();

                        await this.browser.allure.runStep(
                            'Дожидаемся открытия заявки (появления ссылки на корректирующую заявку)',
                            () => this.sourceRequestLink.waitForExist(),
                        );

                        await this.sourceRequestLink
                            .getText()
                            .should.eventually.be.equal('#48504', 'Текст ссылки корректный');
                    },
                }),
            },
        },
    },
});
