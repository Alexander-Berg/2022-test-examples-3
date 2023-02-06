import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Гео сниппет. Кликаут.', {
    id: 'marketfront-4205',
    issue: 'MARKETFRONT-25074',
    story: {
        'Название магазина': {
            'Имеет корректную ссылку': makeCase({
                async test() {
                    await this.geoSnippet.waitForVisible();
                    const url = await this.shopName.getShopLinkUrl();
                    return this.expect(url.path)
                        .to.equal(this.params.url, 'Кликаут ссылка корректна');
                },
            }),
        },
        'Кнопка "В магазин"': {
            'Имеет корректную ссылку': makeCase({
                async test() {
                    await this.geoSnippet.waitForVisible();
                    const url = await this.geoSnippet.getClickoutButtonUrl();
                    return this.expect(url.path)
                        .to.equal(this.params.url, 'Кликаут ссылка корректна');
                },
            }),
        },
    },
});
