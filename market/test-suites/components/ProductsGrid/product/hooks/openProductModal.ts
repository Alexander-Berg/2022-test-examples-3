'use strict';

/**
 * Хук открытия модального окна подключения/изменения услуги
 */
export default () => ({
    async beforeEach() {
        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        const {productKey, productName, pageRouteName, vendor} = this.params;

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        this.setPageObjects({
            form() {
                return this.createPageObject('Form', this.modal);
            },
            modalTitle() {
                return this.createPageObject('TitleB2b', this.modal);
            },
        });

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        await this.browser.allure.runStep('Ожидаем появления списка услуг', () =>
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.products.waitForExist(),
        );

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        this.setPageObjects({
            product() {
                return this.createPageObject('Product', this.products, this.products.getItemByProductKey(productKey));
            },
        });

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        await this.browser.allure.runStep(
            `Ожидаем появления блока услуги "${productName}"`,
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            () => this.product.waitForExist(),
        );

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        await this.browser.vndOpenPage(pageRouteName, {
            vendor,
            editing: productKey,
        });

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        await this.browser.allure.runStep('Ожидаем появления модального окна', () =>
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.modal.waitForExist(),
        );

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        await this.modalTitle.getText().should.eventually.be.equal(productName, 'Текст заголовка корректный');

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        await this.browser.allure.runStep('Ожидаем появления формы редактирования данных услуги', () =>
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.form.waitForExist(),
        );
    },
});
