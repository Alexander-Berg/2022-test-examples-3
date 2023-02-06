import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Пукт меню "500 баллов за Плюс"', {
    story: {
        'По умолчанию': {
            'плашка отображается': makeCase({
                test() {
                    return this.browser.allure.runStep(
                        'Проверяем наличие плашки "500 баллов Плюс" в меню пользователя',
                        () => this.welcomeCashbackMenuItem.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Плашка "500 баллов Плюса" должна отображаться'
                            )
                    );
                },
            }),
            'текст заголовка содержит "500 баллов Плюса': makeCase({
                test() {
                    return this.browser.allure.runStep(
                        'Проверяем текст заголовка',
                        () => this.welcomeCashbackMenuItem.getPrimaryText()
                            .should.eventually.to.be.equal(
                                '500 баллов Плюса',
                                'Текст должен содержать "500 баллов Плюса"'
                            )
                    );
                },
            }),
        },
        'При нажатии': {
            'открывается попап с информацией об акции': makeCase({
                async test() {
                    await this.menuItemLink.click();
                    await this.welcomeCashbackPopup.waitForVisible(3000);
                    return this.welcomeCashbackPopup.isVisible()
                        .should.eventually.to.be.equal(true, 'Попап с информацией об акции должен отображаться');
                },
            }),
        },
    },
});
