import {ReturnItems} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItems/__pageObject';

const {makeCase, makeSuite} = require('ginny');

module.exports = makeSuite('Возврат товара', {
    story: {
        beforeEach() {
            this.setPageObjects({
                returnItemsScreen: () => this.createPageObject(ReturnItems),
            });
        },
        'Корректно отображается содержимое странички возврата.': makeCase({
            async test() {
                await this.allure.runStep('Отображается кнопка "Вернуть заказ"', async () => {
                    const isExist = await this.orderCard.isExisting(this.orderCard.returnButton);
                    return this.expect(isExist).to.be.equal(true, 'Кнопка "Вернуть заказ" должна отображаться');
                });

                await this.allure.runStep('Кнопка "Вернуть заказ" по клику открывает страницу возврата"',
                    async () => {
                        await this.browser.yaWaitForChangeUrl(() => this.orderCard.clickReturnButton(), 20000);

                        return this.browser.getUrl()
                            .should.eventually.to.be
                            .link({
                                pathname: '/my/returns/create',
                                query: {
                                    orderId: String(this.params.orderId),
                                    type: 'refund',
                                },
                            }, {
                                skipHostname: true,
                                skipProtocol: true,
                            });
                    });

                await this.returnItemsScreen.isVisible()
                    .should.eventually.to.equal(
                        true,
                        'Форма для выбора товаров и причин для возврата должна отображаться '
                    );
            },
        }),
    },
});
