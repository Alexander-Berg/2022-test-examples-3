import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Вкладки на КМ', {
    environment: 'kadavr',
    story: {
        'При переходе': {
            'параметр sku сохраняется в url': makeCase({
                id: 'marketfront-5131',
                issue: 'MARKETFRONT-23296',
                async test() {
                    const {skuId, clickMethod} = this.params;
                    await this.linkPageObject[clickMethod]();

                    return this.browser.waitUntil(async () => this.browser
                        .yaCheckUrlParams({
                            sku: skuId,
                        })
                        .should.eventually.to.be.equal(true, 'Параметры присутствуют в урле'));
                },
            }),
        },
    },
});
