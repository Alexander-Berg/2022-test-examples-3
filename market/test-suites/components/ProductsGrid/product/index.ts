'use strict';

import url from 'url';

import {makeCase, makeSuite, mergeSuites, importSuite} from 'ginny';

import showProductHook from './hooks/showProduct';

/**
 * Тесты на блок услуги
 * @param {PageObject.Products} products - список услуг
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - заголовок блока
 * @param {boolean} params.isManager - флаг наличия роли менеджера (из userStory)
 * @param {string} [params.spendingUrl] - ссылка на страницу расходов
 * @param {string} [params.titleUrl] - ссылка на страницу услуг в заголовке
 * @param {string} [params.invoiceUrl] - ссылка на страницу "Счета и акты"
 */
export default makeSuite('Услуга.', {
    issue: 'VNDFRONT-2774',
    environment: 'testing',
    feature: 'Статистика',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(
        showProductHook(),
        {
            'Детали услуги': {
                'отображаются только менеджеру': makeCase({
                    id: 'vendor_auto-807',
                    issue: 'VNDFRONT-3339',
                    environment: 'kadavr',
                    test() {
                        this.setPageObjects({
                            details() {
                                return this.createPageObject('ProductDetails', this.product);
                            },
                        });

                        if (this.params.isManager) {
                            return this.details
                                .isExisting()
                                .should.eventually.be.equal(true, 'Детали услуги отображаются');
                        }

                        return this.details.isExisting().should.eventually.be.equal(false, 'Детали услуги скрыты');
                    },
                }),
            },
        },
        importSuite('Link', {
            suiteName: 'Ссылка в заголовке',
            meta: {
                issue: 'VNDFRONT-2091',
                feature: 'Управление услугами и пользователями',
                id: 'vendor_auto-516',
            },
            params: {
                comparison: {
                    skipHostname: true,
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {titleUrl, productName} = this.params;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.exist = Boolean(titleUrl);
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.url = titleUrl;
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.caption = productName;
                },
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.product, this.product.title);
                },
            },
        }),
        importSuite('Link', {
            suiteName: 'Ссылка "Расходы"',
            meta: {
                feature: 'Статистика',
            },
            params: {
                caption: 'Расходы',
                comparison: {
                    skipHostname: true,
                },
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.product, this.product.statisticsLink);
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {spendingUrl} = this.params;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.exist = Boolean(spendingUrl);
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.url = spendingUrl;
                },
            },
        }),
        importSuite('Link', {
            suiteName: 'Ссылка "Счета и акты"',
            meta: {
                id: 'vendor_auto-530',
                issue: 'VNDFRONT-2774',
                feature: 'Управление услугами и пользователями',
            },
            params: {
                caption: 'Счета и акты',
                target: '_blank',
                external: true,
                comparison: {
                    skipHostname: true,
                    skipQuery: true,
                },
            },
            hooks: {
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {invoiceUrl} = this.params;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.exist = Boolean(invoiceUrl);
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.url = invoiceUrl;

                    /**
                     * Проверяем query-параметры вручную, чтобы не костылить под исключительный кейс общий Link-сьют.
                     * Это необходимо, чтобы не завязываться на конкретный orderId и проверить этот параметр регуляркой.
                     * Напрямую регулярку в сьют не передать, потому что expected url формируется при помощи buildUrl,
                     * в который нельзя передавать регулярки (он просто вырежет некоторые символы).
                     * В общие параметры сравнения для кейса передаём skipQuery: true.
                     */
                    const parsedUrl = url.parse(invoiceUrl, true, true);

                    parsedUrl.query = {
                        ...parsedUrl.query,
                        service_order_id: '[0-9]+',
                    };

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.allure.runStep('Проверяем query-параметры orderId и serviceId', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.link.getUrl().should.eventually.be.link(parsedUrl, {
                            skipHostname: true,
                            skipProtocol: true,
                            skipPathname: true,
                            mode: 'match',
                        }),
                    );
                },
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.product, this.product.invoicesLink);
                },
            },
        }),
    ),
});
