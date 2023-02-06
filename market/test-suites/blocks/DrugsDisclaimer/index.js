import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на виджет DrugsDisclaimer.
 * @property {PageObject.DrugsDisclaimer} drugsDisclaimer
 */
export default makeSuite('Юридический дисклеймер о покупке лекарственных средств.', {
    feature: 'Здоровье',
    story: {
        'По умолчанию': {
            'должен присутствовать.': makeCase({
                test() {
                    return this.drugsDisclaimer.isVisible()
                        .should.eventually.to.be.equal(true,
                            'Дисклеймер присутствует.');
                },
            }),
        },
    },
});
