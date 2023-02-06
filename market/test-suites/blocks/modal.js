import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок modal.
 * @param {PageObject.Modal} modal
 */
export default makeSuite('Модальное окно.', {
    feature: 'Попап',
    story: {
        'При клике на кнопку "Х"': {
            'должно закрываться': makeCase({
                id: 'marketfront-943',
                test() {
                    return this.modal
                        .closeOnButton()
                        .then(() => this.modal.waitForClosed())
                        .should.eventually.be.equal(true, 'Попап корректно закрылся');
                },
            }),
        },

        'При нажатии на клавишу ESC': {
            'должно закрываться ': makeCase({
                id: 'marketfront-944',
                test() {
                    return this.modal
                        .closeOnEscape()
                        .then(() => this.modal.waitForClosed())
                        .should.eventually.be.equal(true, 'Попап корректно закрылся');
                },
            }),
        },

        'При клике вне области попапа': {
            'должно закрываться': makeCase({
                id: 'marketfront-942',
                async test() {
                    await this.modal.closeOnParanja();
                    return this.modal.waitForClosed()
                        .should.eventually.be.equal(true, 'Попап корректно закрылся');
                },
            }),
        },
    },
});
