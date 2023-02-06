'use strict';

import url from 'url';

import {mergeSuites, importSuite, makeSuite} from 'ginny';

import showProductHook from './hooks/showProduct';

/**
 * Тесты на детальную информацию по услуге
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.balanceOrderUrl - ссылка на заказ в Балансе
 */
export default makeSuite('Детали услуги.', {
    issue: 'VNDFRONT-3346',
    feature: 'Управление услугами и пользователями',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(
        showProductHook({
            managerView: true,
            details: true,
        }),
        importSuite('Link', {
            suiteName: 'Ссылка "Заказ в Балансе"',
            meta: {
                id: 'vendor_auto-815',
                environment: 'kadavr',
                feature: 'Управление балансом',
            },
            params: {
                caption: 'Заказ в Балансе',
                target: '_blank',
                external: true,
                comparison: {
                    skipQuery: true,
                },
            },
            pageObjects: {
                column() {
                    return this.createPageObject(
                        'ProductColumn',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.managerView,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.managerView.getColumnByIndex(0),
                    );
                },
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.column, this.column.balanceOrderLink);
                },
            },
            hooks: {
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {balanceOrderUrl} = this.params;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.url = balanceOrderUrl;

                    /**
                     * Костыль с пробросом регулярных выражений в query-параметры в buildUrl не удался,
                     * поэтому проверяем матчинг параметров отдельным шагом.
                     */
                    const parsedUrl = url.parse(balanceOrderUrl, true, true);

                    parsedUrl.query = {
                        ...parsedUrl.query,
                        service_order_id: '[0-9]+',
                    };

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.allure.runStep('Проверяем query-параметр serviceId', () =>
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
        }),
        importSuite('Hint', {
            suiteName: 'Подсказка к ID контракта.',
            meta: {
                id: 'vendor_auto-816',
                issue: 'VNDFRONT-3346',
                environment: 'kadavr',
                feature: 'Управление балансом',
            },
            params: {
                text:
                    'Откройте страницу договора в Яндекс.Балансе и скопируйте из ссылки набор цифр после ' +
                    '«...?contract_id=». Обратите внимание, что при заключении дополнительного соглашения ' +
                    'идентификатор договора изменится.',
            },
            pageObjects: {
                column() {
                    return this.createPageObject(
                        'ProductColumn',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.managerView,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.managerView.getColumnByIndex(1),
                    );
                },
                plainData() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('ProductPlainData', this.column, this.column.getPlainDataByIndex(1));
                },
                hint() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Hint', this.plainData);
                },
            },
        }),
    ),
});
