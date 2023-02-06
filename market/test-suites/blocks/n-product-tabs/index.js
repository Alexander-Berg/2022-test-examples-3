import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ProductTabs} productTabs
 */
export default makeSuite('Вкладки на КМ', {
    environment: 'kadavr',
    story: {
        'Вкладка "Описание".': {
            'По клику': {
                'удаляются параметры fesh и grhow': makeCase({
                    async test() {
                        await this.browser.yaWaitForChangeUrl(() => this.productTabs.clickProduct());

                        const {query} = await this.browser.yaParseUrl();
                        await this.expect(query, 'Не должно быть параметра "fesh"')
                            .to.not.have.property('fesh');
                        await this.expect(query, 'Не должно быть параметра "grhow"')
                            .to.not.have.property('grhow');
                    },
                }),
            },
        },
    },
});
