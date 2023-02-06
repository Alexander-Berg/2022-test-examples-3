import {makeCase, makeSuite} from 'ginny';

export default makeSuite('Большой блок топ 6 (на странице отзывов)', {
    environment: 'kadavr',
    story: {
        'Кнопка "В магазин"': {
            'Имеет корректную ссылку': makeCase({
                async test() {
                    const url = await this.topOfferActions.getClickoutButtonUrl();
                    return this.expect(url.path)
                        .to.equal(this.params.url, 'Кликаут ссылка корректна');
                },
            }),
        },
        'Название магазина': {
            'Имеет корректную ссылку': makeCase({
                async test() {
                    const url = await this.shopName.getShopLinkUrl();
                    return this.expect(url.path)
                        .to.equal(this.params.url, 'Кликаут ссылка корректна');
                },
            }),
        },
    },
});
