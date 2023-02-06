import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на элемент link блока n-shop-info
 *
 * @param {PageObject.shopsInfo} shopsInfo
 */
export default makeSuite('Ссылка с информацией о продавцах', {
    environment: 'kadavr',
    story: {
        'Содержит': {
            'идентификаторы магазинов': makeCase({
                params: {
                    expectedIds: 'id продавцов, разделённые запятыми',
                },
                async test() {
                    const {query} = await this.shopsInfo.getLinkHref();
                    await this.expect(query.shopIds).to.be.equal(this.params.expectedIds);
                },
            }),
            'правильный текст': makeCase({
                params: {
                    expectedText: 'Текст ссылки',
                },
                async test() {
                    const linkText = await this.shopsInfo.getLinkText();
                    await this.expect(linkText).to.be.equal(this.params.expectedText);
                },
            }),
        },
    },
});
