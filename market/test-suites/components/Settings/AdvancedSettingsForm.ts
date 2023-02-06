'use strict';

import {makeSuite, makeCase, mergeSuites} from 'ginny';

/**
 * Тесты для формы расширенных настроек
 * @param {PageObject.FinalForm} form - форма расширенных настроек
 * @param {PageObject.Tags} tags - список тегов с текущими категориями вендора
 * @param {PageObject.Tag} tag - тег с категорией
 * @param {PageObject.Suggest} suggest - саджест со списком брендов
 * @param {PageObject.Suggest} suggestWithTags - саджест со списком категорий
 * @param {PageObject.Messages} message - сообщение-"кирпич"
 * @param {PageObject.PopupB2b} popup - попап для саджеста
 */
export default makeSuite('Форма редактирования расширенных настроек.', {
    feature: 'Настройки',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    submitButton() {
                        return this.createPageObject('ButtonLevitan', this.form);
                    },
                });

                return this.allure.runStep('Ожидаем появления формы расширенных настроек', async () => {
                    await this.form.waitForExist();

                    /**
                     * Необходимо прокрутить окно вниз, так как некоторые поля формы
                     * могут оказаться вне области видимости
                     */
                    await this.browser.vndScrollToBottom();
                });
            },
        },
        {
            'При сохранении пустого списка категорий': {
                'отображается хинт о необходимости выбора категории': makeCase({
                    issue: 'VNDFRONT-3116',
                    id: 'vendor_auto-939',

                    async test() {
                        await this.tags
                            .isExisting()
                            .should.eventually.be.equal(true, 'Список категорий вендора отображается');

                        await this.tags
                            .getItemsCount()
                            .should.eventually.be.equal(1, 'Отображается 1 тег с категорией');

                        await this.tag.remove();

                        await this.browser.allure.runStep('Ожидаем скрытия тега с категорией', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const existing = await this.tag.isExisting();

                                    return existing === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Тег отображается',
                            ),
                        );

                        await this.form
                            .getFieldErrorMessageByLabelText('Категории')
                            .should.eventually.be.equal(
                                'Выберите категорию',
                                'Сообщение об ошибке у поля "Категории" отображается корректно',
                            );

                        await this.browser
                            .yaSafeAction(this.submitButton.isDisabled(), true)
                            .should.eventually.equal(false, 'Кнопка [Сохранить] отображается и активна');
                    },
                }),
            },
        },
        {
            'При удалении категории': {
                'тег скрывается успешно': makeCase({
                    issue: 'VNDFRONT-3116',
                    id: 'vendor_auto-938',

                    async test() {
                        await this.tags
                            .isExisting()
                            .should.eventually.be.equal(true, 'Список категорий вендора отображается');

                        await this.tags
                            .getItemsValues()
                            .should.eventually.have.same.members(
                                ['Все товары', 'Все товары / Авто'],
                                'В списке категорий отображаются корректные значения',
                            );

                        await this.tag.remove();

                        await this.tags
                            .getItemsValues()
                            .should.eventually.have.same.members(
                                ['Все товары / Авто'],
                                'В списке категорий отображаются корректные значения',
                            );

                        await this.submitButton.click();

                        await this.allure.runStep('Ожидаем появления сообщения об успешном сохранении', () =>
                            this.message.waitForExist(),
                        );

                        // Считаем, что изменения применились, когда кнопка Сохранить станет активной
                        await this.browser.allure.runStep('Дожидаемся сохранения нового названия', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const disabled = await this.browser.yaSafeAction(
                                        this.submitButton.isDisabled(),
                                        true,
                                    );

                                    // @ts-expect-error(TS2367) найдено в рамках VNDFRONT-4580
                                    return disabled === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Кнопка "Сохранить" неактивна',
                            ),
                        );

                        await this.message
                            .getMessageText()
                            .should.eventually.be.equal(
                                'Карточка вендора успешно обновлена',
                                'В сообщении отображается корректный текст',
                            );

                        await this.tags
                            .getItemsValues()
                            .should.eventually.have.same.members(
                                ['Все товары / Авто'],
                                'В списке категорий отображаются корректные значения',
                            );
                    },
                }),
            },
        },
        {
            'При редактировании расширенных настроек и обновлении страницы': {
                'значения сбрасываются': makeCase({
                    issue: 'VNDFRONT-3116',
                    id: 'vendor_auto-941',

                    async test() {
                        const NAME = 'Название карточки вендора 3301';
                        const BRAND = 'Cisco (722706)';
                        const CATEGORIES = ['Все товары', 'Все товары / Авто'];

                        await this.browser.allure.runStep('Проверяем изначальные значения полей формы', async () => {
                            await this.form
                                .getInputValue('Заголовок карточки')
                                .should.eventually.be.equal(NAME, 'У поля "Заголовок карточки" корректное значение');

                            await this.form
                                .getInputValue('Бренд')
                                .should.eventually.be.equal(BRAND, 'У поля "Бренд" корректное значение');

                            await this.tags
                                .getItemsValues()
                                .should.eventually.have.same.members(
                                    CATEGORIES,
                                    'В списке категорий отображаются корректные значения',
                                );
                        });

                        await this.browser.allure.runStep('Редактируем поля в форме', async () => {
                            await this.form.setInputValue('Заголовок карточки', 'Test');

                            await this.browser.allure.runStep('Изменяем значение поля бренд на "Samsung"', async () => {
                                await this.suggest.setText('Samsung');

                                await this.suggest.setFocus();

                                await this.popup.waitForPopupShown();

                                await this.suggest.selectItem(0);
                            });

                            await this.browser.allure.runStep('Удаляем одну категорию', () => this.tag.remove());
                        });

                        await this.browser.allure.runStep('Обновляем страницу', () => this.browser.refresh());

                        await this.allure.runStep('Ожидаем появления формы расширенных настроек', async () => {
                            await this.form.waitForExist();

                            await this.browser.vndScrollToBottom();
                        });

                        await this.browser.allure.runStep('Значения в полях формы сбросились', async () => {
                            await this.form
                                .getInputValue('Заголовок карточки')
                                .should.eventually.be.equal(
                                    NAME,
                                    'У поля "Заголовок карточки" корректное значение после обновления страницы',
                                );

                            await this.form
                                .getInputValue('Бренд')
                                .should.eventually.be.equal(
                                    BRAND,
                                    'У поля "Бренд" корректное значение после обновления страницы',
                                );

                            await this.tags
                                .getItemsValues()
                                .should.eventually.have.same.members(
                                    CATEGORIES,
                                    'В списке категорий отображаются корректные значения после обновления страницы',
                                );
                        });
                    },
                }),
            },
        },
        {
            'При редактировании заголовка карточки': {
                'новое значение сохраняется': makeCase({
                    issue: 'VNDFRONT-3174',
                    id: 'vendor_auto-933',

                    async test() {
                        await this.form
                            .getInputValue('Заголовок карточки')
                            .should.eventually.be.equal(
                                'Название карточки вендора 3301',
                                'У поля "Заголовок карточки" корректное изначальное значение',
                            );

                        await this.browser.allure.runStep('Редактируем название карточки', () =>
                            this.form.setInputValue('Заголовок карточки', 'Новое название карточки вендора 666'),
                        );

                        await this.submitButton.click();

                        await this.allure.runStep('Ожидаем появления сообщения об успешном сохранении', () =>
                            this.message.waitForExist(),
                        );

                        // Считаем, что изменения применились, когда кнопка Сохранить станет активной
                        await this.browser.allure.runStep('Дожидаемся сохранения нового названия', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const disabled = await this.browser.yaSafeAction(
                                        this.submitButton.isDisabled(),
                                        true,
                                    );

                                    // @ts-expect-error(TS2367) найдено в рамках VNDFRONT-4580
                                    return disabled === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Кнопка "Сохранить" неактивна',
                            ),
                        );

                        await this.form
                            .getInputValue('Заголовок карточки')
                            .should.eventually.be.equal(
                                'Новое название карточки вендора 666',
                                'У поля "Заголовок карточки" корректное сохранённое значение',
                            );
                    },
                }),
            },
        },
        {
            'При вводе пустого или неуникального заголовка карточки': {
                'отображаются хинты об обязательности и уникальности значения': makeCase({
                    issue: 'VNDFRONT-3174',
                    id: 'vendor_auto-934',

                    async test() {
                        const NAME = 'Название карточки вендора 3301';

                        await this.form
                            .getInputValue('Заголовок карточки')
                            .should.eventually.be.equal(
                                NAME,
                                'У поля "Заголовок карточки" корректное изначальное значение',
                            );

                        await this.browser.allure.runStep('Очищаем поле "Заголовок карточки"', async () => {
                            await this.form.setInputValue('Заголовок карточки', '');
                        });

                        await this.form
                            .getFieldErrorMessageByLabelText('Заголовок карточки')
                            .should.eventually.be.equal(
                                'Поле обязательно для заполнения',
                                'Сообщение об ошибке у поля "Заголовок карточки" отображается корректно',
                            );

                        await this.browser.allure.runStep('Вводим в поле название уже существующей карточки', () =>
                            this.form.setInputValue('Заголовок карточки', 'Название карточки вендора 3302'),
                        );

                        await this.submitButton.click();

                        // Считаем, что изменения применились, когда кнопка Сохранить станет активной,
                        // так как ожидаем, что ручка вернёт ошибку
                        await this.browser.allure.runStep('Дожидаемся сохранения нового названия', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const disabled = await this.browser.yaSafeAction(
                                        this.submitButton.isDisabled(),
                                        true,
                                    );

                                    // @ts-expect-error(TS2367) найдено в рамках VNDFRONT-4580
                                    return disabled === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Кнопка "Сохранить" неактивна',
                            ),
                        );

                        await this.form
                            .getFieldErrorMessageByLabelText('Заголовок карточки')
                            .should.eventually.be.equal(
                                'Заголовок карточки должен быть уникальным',
                                'Сообщение об ошибке у поля "Заголовок карточки" отображается корректно',
                            );

                        await this.form.submitError
                            .vndIsExisting()
                            .should.eventually.be.equal(true, 'Сообщение об ошибке у кнопки "Сохранить" отображается');

                        await this.form.submitError
                            .getText()
                            .should.eventually.be.equal('Ошибка заполнения формы', 'Текст ошибки корректный');
                    },
                }),
            },
        },
        {
            'При добавлении категорий': {
                'теги добавляются успешно': makeCase({
                    issue: 'VNDFRONT-3174',
                    id: 'vendor_auto-871',

                    async test() {
                        await this.tags
                            .isExisting()
                            .should.eventually.be.equal(true, 'Список категорий вендора отображается');

                        await this.tags
                            .getItemsValues()
                            .should.eventually.have.same.members(
                                ['Все товары', 'Все товары / Авто'],
                                'В списке категорий отображаются корректные значения',
                            );

                        await this.browser.allure.runStep(
                            'Добавляем листовую категорию "Микроволновые печи"',
                            async () => {
                                await this.suggestWithTags.setText('Микроволновые печи');

                                await this.suggestWithTags.waitForPopupItemsCount(1);

                                await this.suggestWithTags.selectItem(0);
                            },
                        );

                        await this.tags
                            .getItemsValues()
                            .should.eventually.have.same.members(
                                ['Все товары', 'Все товары / Авто', 'Приготовление блюд / Микроволновые печи'],
                                'В списке категорий отображаются корректные значения',
                            );

                        await this.browser.allure.runStep(
                            'Добавляем групповую категорию "Бытовая техника"',
                            async () => {
                                await this.suggestWithTags.setText('Бытовая техника');

                                await this.suggestWithTags.waitForPopupItemsCount(4);

                                await this.suggestWithTags.selectItem(2);
                            },
                        );

                        await this.tags
                            .getItemsValues()
                            .should.eventually.have.same.members(
                                [
                                    'Все товары',
                                    'Все товары / Авто',
                                    'Приготовление блюд / Микроволновые печи',
                                    'Все товары / Бытовая техника',
                                ],
                                'В списке категорий отображаются корректные значения',
                            );

                        await this.submitButton.click();

                        await this.allure.runStep('Ожидаем появления сообщения об успешном сохранении', () =>
                            this.message.waitForExist(),
                        );

                        // Считаем, что изменения применились, когда кнопка Сохранить станет активной
                        await this.browser.allure.runStep('Дожидаемся сохранения нового названия', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const disabled = await this.browser.yaSafeAction(
                                        this.submitButton.isDisabled(),
                                        true,
                                    );

                                    // @ts-expect-error(TS2367) найдено в рамках VNDFRONT-4580
                                    return disabled === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Кнопка "Сохранить" неактивна',
                            ),
                        );

                        await this.message
                            .getMessageText()
                            .should.eventually.be.equal(
                                'Карточка вендора успешно обновлена',
                                'В сообщении отображается корректный текст',
                            );

                        await this.tags
                            .getItemsValues()
                            .should.eventually.have.same.members(
                                [
                                    'Все товары',
                                    'Все товары / Авто',
                                    'Приготовление блюд / Микроволновые печи',
                                    'Все товары / Бытовая техника',
                                ],
                                'В списке категорий отображаются корректные значения',
                            );
                    },
                }),
            },
        },
        {
            'При редактировании бренда': {
                'новое значение сохраняется': makeCase({
                    issue: 'VNDFRONT-3174',
                    id: 'vendor_auto-935',

                    async test() {
                        await this.form
                            .getInputValue('Бренд')
                            .should.eventually.be.equal(
                                'Cisco (722706)',
                                'У поля "Бренд" корректное изначальное значение',
                            );

                        await this.browser.allure.runStep('Изменяем значение поля "Бренд" на "Samsung"', async () => {
                            await this.suggest.setText('Samsung');

                            await this.suggest.setFocus();

                            await this.popup.waitForPopupShown();

                            await this.suggest.selectItem(0);
                        });

                        await this.submitButton.click();

                        await this.allure.runStep('Ожидаем появления сообщения об успешном сохранении', () =>
                            this.message.waitForExist(),
                        );

                        // Считаем, что изменения применились, когда кнопка Сохранить станет активной
                        await this.browser.allure.runStep('Дожидаемся сохранения нового названия', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const disabled = await this.browser.yaSafeAction(
                                        this.submitButton.isDisabled(),
                                        true,
                                    );

                                    // @ts-expect-error(TS2367) найдено в рамках VNDFRONT-4580
                                    return disabled === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Кнопка "Сохранить" неактивна',
                            ),
                        );

                        await this.form
                            .getInputValue('Бренд')
                            .should.eventually.be.equal(
                                'Samsung (153061)',
                                'У поля "Бренд" корректное сохранённое значение',
                            );
                    },
                }),
            },
        },
        {
            'При сохранении пустого бренда': {
                'отображается хинт о необходимости выбора бренда': makeCase({
                    issue: 'VNDFRONT-3174',
                    id: 'vendor_auto-936',

                    async test() {
                        await this.form
                            .getInputValue('Бренд')
                            .should.eventually.be.equal(
                                'Cisco (722706)',
                                'У поля "Бренд" корректное изначальное значение',
                            );

                        await this.browser.allure.runStep('Очищаем поле "Бренд"', async () => {
                            await this.suggest.setText('');
                        });

                        await this.form
                            .getFieldErrorMessageByLabelText('Бренд')
                            .should.eventually.be.equal(
                                'Выберите бренд',
                                'Сообщение об ошибке у поля "Бренд" отображается корректно',
                            );

                        await this.browser
                            .yaSafeAction(this.submitButton.isDisabled(), true)
                            .should.eventually.equal(false, 'Кнопка [Сохранить] отображается и активна');
                    },
                }),
            },
        },
        {
            'При вводе неуникальной комбинации бренда и категорий': {
                'отображается хинт о том, что такая карточка уже существует': makeCase({
                    issue: 'VNDFRONT-3174',
                    id: 'vendor_auto-940',

                    async test() {
                        await this.browser.allure.runStep(
                            'Добавляем вендору категории существующего вендора',
                            async () => {
                                await this.tags
                                    .isExisting()
                                    .should.eventually.be.equal(true, 'Список категорий вендора отображается');

                                await this.tags
                                    .getItemsValues()
                                    .should.eventually.have.same.members(
                                        ['Все товары', 'Все товары / Авто'],
                                        'В списке категорий отображаются корректные изначальные значения',
                                    );

                                await this.browser.allure.runStep(
                                    'Добавляем листовую категорию "Микроволновые печи"',
                                    async () => {
                                        await this.suggestWithTags.setText('Микроволновые печи');

                                        await this.browser.vndScrollToBottom();

                                        await this.suggestWithTags.waitForPopupItemsCount(1);

                                        await this.suggestWithTags.selectItem(0);
                                    },
                                );

                                await this.tags
                                    .getItemsValues()
                                    .should.eventually.have.same.members(
                                        ['Все товары', 'Все товары / Авто', 'Приготовление блюд / Микроволновые печи'],
                                        'В списке категорий отображаются корректные значения',
                                    );
                            },
                        );

                        await this.browser.allure.runStep(
                            'Меняем бренд на бренд того же существующего вендора',
                            async () => {
                                await this.form
                                    .getInputValue('Бренд')
                                    .should.eventually.be.equal(
                                        'Cisco (722706)',
                                        'У поля "Бренд" корректное изначальное значение',
                                    );

                                await this.browser.allure.runStep(
                                    'Изменяем значение поля "Бренд" на "Samsung"',
                                    async () => {
                                        await this.suggest.setText('Samsung');

                                        await this.suggest.setFocus();

                                        await this.popup.waitForPopupShown();

                                        await this.suggest.selectItem(0);
                                    },
                                );

                                await this.form
                                    .getInputValue('Бренд')
                                    .should.eventually.be.equal(
                                        'Samsung (153061)',
                                        'У поля "Бренд" корректное значение',
                                    );
                            },
                        );

                        await this.submitButton.click();

                        // Считаем, что изменения применились, когда кнопка Сохранить станет активной,
                        // так как ожидаем, что ручка вернёт ошибку
                        await this.browser.allure.runStep('Дожидаемся сохранения данных по бренду и категориям', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const disabled = await this.browser.yaSafeAction(
                                        this.submitButton.isDisabled(),
                                        true,
                                    );

                                    // @ts-expect-error(TS2367) найдено в рамках VNDFRONT-4580
                                    return disabled === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Кнопка "Сохранить" неактивна',
                            ),
                        );

                        await this.form.submitError
                            .vndIsExisting()
                            .should.eventually.be.equal(true, 'Сообщение об ошибке у кнопки "Сохранить" отображается');

                        await this.form.submitError
                            .getText()
                            .should.eventually.include(
                                'Карточка для комбинации этого бренда и категории уже существует',
                                'Текст ошибки корректный',
                            );
                    },
                }),
            },
        },
    ),
});
