import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок n-phone.
 * @param {PageObject.Phone} phone
 */
export default makeSuite('Номер телефона.', {
    story: {
        'По-умолчанию': {
            'должен показывать надпись "ПОКАЗАТЬ ТЕЛЕФОН"': makeCase({
                id: 'marketfront-951',
                feature: 'Сниппет ко/км',
                test() {
                    return this.phone
                        .getValue()
                        .should.eventually.to.be.phone('ПОКАЗАТЬ ТЕЛЕФОН');
                },
            }),
        },

        'При клике': {
            'должен отображаться корректно': makeCase({
                id: 'marketfront-964',
                feature: 'Попапи',
                test() {
                    return this.phone
                        .show()
                        .then(() => this.phone.getValue())
                        .should.eventually.to.be.phone();
                },
            }),
        },
    },
});
