'use strict';

/**
 * Хук проверки появления блока услуги
 * @param {Object} options
 * @param {boolean} [options.details] - флаг проверки блока деталей услуги
 * @param {boolean} [options.managerView] - флаг добавления PO ManagerView
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 */
export default (options = {}) => ({
    async beforeEach() {
        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        const {productKey, productName} = this.params;

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        this.setPageObjects({
            product() {
                return this.createPageObject('Product', this.products, this.products.getItemByProductKey(productKey));
            },
        });

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        await this.browser.allure.runStep('Ожидаем появления списка услуг', () =>
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.products.waitForExist(),
        );

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        await this.browser.allure.runStep(
            `Ожидаем появления блока услуги "${productName}"`,
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            () => this.product.waitForExist(),
        );

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        const {details, managerView} = options;

        if (details || managerView) {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.setPageObjects({
                details() {
                    return this.createPageObject('ProductDetails', this.product);
                },
            });

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.browser.allure.runStep('Ожидаем появления деталей услуги', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.details.waitForExist(),
            );

            if (managerView) {
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.setPageObjects({
                    managerView() {
                        return this.createPageObject('ProductManagerView', this.details);
                    },
                });
            }
        }
    },
});
