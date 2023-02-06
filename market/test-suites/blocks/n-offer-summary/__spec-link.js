import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.OfferSummary} offerSummary
 */
export default makeSuite('Ссылка "Все характеристики"', {
    story: {
        'По умолчанию': {
            'кликабельна': makeCase({
                async test() {
                    return this.expect(this.offerSpecs.getSpecLinkHref())
                        .not.to.be.equal(null, 'Ссылка некликабельна');
                },
            }),
        },
    },
});
