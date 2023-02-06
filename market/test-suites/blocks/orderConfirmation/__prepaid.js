import {
    makeSuite,
    makeCase,
} from 'ginny';

/**
 * Тесты на успешное оформление предоплатного заказа
 * @param {PageObject.OrderConfirmation} orderConfirmation
 */
// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Предоплатный заказ успешно оформлен.', {
    environment: 'testing',
    story: {
        'Ссылка на чек': {
            'по умолчанию': {
                'отображается, по клику открывается новая вкладка с pdf-файлом': makeCase({
                    feature: 'Чеки',
                    id: 'bluemarket-516',
                    issue: 'bluemarket-462',
                    test() {
                        const {browser} = this;
                        const RECEIPT_FILE_PATH_REGEXP = /\/receipts\/income_receipt_[0-9]+_[0-9]+\.pdf$/;

                        return this.orderConfirmation.receiptLink
                            .isVisible()
                            .should.eventually.to.be.equal(true, 'Ссылка на чек должна быть отображена')
                            .then(() => this.browser.getTabIds())
                            .then(tabIds => Promise.all([
                                browser.yaWaitForNewTab({startTabIds: tabIds}),
                                () => this.orderConfirmation.receiptClick(),
                            ]))
                            .then(([newTabId]) => this.allure.runStep(
                                'Переключаемся на новую вкладку и проверяем URL',
                                () => (
                                    browser
                                        .switchTab(newTabId)
                                        // ссылка на чек ведет на страницу редректа
                                        // поэтому смотрим урл после редиректа
                                        .then(() => browser.yaWaitForChangeUrl())
                                        // адрес мог измениться быстрее, чем дошли до этой команды, попадем в таймаут
                                        // продолжаем, чтобы не обваливать тест, главная проверка - последняя
                                        .catch(() => null)
                                        .then(() => this.browser.getUrl())
                                        .should.eventually.be.link({
                                            pathname: RECEIPT_FILE_PATH_REGEXP,
                                        }, {
                                            mode: 'match',
                                            skipProtocol: true,
                                            skipHostname: true,
                                        })
                                )
                            ));
                    },
                }),
            },
        },

        'Состав кнопок': {
            'по умолчанию': {
                'отображается правильный состав': makeCase({
                    id: 'bluemarket-512',
                    issue: 'bluemarket-460',
                    feature: 'Спасибо за заказ',
                    test() {
                        const checkVisibility = (control, title) => this.orderConfirmation.isVisible(control)
                            .should.eventually.to.be.equal(true, `Ссылка "${title}" должна быть отображена`);

                        return checkVisibility(
                            this.orderConfirmation.detailsLink, 'Подробности'
                        )
                            .then(() => checkVisibility(
                                this.orderConfirmation.trackLink, 'Отследить'
                            ))
                            .then(() => checkVisibility(
                                this.orderConfirmation.receiptLink, 'Посмотреть чек'
                            ))
                            .then(() => checkVisibility(
                                this.orderConfirmation.continueShoppingButton, 'Продолжить покупки'
                            ));
                    },
                }),
            },
        },
    },
});
