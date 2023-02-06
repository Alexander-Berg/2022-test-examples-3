import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Скрытая минимальная цена на фильтре «Состояние товара»', {
    feature: 'состояние товара',
    story: {
        'Минимальная цена': {
            'если уценённый оффер дороже нового': {
                'не отображается': makeCase({
                    async test() {
                        const isExists = await this.filterPickerDiscount.isPriceFromExists();

                        return this.expect(isExists).to.be.equal(false, 'минимальная цена скрыта');
                    },
                }),
            },
        },
    },
});
