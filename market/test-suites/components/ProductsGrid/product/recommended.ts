'use strict';

import {makeCase, makeSuite, mergeSuites, importSuite} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

import showProductHook from './hooks/showProduct';

/**
 * Тесты на услугу Рекомендованные магазины
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 */
export default makeSuite('Услуга Рекомендованные магазины.', {
    issue: 'VNDFRONT-4348',
    environment: 'kadavr',
    feature: 'Управление услугами и пользователями',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(
        showProductHook({
            managerView: false,
            details: false,
        }),
        {
            async beforeEach() {
                this.setPageObjects({
                    recommendedProduct() {
                        return this.createPageObject('RecommendedProduct');
                    },
                    spinner() {
                        return this.createPageObject('SpinnerLevitan', this.recommendedProduct);
                    },
                    link() {
                        return this.createPageObject('RecommendedBusinessesLinkButton', this.recommendedProduct);
                    },
                    officialCounterText() {
                        return this.createPageObject('TextNextLevitan', this.recommendedProduct.officialCounter);
                    },
                    recommendationCounterText() {
                        return this.createPageObject('TextNextLevitan', this.recommendedProduct.recommendationCounter);
                    },
                    details() {
                        return this.createPageObject('ProductDetails', this.product);
                    },
                });

                await this.browser.allure.runStep('Ожидаем загрузки рекомендаций', () =>
                    this.browser.waitUntil(
                        async () => {
                            const visible = await this.spinner.isVisible();

                            return visible === false;
                        },
                        this.browser.options.waitforTimeout,
                        'Не удалось дождаться загрузки рекомендаций',
                    ),
                );
            },
        },
        {
            'Для подключенной услуги Рекомендованных магазинов': {
                'если рекомендации настроены, то': {
                    'отображаются счётчики с рекомендациями': makeCase({
                        id: 'vendor_auto-1456',
                        async test() {
                            const {isManager} = this.params;

                            await this.product.cover
                                .vndIsExisting()
                                .should.eventually.be.equal(false, 'Обложка услуги не отображается');

                            await this.officialCounterText
                                .isVisible()
                                .should.eventually.be.equal(true, 'Счётчик официальных магазинов отображается');

                            await this.officialCounterText
                                .getText()
                                .should.eventually.be.equal(
                                    '2 официальных магазина',
                                    'Текст в счётчике официальных магазинов корректный',
                                );

                            await this.recommendationCounterText
                                .isVisible()
                                .should.eventually.be.equal(true, 'Счётчик представителей брендов отображается');

                            await this.recommendationCounterText
                                .getText()
                                .should.eventually.be.equal(
                                    '1 представитель бренда',
                                    'Текст в счётчике представителей брендов корректный',
                                );

                            await this.link
                                .isVisible()
                                .should.eventually.be.equal(
                                    true,
                                    'Кнопка-ссылка на страницу с услугой «Настроить» отображается',
                                );

                            if (isManager) {
                                await this.details
                                    .isVisible()
                                    .should.eventually.be.equal(true, 'Менеджерский блок отображается');
                            }
                        },
                    }),
                },
            },
        },
        importSuite('Link', {
            suiteName: 'Ссылка «Настроить» на страницу услуги',
            meta: {
                id: 'vendor_auto-1457',
                environment: 'kadavr',
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {vendor} = this.params;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.url = buildUrl(ROUTE_NAMES.BUSINESSES, {vendor});
                },
            },
            params: {
                caption: 'Настроить',
                comparison: {
                    skipHostname: true,
                },
            },
        }),
    ),
});
