'use strict';

import {importSuite, mergeSuites, PageObject, makeSuite, makeCase} from 'ginny';

const ProductInfoLabel = PageObject.get('ProductInfoLabel');
const ButtonLevitan = PageObject.get('ButtonLevitan');
const TextLevitan = PageObject.get('TextLevitan');

/**
 * Тесты на товар из списка услуги "Отзывы за баллы"
 * @param {PageObject.OpinionsPromotionListItem} item - товар
 * @param {PageObject.OpinionsPromotionListItem} nextItem - вспомогательный товар
 * @param {PageObject.OpinionsPromotionListItem} thirdItem - вспомогательный товар
 * @param {PageObject.OpinionsPromotionAgitationStats} agitationStats - блок со статистикой
 */
export default makeSuite('Товар.', {
    feature: 'Отзывы за баллы',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites({
        beforeEach() {
            return this.allure.runStep('Ожидаем появления элемента товара в списке', () => this.item.waitForExist());
        },
        'Баллы.': {
            beforeEach() {
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.setPageObjects({
                    toasts() {
                        return this.createPageObject('NotificationGroupLevitan');
                    },
                    firstToast() {
                        return this.createPageObject('NotificationLevitan', this.toasts, this.toasts.getItemByIndex(0));
                    },
                    agitationStatsSpinner() {
                        return this.createPageObject('SpinnerLevitan', this.agitationStats);
                    },
                    agitationStatsModelsCount() {
                        return this.createPageObject(
                            'ProductInfoLabel',
                            this.agitationStats,
                            `${ProductInfoLabel.root}:nth-child(1)`,
                        );
                    },
                    agitationStatsModelsCountText() {
                        return this.createPageObject(
                            'TextLevitan',
                            this.agitationStatsModelsCount,
                            `${TextLevitan.root}:last-child`,
                        );
                    },
                    bar() {
                        return this.createPageObject('OpinionsPromotionRatesControlBar');
                    },
                    submitButton() {
                        return this.createPageObject('ButtonLevitan', this.bar, `${ButtonLevitan.root}:first-child`);
                    },
                    cancelButton() {
                        return this.createPageObject('ButtonLevitan', this.bar, `${ButtonLevitan.root}:last-child`);
                    },
                    inputWithRates() {
                        return this.createPageObject('InputWithRates', this.item.cashbackContainer);
                    },
                    input() {
                        return this.createPageObject('InputB2b', this.inputWithRates);
                    },
                    inputHint() {
                        return this.createPageObject('TextLevitan', this.item.cashbackContainer);
                    },
                });
            },
            'Введённое в поле значение': {
                валидируется: makeCase({
                    id: 'vendor_auto-1326',
                    issue: 'VNDFRONT-3995',
                    async test() {
                        this.setPageObjects({
                            ratesPopup() {
                                return this.createPageObject('RatesPopup');
                            },
                            minCashbackLink() {
                                return this.createPageObject('Link', this.ratesPopup.getItemByIndex(0));
                            },
                            maxCashbackLink() {
                                return this.createPageObject('Link', this.ratesPopup.getItemByIndex(1));
                            },
                        });

                        const cashback = await this.input.value;

                        /**
                         * Предустанавливаем минимальное количество баллов
                         */
                        await this.input.setFocus();

                        await this.browser.allure.runStep('Ожидаем появления всплывающего окна с баллами', () =>
                            this.ratesPopup.waitForVisible(),
                        );

                        const minCashback = await this.minCashbackLink.getText();

                        await this.browser.allure.runStep(
                            `Кликаем на минимальное количество баллов "${minCashback}"`,
                            () => this.minCashbackLink.root.click(),
                        );

                        await this.browser.allure.runStep('Проверяем изменение значения в поле ввода', () =>
                            this.input.value.should.eventually.be.equal(
                                minCashback,
                                `Значение изменилось на "${minCashback}"`,
                            ),
                        );

                        await this.browser.allure.runStep('Ожидаем появления сайдбара управления баллами', () =>
                            this.bar.waitForVisible(),
                        );

                        await this.browser.allure.runStep('Отменяем изменение баллов', () => this.cancelButton.click());

                        await this.allure.runStep('Ожидаем скрытия сайдбара управления баллами', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const visible = await this.bar.isVisible();

                                    return visible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться скрытия сайдбара',
                            ),
                        );

                        await this.browser.allure.runStep('Проверяем сброс баллов на изначальное значение', () =>
                            this.input.value.should.eventually.be.equal(cashback, 'Баллы сброшены'),
                        );

                        /**
                         * Предустанавливаем максимальное количество баллов
                         */
                        await this.input.setFocus();

                        await this.browser.allure.runStep('Ожидаем появления всплывающего окна с баллами', () =>
                            this.ratesPopup.waitForVisible(),
                        );

                        const maxCashback = await this.maxCashbackLink.getText();

                        await this.browser.allure.runStep(
                            `Кликаем на максимальное количество баллов "${maxCashback}"`,
                            () => this.maxCashbackLink.root.click(),
                        );

                        await this.browser.allure.runStep('Проверяем изменение значения в поле ввода', () =>
                            this.input.value.should.eventually.be.equal(
                                maxCashback,
                                `Значение изменилось на "${maxCashback}"`,
                            ),
                        );

                        await this.browser.allure.runStep('Ожидаем появления сайдбара управления баллами', () =>
                            this.bar.waitForVisible(),
                        );

                        /**
                         * Проверяем нижнюю границу значений
                         */
                        await this.input.setValue('49');

                        await this.browser.allure.runStep('Ожидаем появления сайдбара управления баллами', () =>
                            this.bar.waitForVisible(),
                        );

                        await this.allure.runStep('Ожидаем дизейбла кнопки [Сохранить]', () =>
                            this.browser.waitUntil(
                                () => this.submitButton.isDisabled(),
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться дизейбла кнопки',
                            ),
                        );

                        /**
                         * Проверяем верхнюю границу значений
                         */
                        await this.input.setValue('1001');

                        await this.browser.allure.runStep('Ожидаем появления сайдбара управления баллами', () =>
                            this.bar.waitForVisible(),
                        );

                        await this.allure.runStep('Ожидаем дизейбла кнопки [Сохранить]', () =>
                            this.browser.waitUntil(
                                () => this.submitButton.isDisabled(),
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться дизейбла кнопки',
                            ),
                        );

                        /**
                         * Проверяем фильтрацию невалидных значений в поле ввода
                         */
                        await this.input.setValue('-678.,abcабв');

                        await this.browser.allure.runStep('Проверяем значение поля ввода баллов', () =>
                            this.input.value.should.eventually.be.equal('678', 'Значение отфильтровалось корректно'),
                        );
                    },
                }),
                сохраняется: makeCase({
                    id: 'vendor_auto-1327',
                    issue: 'VNDFRONT-4003',
                    async test() {
                        const cashback = '98';

                        await this.input.setValue(cashback);

                        await this.browser.allure.runStep('Ожидаем появления сайдбара управления баллами', () =>
                            this.bar.waitForVisible(),
                        );

                        await this.submitButton.click();

                        await this.allure.runStep('Ожидаем скрытия сайдбара управления баллами', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const visible = await this.bar.isVisible();

                                    return visible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться скрытия сайдбара',
                            ),
                        );

                        await this.browser.allure.runStep('Ожидаем показа всплывающего сообщения', () =>
                            this.firstToast.waitForVisible(),
                        );

                        await this.browser.allure.runStep('Проверяем текст всплывающего сообщения', () =>
                            this.firstToast
                                .getText()
                                .should.eventually.equal(
                                    'Настроен сбор отзывов для 1 товара',
                                    'Текст всплывающего сообщения корректный',
                                ),
                        );

                        await this.browser.allure.runStep('Проверяем изменение значения в поле ввода', () =>
                            this.input.value.should.eventually.be.equal(
                                cashback,
                                `Значение изменилось на "${cashback}"`,
                            ),
                        );

                        await this.browser.allure.runStep('Проверяем текст подсказки под полем ввода', () =>
                            this.inputHint
                                .getText()
                                .should.eventually.be.equal('Спишем 108 ₽ или 3.6 у. е.', 'Текст подсказки корректный'),
                        );

                        await this.browser.allure.runStep('Ожидаем появления блока со статистикой', () =>
                            this.agitationStats.waitForExist(),
                        );

                        await this.browser.allure.runStep('Ожидаем загрузки блока со статистикой', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const visible = await this.agitationStatsSpinner.isVisible();

                                    return visible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться скрытия спиннера',
                            ),
                        );

                        await this.browser.allure.runStep('Проверяем количество товаров в блоке статистики', () =>
                            this.agitationStatsModelsCountText
                                .getText()
                                .should.eventually.be.equal('1 товара', 'Текст с количеством товаров корректный'),
                        );
                    },
                }),
            },
            'При сбросе значения': {
                'сбор отзывов прекращается': makeCase({
                    id: 'vendor_auto-1328',
                    issue: 'VNDFRONT-4003',
                    async test() {
                        this.setPageObjects({
                            agitationStatsCharges() {
                                return this.createPageObject(
                                    'ProductInfoLabel',
                                    this.agitationStats,
                                    `${ProductInfoLabel.root}:nth-child(2)`,
                                );
                            },
                            agitationStatsChargesText() {
                                return this.createPageObject(
                                    'TextLevitan',
                                    this.agitationStatsCharges,
                                    `${TextLevitan.root}:last-child`,
                                );
                            },
                            agitationStatsNonPromotedMessageHeading() {
                                return this.createPageObject(
                                    'TextLevitan',
                                    this.agitationStats.nonPromotedMessage,
                                    `${TextLevitan.root}:first-child`,
                                );
                            },
                            targetOpinionsInput() {
                                return this.createPageObject('InputB2b', this.item.targetOpinionsContainer);
                            },
                            nextInputWithRates() {
                                return this.createPageObject('InputWithRates', this.nextItem.cashbackContainer);
                            },
                            nextInput() {
                                return this.createPageObject('InputB2b', this.nextInputWithRates);
                            },
                            nextInputHint() {
                                return this.createPageObject('TextLevitan', this.nextItem.cashbackContainer);
                            },
                            nextTargetOpinionsInput() {
                                return this.createPageObject('InputB2b', this.nextItem.targetOpinionsContainer);
                            },
                        });

                        await this.allure.runStep('Ожидаем появления вспомогательного товара в списке', () =>
                            this.nextItem.waitForExist(),
                        );

                        const cashback = '50';
                        const targetOpinions = '1';
                        const nextCashback = '100';
                        const nextTargetOpinions = '2';

                        await this.browser.allure.runStep('Вводим баллы и цели для первого товара', async () => {
                            await this.input.setValue(cashback);

                            await this.browser.allure.runStep('Ждём пока поле «Цель» станет активным', () =>
                                this.browser.waitUntil(
                                    async () => {
                                        const isDisabled = await this.targetOpinionsInput.isDisabled();

                                        return isDisabled === false;
                                    },
                                    this.browser.options.waitforTimeout,
                                    'Не удалось дождаться активного поля «Цель»',
                                ),
                            );

                            await this.targetOpinionsInput.setValue(targetOpinions);
                        });

                        await this.browser.allure.runStep('Вводим баллы и цели для второго товара', async () => {
                            await this.nextInput.setValue(nextCashback);

                            await this.browser.allure.runStep('Ждём пока поле «Цель» станет активным', () =>
                                this.browser.waitUntil(
                                    async () => {
                                        const isDisabled = await this.nextTargetOpinionsInput.isDisabled();

                                        return isDisabled === false;
                                    },
                                    this.browser.options.waitforTimeout,
                                    'Не удалось дождаться активного поля «Цель»',
                                ),
                            );

                            await this.nextTargetOpinionsInput.setValue(nextTargetOpinions);
                        });

                        await this.browser.allure.runStep('Ожидаем появления сайдбара управления баллами', () =>
                            this.bar.waitForVisible(),
                        );

                        await this.browser.allure.runStep('Ожидаем появления кнопки сохранения', () =>
                            this.submitButton.waitForVisible(),
                        );

                        await this.submitButton.click();

                        await this.allure.runStep('Ожидаем скрытия сайдбара управления баллами', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const visible = await this.bar.isVisible();

                                    return visible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться скрытия сайдбара',
                            ),
                        );

                        await this.browser.allure.runStep('Ожидаем показа всплывающего сообщения', () =>
                            this.firstToast.waitForVisible(),
                        );

                        await this.browser.allure.runStep('Проверяем текст всплывающего сообщения', () =>
                            this.firstToast
                                .getText()
                                .should.eventually.equal(
                                    'Настроен сбор отзывов для 2 товаров',
                                    'Текст всплывающего сообщения корректный',
                                ),
                        );

                        await this.browser.allure.runStep('Проверяем значение баллов для первого товара', () =>
                            this.input.value.should.eventually.be.equal(
                                cashback,
                                `Значение изменилось на "${cashback}"`,
                            ),
                        );

                        await this.browser.allure.runStep(
                            'Проверяем текст подсказки под полем ввода баллов для первого товара',
                            () =>
                                this.inputHint
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Спишем 55.2 ₽ или 1.84 у. е.',
                                        'Текст подсказки корректный',
                                    ),
                        );

                        await this.browser.allure.runStep('Проверяем значение целей для первого товара', () =>
                            this.targetOpinionsInput.value.should.eventually.be.equal(
                                targetOpinions,
                                `Значение изменилось на "${targetOpinions}"`,
                            ),
                        );

                        await this.browser.allure.runStep('Проверяем значение баллов для второго товара', () =>
                            this.nextInput.value.should.eventually.be.equal(
                                nextCashback,
                                `Значение изменилось на "${nextCashback}"`,
                            ),
                        );

                        await this.browser.allure.runStep(
                            'Проверяем текст подсказки под полем ввода баллов для второго товара',
                            () =>
                                this.nextInputHint
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Спишем 109.8 ₽ или 3.66 у. е.',
                                        'Текст подсказки корректный',
                                    ),
                        );

                        await this.browser.allure.runStep('Проверяем значение целей для второго товара', () =>
                            this.nextTargetOpinionsInput.value.should.eventually.be.equal(
                                nextTargetOpinions,
                                `Значение изменилось на "${nextTargetOpinions}"`,
                            ),
                        );

                        await this.browser.allure.runStep('Ожидаем появления блока со статистикой', () =>
                            this.agitationStats.waitForExist(),
                        );

                        await this.browser.allure.runStep('Ожидаем загрузки блока со статистикой', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const visible = await this.agitationStatsSpinner.isVisible();

                                    return visible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться скрытия спиннера',
                            ),
                        );

                        await this.browser.allure.runStep('Проверяем количество товаров в блоке статистики', () =>
                            this.agitationStatsModelsCountText
                                .getText()
                                .should.eventually.be.equal('2 товаров', 'Текст с количеством товаров корректный'),
                        );

                        await this.browser.allure.runStep('Проверяем ожидаемые расходы в блоке статистики', () =>
                            this.agitationStatsChargesText
                                .getText()
                                .should.eventually.be.equal('16,52 у. е.495,60 ₽', 'Ожидаемые расходы корректные'),
                        );

                        await this.browser.allure.runStep(
                            'Проверяем прогресс по сбору отзывов в блоке статистики',
                            () =>
                                this.agitationStats.progressInfo
                                    .getText()
                                    .should.eventually.be.equal(
                                        '0 из 3\nотзывов собрано для 2 целей',
                                        'Прогресс по сбору отзывов корректный',
                                    ),
                        );

                        /**
                         * Сбрасываем баллы
                         */
                        await this.browser.allure.runStep('Сбрасываем баллы для первого товара', () =>
                            this.input.setValue(''),
                        );

                        await this.browser.allure.runStep('Сбрасываем баллы для второго товара', () =>
                            this.nextInput.setValue('0'),
                        );

                        await this.browser.allure.runStep('Ожидаем появления сайдбара управления баллами', () =>
                            this.bar.waitForVisible(),
                        );

                        await this.browser.allure.runStep('Ожидаем появления кнопки сохранения', () =>
                            this.submitButton.waitForVisible(),
                        );

                        await this.submitButton.click();

                        await this.allure.runStep('Ожидаем скрытия сайдбара управления баллами', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const visible = await this.bar.isVisible();

                                    return visible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться скрытия сайдбара',
                            ),
                        );

                        await this.browser.allure.runStep('Ожидаем показа всплывающего сообщения', () =>
                            this.firstToast.waitForVisible(),
                        );

                        await this.browser.allure.runStep('Проверяем текст всплывающего сообщения', () =>
                            this.firstToast
                                .getText()
                                .should.eventually.equal(
                                    'Настроен сбор отзывов для 2 товаров',
                                    'Текст всплывающего сообщения корректный',
                                ),
                        );

                        await this.browser.allure.runStep('Ожидаем загрузки блока со статистикой', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const visible = await this.agitationStatsSpinner.isVisible();

                                    return visible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться скрытия спиннера',
                            ),
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем появления сообщения о ненастроенном сборе баллов',
                            () => this.agitationStatsNonPromotedMessageHeading.waitForExist(),
                        );

                        await this.browser.allure.runStep(
                            'Проверяем заголовок сообщения о ненастроенном сборе баллов',
                            () =>
                                this.agitationStatsNonPromotedMessageHeading
                                    .getText()
                                    .should.eventually.be.equal(
                                        'У вас пока не настроен сбор отзывов за баллы Плюса',
                                        'Заголовок сообщения корректный',
                                    ),
                        );
                    },
                }),
            },
        },
        'Цели.': {
            beforeEach() {
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.setPageObjects({
                    toasts() {
                        return this.createPageObject('NotificationGroupLevitan');
                    },
                    toast() {
                        return this.createPageObject('NotificationLevitan', this.toasts, this.toasts.getItemByIndex(0));
                    },
                    bar() {
                        return this.createPageObject('OpinionsPromotionRatesControlBar');
                    },
                    submitButton() {
                        return this.createPageObject('ButtonLevitan', this.bar, `${ButtonLevitan.root}:first-child`);
                    },
                    cancelButton() {
                        return this.createPageObject('ButtonLevitan', this.bar, `${ButtonLevitan.root}:last-child`);
                    },
                    cashbackInput() {
                        return this.createPageObject('InputB2b', this.item.cashbackContainer);
                    },
                    targetInput() {
                        return this.createPageObject('InputB2b', this.item.targetOpinionsContainer);
                    },
                    inputHint() {
                        return this.createPageObject('TextLevitan', this.item.cashbackContainer);
                    },
                });
            },
            'Значение в поле «Цель»': mergeSuites(
                {
                    'валидируется и сохраняется': {
                        корректно: makeCase({
                            id: 'vendor_auto-1329',
                            environment: 'kadavr',
                            async test() {
                                await this.cashbackInput.value.should.eventually.be.equal(
                                    '',
                                    'Поле «Баллы» у первого товара пустое',
                                );

                                await this.targetInput
                                    .isDisabled()
                                    .should.eventually.be.equal(true, 'Поле «Цель» у первого товара задизейблено');

                                await this.cashbackInput.setValue('66');

                                await this.browser.allure.runStep('Ждём пока поле «Цель» станет активным', () =>
                                    this.browser.waitUntil(
                                        async () => {
                                            const disabled = await this.targetInput.isDisabled();

                                            return disabled === false;
                                        },
                                        this.browser.options.waitforTimeout,
                                        'Не удалось дождаться активации поля «Цель»',
                                    ),
                                );

                                await this.targetInput.setValue('абв-');

                                await this.targetInput.value.should.eventually.be.equal(
                                    '',
                                    'Поле «Цель» у первого товара осталось пустым',
                                );

                                await this.targetInput.setValue('5');

                                await this.submitButton
                                    .isVisible()
                                    .should.eventually.be.equal(true, 'Кнопка сохранения баллов и ставок отображается');

                                await this.cashbackInput.setValue('');

                                await this.browser.allure.runStep('Ждём пока поле «Цель» станет задизейбленным', () =>
                                    this.browser.waitUntil(
                                        () => this.targetInput.isDisabled(),
                                        this.browser.options.waitforTimeout,
                                        'Не удалось дождаться дизейбла поля «Цель»',
                                    ),
                                );

                                await this.submitButton
                                    .isVisible()
                                    .should.eventually.be.equal(
                                        true,
                                        'Кнопка сохранения баллов и ставок по-прежнему отображается',
                                    );

                                await this.submitButton.click();

                                await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                                    this.toasts.waitForVisible(),
                                );

                                await this.browser.allure.runStep(
                                    'Ожидаем показа всплывающего сообщения с подтверждением',
                                    () => this.toast.waitForVisible(),
                                );

                                await this.toast
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Настроен сбор отзывов для 1 товара',
                                        'Текст всплывающего сообщения верный',
                                    );

                                await this.cashbackInput.value.should.eventually.be.equal(
                                    '0',
                                    'Поле «Баллы» у первого товара пустое',
                                );

                                await this.targetInput
                                    .isDisabled()
                                    .should.eventually.be.equal(true, 'Поле «Цель» у первого товара задизейблено');

                                await this.targetInput.value.should.eventually.be.equal(
                                    '0',
                                    'Поле «Цель» у первого товара пустое',
                                );
                            },
                        }),
                    },
                },
                {
                    редактируется: {
                        корректно: makeCase({
                            id: 'vendor_auto-1341',
                            environment: 'kadavr',
                            async test() {
                                this.setPageObjects({
                                    agitationStatsSpinner() {
                                        return this.createPageObject('SpinnerLevitan', this.agitationStats);
                                    },
                                    cashbackInput() {
                                        return this.createPageObject('InputB2b', this.thirdItem.cashbackContainer);
                                    },
                                    targetInput() {
                                        return this.createPageObject(
                                            'InputB2b',
                                            this.thirdItem.targetOpinionsContainer,
                                        );
                                    },
                                    targetOpinionsHint() {
                                        return this.createPageObject(
                                            'TextLevitan',
                                            this.thirdItem,
                                            this.thirdItem.targetOpinionsHint,
                                        );
                                    },
                                    agitationStatsModelsCount() {
                                        return this.createPageObject(
                                            'ProductInfoLabel',
                                            this.agitationStats,
                                            `${ProductInfoLabel.root}:nth-child(1)`,
                                        );
                                    },
                                    agitationStatsModelsCountText() {
                                        return this.createPageObject(
                                            'TextLevitan',
                                            this.agitationStatsModelsCount,
                                            `${TextLevitan.root}:last-child`,
                                        );
                                    },
                                });

                                await this.browser.allure.runStep('Дожидаемся появления блока со статистикой', () =>
                                    this.agitationStats.waitForExist(),
                                );

                                await this.browser.allure.runStep('Дожидаемся загрузки блока со статистикой', () =>
                                    this.browser.waitUntil(
                                        async () => {
                                            const visible = await this.agitationStatsSpinner.isVisible();

                                            return visible === false;
                                        },
                                        this.browser.options.waitforTimeout,
                                        'Не удалось дождаться скрытия спиннера',
                                    ),
                                );

                                await this.agitationStatsModelsCountText
                                    .getText()
                                    .should.eventually.be.equal(
                                        '2 товаров',
                                        'Количество товаров, для которых назначен сбор отображается корректно',
                                    );

                                await this.agitationStats.progressInfo
                                    .getText()
                                    .should.eventually.be.equal(
                                        '2 из 3\nотзывов собрано для 1 цели',
                                        'Прогресс по сбору целей отображается корректно',
                                    );

                                await this.cashbackInput.value.should.eventually.be.equal(
                                    '100',
                                    'Поле «Баллы» у товара заполнено',
                                );

                                await this.targetInput
                                    .isDisabled()
                                    .should.eventually.be.equal(false, 'Поле «Цель» у товара активно');

                                await this.targetInput.value.should.eventually.be.equal(
                                    '',
                                    'Поле «Цель» у товара не заполнено',
                                );

                                await this.targetOpinionsHint
                                    .isVisible()
                                    .should.eventually.be.equal(true, 'Подсказка у поля «Цель» отображается');

                                await this.targetOpinionsHint
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Отзывы собираются бесконечно',
                                        'Подсказка у поля «Цель» корректная',
                                    );

                                await this.targetInput.setValue('5');

                                await this.allure.runStep('Ожидаем появления кнопки сохранения', () =>
                                    this.submitButton.waitForExist(),
                                );

                                await this.submitButton.click();

                                await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                                    this.toasts.waitForVisible(),
                                );

                                await this.browser.allure.runStep(
                                    'Ожидаем показа всплывающего сообщения с подтверждением',
                                    () => this.toast.waitForVisible(),
                                );

                                await this.toast
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Настроен сбор отзывов для 1 товара',
                                        'Текст всплывающего сообщения верный',
                                    );

                                await this.targetOpinionsHint
                                    .isVisible()
                                    .should.eventually.be.equal(false, 'Подсказка у поля «Цель» скрылась');

                                await this.agitationStatsModelsCountText
                                    .getText()
                                    .should.eventually.be.equal(
                                        '2 товаров',
                                        'Количество товаров, для которых назначен сбор отображается корректно',
                                    );

                                await this.agitationStats.progressInfo
                                    .getText()
                                    .should.eventually.be.equal(
                                        '2 из 8\nотзывов собрано для 2 целей',
                                        'Прогресс по сбору целей отображается корректно',
                                    );

                                await this.targetInput.setValue('8');

                                await this.allure.runStep('Ожидаем появления кнопки сохранения', () =>
                                    this.submitButton.waitForExist(),
                                );

                                await this.submitButton.click();

                                await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                                    this.toasts.waitForVisible(),
                                );

                                await this.browser.allure.runStep(
                                    'Ожидаем показа всплывающего сообщения с подтверждением',
                                    () => this.toast.waitForVisible(),
                                );

                                await this.toast
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Настроен сбор отзывов для 1 товара',
                                        'Текст всплывающего сообщения верный',
                                    );

                                await this.agitationStatsModelsCountText
                                    .getText()
                                    .should.eventually.be.equal(
                                        '2 товаров',
                                        'Количество товаров, для которых назначен сбор отображается корректно',
                                    );

                                await this.agitationStats.progressInfo
                                    .getText()
                                    .should.eventually.be.equal(
                                        '2 из 11\nотзывов собрано для 2 целей',
                                        'Прогресс по сбору целей отображается корректно',
                                    );
                            },
                        }),
                    },
                },
                {
                    сбрасывается: mergeSuites(
                        importSuite('OpinionsPromotion/resetTargetOpinionsCount', {
                            suiteName: 'со значением 0',
                            meta: {
                                feature: 'Отзывы за баллы',
                                id: 'vendor_auto-1330',
                                environment: 'kadavr',
                            },
                            params: {
                                cashBackInitialValue: '100',
                                targetOpinionsResetValue: '0',
                                targetOpinionsInitialValue: '3',
                            },
                        }),
                        importSuite('OpinionsPromotion/resetTargetOpinionsCount', {
                            suiteName: 'с пустым значением',
                            meta: {
                                feature: 'Отзывы за баллы',
                                id: 'vendor_auto-1330',
                                environment: 'kadavr',
                            },
                            params: {
                                cashBackInitialValue: '100',
                                targetOpinionsResetValue: '',
                                targetOpinionsInitialValue: '3',
                            },
                        }),
                    ),
                },
                {
                    'при текущем количестве отзывов большим или равным целевому количеству отзывов': {
                        'помечается значком «Цель достигнута»': makeCase({
                            id: 'vendor_auto-1332',
                            environment: 'kadavr',
                            async test() {
                                this.setPageObjects({
                                    targetOpinionsTick() {
                                        return this.createPageObject('IconLevitan', this.item.targetOpinionsContainer);
                                    },
                                });

                                await this.cashbackInput.value.should.eventually.be.equal(
                                    '100',
                                    'Поле «Баллы» у первого товара заполнено',
                                );

                                await this.targetInput.value.should.eventually.be.equal(
                                    '3',
                                    'Поле «Цель» у первого товара заполнено',
                                );

                                await this.item.currentOpinionsCollectedColumn
                                    .getText()
                                    .should.eventually.be.equal('2', 'Количество собранных отзывов корректное');

                                await this.targetOpinionsTick
                                    .isVisible()
                                    .should.eventually.be.equal(
                                        false,
                                        'Значок «Цель достигнута» у поля «Цель» не отображается',
                                    );

                                /* Устанавливаем цель равной текущему количеству отзывов */

                                await this.targetInput.setValue('2');

                                await this.allure.runStep('Ожидаем появления кнопки сохранения', () =>
                                    this.submitButton.waitForExist(),
                                );

                                await this.submitButton.click();

                                await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                                    this.toasts.waitForVisible(),
                                );

                                await this.browser.allure.runStep(
                                    'Ожидаем показа всплывающего сообщения с подтверждением',
                                    () => this.toast.waitForVisible(),
                                );

                                await this.toast
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Настроен сбор отзывов для 1 товара',
                                        'Текст всплывающего сообщения верный',
                                    );

                                await this.targetOpinionsTick
                                    .isVisible()
                                    .should.eventually.be.equal(
                                        true,
                                        'Значок «Цель достигнута» у поля «Цель» отображается',
                                    );

                                /* Устанавливаем цель меньше текущего количества отзывов */

                                await this.targetInput.setValue('1');

                                await this.allure.runStep('Ожидаем появления кнопки сохранения', () =>
                                    this.submitButton.waitForExist(),
                                );

                                await this.submitButton.click();

                                await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                                    this.toasts.waitForVisible(),
                                );

                                await this.browser.allure.runStep(
                                    'Ожидаем показа всплывающего сообщения с подтверждением',
                                    () => this.toast.waitForVisible(),
                                );

                                await this.toast
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Настроен сбор отзывов для 1 товара',
                                        'Текст всплывающего сообщения верный',
                                    );

                                await this.targetOpinionsTick
                                    .isVisible()
                                    .should.eventually.be.equal(
                                        true,
                                        'Значок «Цель достигнута» у поля «Цель» отображается',
                                    );

                                /* Устанавливаем цель больше текущего количества отзывов */

                                await this.targetInput.setValue('666');

                                await this.allure.runStep('Ожидаем появления кнопки сохранения', () =>
                                    this.submitButton.waitForExist(),
                                );

                                await this.submitButton.click();

                                await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                                    this.toasts.waitForVisible(),
                                );

                                await this.browser.allure.runStep(
                                    'Ожидаем показа всплывающего сообщения с подтверждением',
                                    () => this.toast.waitForVisible(),
                                );

                                await this.toast
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Настроен сбор отзывов для 1 товара',
                                        'Текст всплывающего сообщения верный',
                                    );

                                await this.targetOpinionsTick
                                    .isVisible()
                                    .should.eventually.be.equal(
                                        false,
                                        'Значок «Цель достигнута» у поля «Цель» не отображается',
                                    );
                            },
                        }),
                    },
                },
            ),
        },
    }),
});
