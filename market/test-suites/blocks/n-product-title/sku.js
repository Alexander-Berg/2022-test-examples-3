import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Заголовок sku модели', {
    environment: 'kadavr',
    story: {
        'При переключении sku фильтра': {
            'заголовок меняется': makeCase({
                id: 'marketfront-5132',
                issue: 'MARKETFRONT-23296',
                async test() {
                    if (this.params.skuState) {
                        await this.browser.setState('report', this.params.skuState);
                    }

                    await this.colorFilter.selectColor(this.params.selectedPickerIndex);

                    return this.browser.waitUntil(async () => {
                        const title = await this.productTitle.getHeaderTitleText();
                        return title === this.params.expectedTitle;
                    });
                },
            }),
        },
    },
});
