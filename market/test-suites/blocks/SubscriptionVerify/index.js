import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.SubscriptionVerify} subscriptionVerify
 */
export default makeSuite('Информация о проверке подписки.', {
    story: {
        'По умолчанию': {
            'должна отображаться': makeCase({
                test() {
                    return this.browser.allure.runStep('Проверям видимость блока', () =>
                        this.subscriptionVerify.isVisible()
                            .should.eventually.to.be.equal(true, 'Блок отображается')
                    );
                },
            }),
            'ссылка ведёт на карточку модели': makeCase({
                async test() {
                    const expectedUrl = await this.browser.yaBuildURL('touch:product', {
                        productId: this.params.productId,
                        slug: this.params.slug,
                    });

                    await this.subscriptionVerify.waitForProductLinkVisible();

                    const url = await this.browser.yaWaitForChangeUrl(
                        () => this.subscriptionVerify.clickProductLink()
                    );

                    return this.expect(url).to.be.link(
                        {pathname: expectedUrl},
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
