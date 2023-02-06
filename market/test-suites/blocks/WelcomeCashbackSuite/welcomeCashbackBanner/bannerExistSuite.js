import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Промо баннер "Дарим 500 баллов"', {
    story: {
        'По умолчанию': {
            'баннер должен отображаться': makeCase({
                test() {
                    return this.welcomeCashbackPromoBanner.isExisting()
                        .should.eventually.to.be.equal(
                            true,
                            'Баннер "Дарим 500 баллов" должен отображаться!'
                        );
                },
            }),
        },
        'При нажатии на кнопку': {
            'должен открываться попап с информацией об акции': makeCase({
                async test() {
                    await this.infoBannerButton.click();
                    await this.welcomeCashbackPopup.waitForVisible(3000);
                    return this.welcomeCashbackPopup.isVisible()
                        .should.eventually.to.be.equal(true, 'Попап с информацией об акции должен отображаться');
                },
            }),
        },
    },
});
