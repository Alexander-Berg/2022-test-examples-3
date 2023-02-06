import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на элемент __list-item.link блока footer-market.
 * @param {PageObject.FooterMarket} footerMarket
 */
export default makeSuite('Промо-кнопка.', {
    feature: 'Футер',
    issue: 'MARKETVERSTKA-28137',
    story: {
        'Apple App Store': {
            'должна иметь ссылку в аппстор': makeCase({
                id: 'marketfront-1176',
                async test() {
                    const href = await this.footerMarket.promoButtonByIndex(1).getAttribute('href');
                    return href.should.to.be.equal('https://mobile.yandex.ru/apps/iphone/market?from=market_footer#main');
                },
            }),
        },

        'Google Play Store': {
            'должна иметь ссылку в плей стор': makeCase({
                id: 'marketfront-1175',
                async test() {
                    const href = await this.footerMarket.promoButtonByIndex(2).getAttribute('href');
                    return href.should.to.be.equal('https://mobile.yandex.ru/apps/android/market?from=market_footer#main');
                },
            }),
        },
    },
});
