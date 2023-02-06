import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Быстрые фильтры', {
    environment: 'kadavr',
    story: {
        'Кнопка сброса': {
            отсутствует: makeCase({
                id: 'marketfront-5130',
                issue: 'MARKETFRONT-23296',
                async test() {
                    return this.colorFilter.resetButton.isVisible().catch(() => false)
                        .should.eventually.be.equal(false, 'Кнопка сброса фильтров не должна отображаться');
                },
            }),

        },

        'При выборе sku фильтра': {
            'уходит правильный запрос в репорт': makeCase({
                id: 'marketfront-5134',
                issue: 'MARKETFRONT-23296',
                async test() {
                    const {selectedPickerIndex, skuId, skuState} = this.params;

                    if (skuState) {
                        await this.browser.setState('report', skuState);
                    }

                    await this.colorFilter.selectColor(selectedPickerIndex);
                    await this.browser.waitUntil(
                        async () => this.recommendedOffers.waitForLoaded(),
                        5000,
                        'загрузился новый ДО'
                    );

                    const requests = await this.browser.yaGetKadavrLogByBackendMethod('Report', 'search');

                    const filteredRequests = requests
                        .map(request => request.request.url)
                        .filter(url => /place=productoffers/.test(url));

                    const isSkuChanged = filteredRequests[filteredRequests.length - 1].includes(`sku%253D${skuId}`);

                    await this.expect(isSkuChanged)
                        .to.be.equal(true, 'В репорт уходит новый sku параметр');
                },
            }),
        },
    },
});
