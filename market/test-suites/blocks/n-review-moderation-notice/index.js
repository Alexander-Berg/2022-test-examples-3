import {makeCase, makeSuite} from 'ginny';

export default makeSuite('Блок "Спасибо за отзыв" после оставления отзыва', {
    story: {
        'по умолчанию': {
            'должен отображаться': makeCase({
                id: 'm-touch-2112',
                issue: 'MOBMARKET-8130',
                test() {
                    const {allure} = this.browser;

                    return allure.runStep('Проверяем, что блок присутствует на странице', () =>
                        this.reviewModerationNotice
                            .isVisible()
                            .should.eventually.to.equal(true, 'Блок отображается')
                    );
                },
            }),
        },

        'по клику на крестик': {
            'должен закрываться': makeCase({
                id: 'm-touch-2113',
                issue: 'MOBMARKET-8131',
                test() {
                    const {allure} = this.browser;

                    return allure.runStep('Проверяем, что блок скрывается по клику на крестик', () =>
                        this.reviewModerationNotice
                            .closeBlock()
                            .then(() => this.reviewModerationNotice.isExisting())
                            .should.eventually.to.equal(false, 'Блок не отображается')
                    );
                },
            }),
        },
    },
});
