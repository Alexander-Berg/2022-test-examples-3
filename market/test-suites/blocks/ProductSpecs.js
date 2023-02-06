import {makeSuite, makeCase} from 'ginny';
import ProductSpecsPreview from '@self/platform/widgets/content/ProductSpecsPreview/__pageObject';
import EmptyProductReviews from '@self/platform/widgets/content/EmptyProductReviews/__pageObject';

/**
 * Тесты на блок ProductSpecs
 * @param {PageObject.ProductSpecs} productSpecs
 */
export default makeSuite('Блок "Характеристики".', {
    feature: 'Карточка модели',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                test() {
                    return this.productSpecs.isExists()
                        .should.eventually.to.be.equal(true, 'Блок отображается');
                },
            }),

            'должен иметь правильный заголовок': makeCase({
                test() {
                    return this.productSpecs.title.getText()
                        .should.eventually.to.be.equal('Коротко о товаре', 'Заголовок правильный');
                },
            }),

            'должен содержать хотя бы одну характеристику': makeCase({
                test() {
                    return this.productSpecs.getItemsCount().should.eventually.be.greaterThan(0, 'Есть характеристики');
                },
            }),
        },

        'В групповой модели': {
            'должен содержать список характеристик': makeCase({
                test() {
                    return this.productSpecs.getSpecs().then(specs => (
                        this.expect(specs).not.to.be.empty
                    ));
                },
            }),
        },

        'При кол-ве характеристик больше 6': {
            'должен содержать ссылку "Все характеристики"': makeCase({
                test() {
                    return this.productSpecs.isSpecsLinkExists()
                        .should.eventually.to.be.equal(true, 'Есть ссылка на все характеристики');
                },
            }),


            'при клике на "Подробнее" будет скролл к блоку описания': makeCase({
                async test() {
                    await this.browser.yaWaitForPageReady();
                    await this.browser.waitForVisible(EmptyProductReviews.root, 5000);
                    return this.browser.waitUntil(
                        async () => {
                            await this.productSpecs.clickSpecsLink();
                            return this.browser.isVisibleWithinViewport(ProductSpecsPreview.scroll);
                        },
                        5000,
                        'Блок описание не виден во вьюпорте'
                    );
                },
            }),
        },
    },
});
