import {makeCase, makeSuite} from 'ginny';

export default makeSuite('Блок "Спасибо за отзыв"', {
    story: {
        'При отсутствии параметра gradeId': {
            'не должен отображаться': makeCase({
                id: 'm-touch-2114',
                issue: 'MOBMARKET-8132',
                test() {
                    const {allure} = this.browser;

                    return allure.runStep('Проверяем, что блок отсутствует на странице', () =>
                        this.reviewModerationNotice
                            .isExisting()
                            .should.eventually.to.equal(false, 'Блок не отображается')
                    );
                },
            }),
        },
    },
});
