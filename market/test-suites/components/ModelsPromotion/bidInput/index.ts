'use strict';

import {mergeSuites, importSuite, PageObject, makeSuite, makeCase} from 'ginny';

const ModelBidRecommendationTooltip = PageObject.get('ModelBidRecommendationTooltip');

/**
 * Тесты на изменение ставки товара
 * @param {PageObject.ListContainer} list - список товаров
 * @param {PageObject.ModelsPromotionListItem} item - товар из списка
 * @param {PageObject.TextFieldLevitan} textField - поле ввода ставки
 * @param {PageObject.ModelBidRecommendationTooltip} modelBidRecommendationTooltip - тултип с рекомендованными ставками
 * @param {PageObject.RatesControlBar} bar - ссылка установки ставки
 * @param {PageObject.ButtonB2b} submitButton - кнопка применения ставки
 * @param {PageObject.ButtonB2b} cancelButton - кнопка отмены ставки
 * @param {PageObject.NotificationLevitan} toast - хинт с ошибкой
 */
export default makeSuite('Ставка.', {
    issue: 'VNDFRONT-2249',
    environment: 'kadavr',
    feature: 'Прогнозатор',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.allure.runStep('Ожидаем появления списка товаров', () => this.list.waitForExist());
                await this.list.waitForLoading();

                return this.browser.allure.runStep('Ожидаем появления товара', () => this.item.waitForExist());
            },
        },
        importSuite('ModelsPromotion/bidInput/ratesPopup', {
            suiteName: 'Подсказка с минимальной ставкой.',
            meta: {
                id: 'vendor_auto-681',
            },
            params: {
                expectedBid: '1.2',
            },
            pageObjects: {
                link() {
                    return this.createPageObject(
                        'LinkLevitan',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        `${ModelBidRecommendationTooltip.root} ul > li:nth-child(1) > ${ModelBidRecommendationTooltip.recommendationInfo}`,
                    );
                },
            },
        }),
        importSuite('ModelsPromotion/bidInput/ratesPopup', {
            suiteName: 'Подсказка с максимальной ставкой.',
            meta: {
                id: 'vendor_auto-682',
            },
            params: {
                expectedBid: '2.8',
            },
            pageObjects: {
                link() {
                    return this.createPageObject(
                        'LinkLevitan',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        `${ModelBidRecommendationTooltip.root} ul > li:nth-child(2) > ${ModelBidRecommendationTooltip.recommendationInfo}`,
                    );
                },
            },
        }),
        importSuite('ModelsPromotion/bidInput/valueApply', {
            suiteName: 'Проставление положительной ставки.',
            meta: {
                id: 'vendor_auto-685',
            },
            params: {
                status: 'Включается\nНовая ставка применится\nв течение 4 часов',
                value: '9.95',
            },
        }),
        importSuite('ModelsPromotion/bidInput/valueApply', {
            suiteName: 'Сброс ставки.',
            meta: {
                id: 'vendor_auto-687',
            },
            params: {
                status: 'Выключается\nНовая ставка применится\nв течение 4 часов',
                value: '0',
            },
        }),
        {
            'При сохранении ставки более 84.00': {
                'отображается ошибка': makeCase({
                    id: 'vendor_auto-684',
                    environment: 'testing',
                    async test() {
                        await this.textField.setValue('84.01');

                        await this.browser.allure.runStep('Ожидаем появления сайдбара управления ставками', () =>
                            this.bar.waitForVisible(),
                        );

                        await this.submitButton.click();

                        await this.allure.runStep('Ожидаем появления сообщения об ошибке', () =>
                            this.toast.waitForExist(),
                        );

                        await this.toast
                            .getText()
                            .should.eventually.be.equal(
                                'Некорректная ставка — укажите ставку до 84 у. e.',
                                'Текст ошибки корректный',
                            );
                    },
                }),
            },
            'При отмене сохранения ставки': {
                'значение в поле не меняется': makeCase({
                    id: 'vendor_auto-686',
                    async test() {
                        const initialBid = await this.textField.placeholder;

                        await this.textField.setValue('3.14');

                        await this.browser.allure.runStep('Ожидаем появления сайдбара управления ставками', () =>
                            this.bar.waitForVisible(),
                        );

                        await this.browser.allure.runStep('Отменяем новую ставку', () => this.cancelButton.click());

                        await this.allure.runStep('Ожидаем скрытия сайдбара управления ставками', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const visible = await this.bar.isVisible();

                                    return visible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться скрытия сайдбара',
                            ),
                        );

                        await this.browser.allure.runStep('Проверяем отсутствие новой ставки в поле ввода', () =>
                            this.textField.value.should.eventually.be.equal('', 'Поле ввода ставки пустое'),
                        );

                        await this.browser.allure.runStep('Проверяем соответствие ставки изначальной', () =>
                            this.textField.placeholder.should.eventually.be.equal(
                                initialBid,
                                `Ставка соответствует изначальной "${initialBid}"`,
                            ),
                        );
                    },
                }),
            },
        },
    ),
});
