import {makeSuite, makeCase} from 'ginny';

const DEFAULT_TITLE_TEXT = 'Получите 500 баллов Плюса';

export default makeSuite('Тултип "Получите 500 баллов Плюса.', {
    story: {
        'По умолчанию': {
            'тултип отображается': makeCase({
                test() {
                    return this.welcomeCashbackOnboardingTooltip.isExisting()
                        .should.eventually.to.be.equal(true, 'Тултип должен отображаться');
                },
            }),
            'заголовк тултипа содержит корректный текст': makeCase({
                test() {
                    return this.tooltipTitle.getTitle()
                        .should.eventually.to.be.equal(
                            DEFAULT_TITLE_TEXT,
                            `Текст должен быть равен "${DEFAULT_TITLE_TEXT}"`
                        );
                },
            }),
        },
        'При клике на ссылку в тултипе': {
            'открывается попап с информацией об акции': makeCase({
                async test() {
                    await this.popupClickableElem.click();
                    await this.welcomeCashbackPopup.waitForVisible(3000);
                    return this.welcomeCashbackPopup.isVisible()
                        .should.eventually.to.be.equal(true, 'Попап с информацией об акции должен отображаться');
                },
            }),
        },
    },
});
