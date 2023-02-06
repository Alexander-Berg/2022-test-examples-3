import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.VendorProductLinePromo} vendorProductLinePromo
 */
export default makeSuite('Баннер.', {
    story: {
        'Всегда': {
            'содержит ожидаемую ссылку': makeCase({
                async test() {
                    const actualPath = await this.vendorProductLinePromo.getLink();

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
