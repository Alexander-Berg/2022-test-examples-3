import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.offerDefaultShopLink} offerDefaultShopLink
 */

export default makeSuite('Ссылка на магазин в дефолтном оффере', {
    story: {
        'По умолчанию': {
            'должна открываться в новом окне': makeCase({
                feature: 'Дефолтный оффер',
                id: 'm-touch-1531',
                issue: 'MOBMARKET-5912',
                async test() {
                    await this.browser.allure.runStep(
                        'Проверяем, что ссылка открывается в новом окне',
                        () => this.offerDefaultShopLink
                            .getTarget()
                            .should.eventually.equal('_blank', 'Ссылка открывается в новом окне')
                    );

                    return this.browser.allure.runStep(
                        'Проверяем, что ссылка ведёт на магазин',
                        () => this.offerDefaultShopLink
                            .getHref()
                            .should.eventually.be.link({
                                pathname: 'shop(--(.*))?\\/\\d+',
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            })
                    );
                },
            }),
        },
    },
});
