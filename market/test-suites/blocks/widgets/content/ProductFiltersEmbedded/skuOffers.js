import {makeSuite, makeCase} from 'ginny';
import {isEmpty} from 'ambar';

/**
 * @property {PageObject.VisualEnumFilter} visualEnumFilter
 */
export default makeSuite('Фильтр офферов с sku', {
    story: {
        'КМ c параметром sku. Зайти во все фильтры, нажать "Сбросить фильтры"': {
            'Пользователь вернулся на модель и убран параметр sku': makeCase({
                id: 'm-touch-3581',
                issue: 'MARKETFRONT-25487',
                async test() {
                    await this.browser.allure.runStep('Открываем страницу с параметром SKU', () =>
                        this.browser.yaWaitForChangeUrl(() =>
                            this.browser.yaOpenPage(
                                'touch:product-filters',
                                Object.assign({}, this.params.initialPageUrl, {sku: 1})
                            )
                        )
                    );
                    await this.filters.reset();
                    await this.filters.waitForApplyButtonActive();

                    const changedUrl = await this.browser.yaWaitForChangeUrl(() => this.filters.apply());
                    const skuParamValue = changedUrl.match(/sku=([0-9]+)/);

                    return this.expect(isEmpty(skuParamValue)).to.be.equal(true, 'SKU параметр отсутствует в урле');
                },

            }),
        },
    },
});
