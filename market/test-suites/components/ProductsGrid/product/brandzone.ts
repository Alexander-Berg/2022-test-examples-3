'use strict';

import {makeCase, makeSuite, mergeSuites, importSuite} from 'ginny';

import showProductHook from './hooks/showProduct';

/**
 * Тесты на управление услугой Бренд-зона
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 */
export default makeSuite('Управление услугой Бренд-зона.', {
    id: 'vendor_auto-529',
    issue: 'VNDFRONT-4149',
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
                    hint() {
                        return this.createPageObject('ClickableLevitan', this.product.placement);
                    },
                    spinner() {
                        return this.createPageObject('SpinnerLevitan', this.product.placement);
                    },
                });

                await this.browser.allure.runStep('Дожидаемся загрузки списка катофов', () =>
                    this.browser.waitUntil(
                        async () => {
                            const visible = await this.spinner.isVisible();

                            return visible === false;
                        },
                        this.browser.options.waitforTimeout,
                        'Не удалось дождаться скрытия спиннера',
                    ),
                );
            },
        },
        importSuite('Hint', {
            meta: {
                environment: 'kadavr',
            },
            params: {
                text: 'Услуга будет активирована 22 января 2023.',
            },
        }),
        {
            'При наличии неактивной услуги Бренд-зона': {
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.setPageObjects({
                        modal() {
                            return this.createPageObject('ModalLevitan');
                        },
                        productsControls() {
                            return this.createPageObject('ProductsControls');
                        },
                        rechargeButton() {
                            return this.createPageObject(
                                'ButtonLevitan',
                                this.productsControls,
                                this.productsControls.rechargeButton,
                            );
                        },
                        productSelectField() {
                            return this.createPageObject('SelectLevitan', this.modal);
                        },
                    });
                },
                'нет возможности её запустить и есть возможность пополнить счёт': makeCase({
                    async test() {
                        await this.product.placement
                            .getText()
                            .should.eventually.be.equal(
                                'Неактивно\nЗапуск – 22 января 2023',
                                'Текст статуса услуги верный',
                            );

                        await this.product.setPlacementButton
                            .vndIsExisting()
                            .should.eventually.be.equal(false, 'Нет возможности запустить услугу');

                        await this.rechargeButton.click();

                        await this.modal.waitForOpened();

                        await this.browser.allure.runStep('Выбираем услугу Бренд-зона', async () => {
                            await this.productSelectField.click();
                            await this.productSelectField.waitForSelectShown();

                            return this.productSelectField.selectItem('Бренд-зона');
                        });

                        return this.productSelectField
                            .getText()
                            .should.eventually.be.equal(
                                'Бренд-зона',
                                'В модалке пополнения удалось выбрать услугу "Бренд-зона"',
                            );
                    },
                }),
            },
        },
    ),
});
