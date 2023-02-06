import {makeCase, makeSuite} from 'ginny';

export default makeSuite('Футер на панели блока карты.', {
    story: {
        'Ссылка «информация о продавцах»': {
            'По умолчанию': {
                'должна отображаться': makeCase({
                    id: 'marketfront-1110',
                    issue: 'MARKETVERSTKA-25319',
                    async test() {
                        const isVisible = await this.geo.hasShopInfoLink();

                        return this.expect(isVisible).to.be.equal(true, 'Ссылка отображается');
                    },
                }),
            },
        },
    },
});
