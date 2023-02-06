import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.UnsubscribeReasons} unsubscribeReasons
 */
export default makeSuite('Форма причин отписки.', {
    story: {
        'По умолчанию': {
            'должна отображаться': makeCase({
                test() {
                    return this.browser.allure.runStep('Проверям видимость блока', () =>
                        this.unsubscribeReasons.isVisible()
                            .should.eventually.to.be.equal(true, 'Блок отображается')
                    );
                },
            }),
        },

        'При выборе причин и сабмите формы': {
            'отображается благодарственное сообщение': makeCase({
                async test() {
                    await this.unsubscribeReasons.clickCheckbox();

                    await this.unsubscribeReasons.setCustomReason('Какая-то другая причина');

                    await this.unsubscribeReasons.clickSubmitButton();

                    return this.unsubscribeReasons.waitForThanksMessageVisible();
                },
            }),
        },
    },
});
