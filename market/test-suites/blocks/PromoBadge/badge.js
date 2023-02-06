import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ProductCardVendorPromoBadge} vendorTagBadge
 */
export default makeSuite('Описание.', {
    story: {
        'Всегда': {
            'содержит ожидаемую ссылку': makeCase({
                async test() {
                    const badgeText = this.vendorTagBadge.badgeDescription.getText();

                    await this.expect(badgeText)
                        .to.be.equal(this.params.badgeText);
                },
            }),
        },
    },
});
