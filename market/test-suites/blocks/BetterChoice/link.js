import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.BetterChoice} betterChoice
 */
export default makeSuite('Сниппет.', {
    story: {
        'Всегда': {
            'содержит ожидаемую ссылку': makeCase({
                async test() {
                    const expectedPath = await this.browser.yaBuildURL(
                        this.params.routeName,
                        this.params.routeParams
                    );
                    const actualPath = await this.betterChoice.getLink();

                    await this.expect(actualPath)
                        .to.be.link(
                            expectedPath,
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
