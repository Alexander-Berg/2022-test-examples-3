import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на элемент __list-item.link блока footer-market.
 * @param {PageObject.FooterMarket} footerMarket
 */
export default makeSuite('Ссылка в футере', {
    feature: 'Футер',
    story: {
        'по умолчанию': {
            'должна совпадать с переданной ссылкой': makeCase({
                params: {
                    expectedLink: 'href ссылки',
                    expectedLinkText: 'Название ссылки',
                },
                async test() {
                    const {expectedLink, expectedLinkText} = this.params;
                    const link = await this.footerMarket.getFooterListLinkByText(expectedLinkText);
                    return link.href.should.to.be.equal(expectedLink);
                },
            }),
        },
    },
});
