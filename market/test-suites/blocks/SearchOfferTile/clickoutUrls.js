import {makeCase, makeSuite} from 'ginny';

export default makeSuite('Кликаут.', {
    environment: 'kadavr',
    story: {
        'Ссылка на карточке': {
            'должна быть корректной': makeCase({
                async test() {
                    const url = await this.searchOfferTile.getTileLinkUrl();
                    return this.expect(url.path)
                        .to.equal(this.params.url, 'Кликаут ссылка корректна');
                },
            }),
        },
        'Ссылка на кнопке "В магазин"': {
            'должна быть корректной': makeCase({
                async test() {
                    const url = await this.searchOfferTile.getGoToShopButtonUrl();
                    return this.expect(url.path)
                        .to.equal(this.params.url, 'Кликаут ссылка корректна');
                },
            }),
        },
        'Ссылка на наименовании магазина': {
            'должна быть корректной': makeCase({
                async test() {
                    const url = await this.searchOfferTile.getShopNameLinkUrl();
                    return this.expect(url.path)
                        .to.equal(this.params.url, 'Кликаут ссылка корректна');
                },
            }),
        },
    },
});
