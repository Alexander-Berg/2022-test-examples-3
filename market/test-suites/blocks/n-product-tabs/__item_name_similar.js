import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ProductTabs} productTabs
 */
export default makeSuite('Вкладка "Похожие товары"', {
    environment: 'kadavr',
    story: {
        'По клику': {
            'параметр "text" сохраняется': makeCase({
                async test() {
                    await this.browser.yaWaitForChangeUrl(() => this.productTabs.clickSimilar());

                    const {query} = await this.browser.yaParseUrl();

                    await this.expect(query, 'Должен быть параметр "text"')
                        .to.have.property('text');
                },
            }),
        },
    },
});
