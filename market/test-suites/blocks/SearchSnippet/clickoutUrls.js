import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.SearchSnippet} snippet
 * @param {PageObject.SearchSnippetClickoutButton} snippetClickoutButton
 * @param {PageObject.SearchSnippetShopInfo} snippetShopInfo
 */
export default makeSuite('Кликаут.', {
    environment: 'kadavr',
    story: {
        'Ссылка на карточке': {
            'должна быть корректной': makeCase({
                async test() {
                    const url = await this.snippet.getSnippetUrl();
                    return this.expect(url.path)
                        .to.equal(this.params.url, 'Кликаут ссылка корректна');
                },
            }),
        },
        'Ссылка на кнопке "В магазин"': {
            'должна быть корректной': makeCase({
                async test() {
                    const url = await this.snippetClickoutButton.getGoToShopButtonUrl();
                    return this.expect(url.path)
                        .to.equal(this.params.url, 'Кликаут ссылка корректна');
                },
            }),
        },
        'Ссылка на наименовании магазина': {
            'должна быть корректной': makeCase({
                async test() {
                    const url = await this.snippetShopInfo.getShopInfoLinkUrl();
                    return this.expect(url.path)
                        .to.equal(this.params.url, 'Кликаут ссылка корректна');
                },
            }),
        },
    },
});
