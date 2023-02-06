import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Параметры sku', {
    environment: 'kadavr',
    story: {
        'При переключении sku фильтра': {
            меняются: makeCase({
                id: 'marketfront-5133',
                issue: 'MARKETFRONT-23296',
                async test() {
                    const {skuState, selectedPickerIndex} = this.params;

                    await this.browser.setState('report', skuState);

                    await this.colorFilter.selectColor(selectedPickerIndex);

                    return this.browser.waitUntil(async () => this.browser
                        .yaCheckUrlParams({
                            glfilter: '14871214:15278711_100210863681',
                            sku: '100210863681',
                        })
                        .should.eventually.to.be.equal(true, 'Параметры присутствуют в урле'));
                },
            }),
        },
    },
});
