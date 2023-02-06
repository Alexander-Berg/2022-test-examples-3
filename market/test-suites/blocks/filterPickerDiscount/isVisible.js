import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Видимая минимальная цена на фильтре «Состояние товара»', {
    feature: 'состояние товара',
    story: {
        'Минимальная цена': {
            'если уценённый оффер дешевле нового': {
                'отображается': makeCase({
                    async test() {
                        const isVisible = await this.filterPickerDiscount.priceFrom.isVisible();

                        return this.expect(isVisible).to.be.equal(true, 'минимальная цена видна');
                    },
                }),
            },
        },
    },
});
