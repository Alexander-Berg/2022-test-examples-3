import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Баннер "Дарим 500 баллов за заказ"', {
    story: {
        'По умолчанию': {
            'баннер не отображается': makeCase({
                test() {
                    return this.welcomeCashbackPromoBannerTouch.isExisting()
                        .should.eventually.to.be.equal(
                            false,
                            'Баннер "Дарим 500 баллов за заказ" не должен отображаться'
                        );
                },
            }),
        },
    },
});
