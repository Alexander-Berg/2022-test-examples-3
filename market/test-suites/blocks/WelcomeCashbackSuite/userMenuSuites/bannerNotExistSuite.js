import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Пукт меню "500 баллов за Плюс"', {
    story: {
        'По умолчанию': {
            'плашка не отображается': makeCase({
                test() {
                    return this.browser.allure.runStep('Проверяем наличие плашки "500 баллов Плюс" в меню пользователя',
                        () => this.welcomeCashbackMenuItem.isVisible()
                            .should.eventually.to.be.equal(
                                false,
                                'Плашка "500 баллов Плюса" не должна отображаться'
                            )
                    );
                },
            }),
        },
    },
});
