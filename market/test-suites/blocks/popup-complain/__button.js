import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на видимость блока b-popup-complain.
 * @param {PageObject.PopupComplain} complainPopup
 * @param {PageObject.PopupComplainForm} complainPopupForm
 */
export default makeSuite('Кнопка "Пожаловаться"', {
    story: {
        'При клике': {
            'должен появиться попап жалобы': makeCase({
                async test() {
                    // yate fallback
                    if (this.complainPopup) {
                        await this.complainPopup.button.click();
                    }

                    if (this.complainButton) {
                        await this.complainButton.show();
                    }

                    const isVisible = await this.complainPopupForm.isVisible();

                    await this.expect(isVisible).to.be.equal(true, 'Есть попап жалобы');
                },
            }),
        },
    },
});
