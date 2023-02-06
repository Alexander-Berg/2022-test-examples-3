import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Промо баннер "Дарим 500 баллов"', {
    story: {
        'По умолчанию': {
            'баннер не должен отображаться': makeCase({
                test() {
                    return this.welcomeCashbackPromoBanner.isExisting()
                        .should.eventually.to.be.equal(
                            false,
                            'Баннер "Дарим 500 баллов" не должен отображаться!'
                        );
                },
            }),
        },
    },
});
