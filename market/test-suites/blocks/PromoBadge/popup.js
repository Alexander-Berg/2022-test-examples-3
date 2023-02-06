import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.VendorTagBadge} vendorTagBadge
 */
export default makeSuite('Попап.', {
    story: {
        'Всегда': {
            'содержит ожидаемую ссылку': makeCase({
                async test() {
                    await this.vendorTagBadge.openPopup();

                    const actualPath = await this.vendorTagBadge.getLink();

                    await this.expect(actualPath)
                        .to.be.link(
                            this.params.link,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );
                },
            }),
        },
    },
});
