import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тултип "Получите 500 баллов Плюса.', {
    story: {
        'По умолчанию': {
            'тултип не отображается': makeCase({
                test() {
                    return this.welcomeCashbackOnboardingTooltip.isExisting()
                        .should.eventually.to.be.equal(false, 'Тултип не должен отображаться');
                },
            }),
        },
    },
});
