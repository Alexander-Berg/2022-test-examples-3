import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.CategorySnippet} categorySnippet
 * @param {PageObject.ProductCardLinks} productCardLinks
 */
export default makeSuite('Блок тематик', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'отображается.': makeCase({
                id: 'm-touch-3947',
                issue: 'MARKETFRONT-72241',
                async test() {
                    await this.browser.yaDelay(6000);
                    return this.categorySnippet.isVisible()
                        .should.eventually.to.be.equal(true, 'Блок отображается');
                },
            }),
            'состоит из 6 элементов': makeCase({
                id: 'm-touch-3947',
                issue: 'MARKETFRONT-72241',
                async test() {
                    return this.categorySnippet.getItemsCount()
                        .should.eventually.to.be.equal(this.params.expectedItemsCount);
                },

            }),
        },
        'Отображается при переходе на страницу': {
            'отзывов': makeCase({
                id: 'm-touch-3949',
                issue: 'MARKETFRONT-72241',
                async test() {
                    await this.productCardLinks.clickOnInfoLink('reviewCardLink');
                    await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                    return this.categorySnippet.isVisible()
                        .should.eventually.to.be.equal(true, 'Блок отображается');
                },
            }),
            'характеристик': makeCase({
                id: 'm-touch-3949',
                issue: 'MARKETFRONT-72241',
                async test() {
                    await this.productCardLinks.clickOnInfoLink('specsCardLink');
                    await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                    return this.categorySnippet.isVisible()
                        .should.eventually.to.be.equal(true, 'Блок отображается');
                },
            }),
            'предложений': makeCase({
                id: 'm-touch-3949',
                issue: 'MARKETFRONT-72241',
                async test() {
                    await this.productPage.showMoreOffersKMLink.click();
                    await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                    return this.categorySnippet.isVisible()
                        .should.eventually.to.be.equal(true, 'Блок отображается');
                },
            }),
        },
        'Заголовок выдачи совпадает с заголовком гридовой тематики при переходе по ссылке': {
            'id': 'm-touch-3948',
            'issue': 'MARKETFRONT-72241',
            'смотреть все': makeCase({
                async test() {
                    const expectedTitle = await this.widgetWrapper.title.getText();

                    await this.widgetWrapper.link.click();
                    return this.searchHeaderRedesigned.title.getText()
                        .should.eventually.to.be.equal(expectedTitle);
                },
            }),
            'тематики': makeCase({
                async test() {
                    const expectedText = await this.categorySnippet.getItemByIndex(1).getText();

                    await this.categorySnippet.getItemByIndex(1).click();
                    return this.searchHeaderRedesigned.title.getText()
                        .should.eventually.to.be.equal(expectedText);
                },
            }),
        },
    },
});
