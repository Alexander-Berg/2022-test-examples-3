import {makeCase, makeSuite} from 'ginny';

/**
 * Тест формы жалобы на КМ
 *
 * @param {PageObject.ComplainProductButton} popup
 * @param {PageObject.PopupComplainForm} popupForm
 */
export default makeSuite('Форма обратной связи КМ', {
    story: {
        'Жалоба на КМ': {
            'По умолчанию': {
                'должна работать кнопка назад': makeCase({
                    id: 'marketfront-4022',
                    issue: 'MARKETPROJECT-3267',
                    async test() {
                        const {popup, popupForm} = this;

                        await popup.show();
                        await popupForm.selectProductComplainReason();
                        await popupForm.clickToComplainFillingFormButton();
                        await popupForm.clickBackFromComplainFillingFormButton();


                        return popupForm.isCauseListVisible()
                            .should
                            .eventually
                            .be
                            .equal(true, 'Выбор причины жалобы отобразился');
                    },
                }),
            },
            'При успешной отправке': {
                'должна открыться форма с благодарностью': makeCase({
                    id: 'marketfront-4023',
                    issue: 'MARKETPROJECT-3267',
                    async test() {
                        const {popup, popupForm} = this;

                        await popup.show();
                        await popupForm.selectProductComplainReason();
                        await popupForm.clickToComplainFillingFormButton();
                        await popupForm.submit();
                        await popupForm.waitForSuccessScreenVisible();

                        return popupForm.isSuccessScreenVisible()
                            .should
                            .eventually
                            .be
                            .equal(true, 'Форма с благодарностью отобразилась');
                    },
                }),
            },
        },
    },
});
