import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок b-popup-complain
 *
 * @param {PageObject.PopupComplain} popup
 * @param {PageObject.PopupComplainForm} popupForm
 */
export default makeSuite('Форма обратной связи', {
    params: {
        skipFirstScreen: 'Пропускать ли первый экран (для жалобы только на магазин)',
    },
    defaultParams: {
        skipFirstScreen: false,
    },
    story: {
        'Жалоба на оффер': {
            'По умолчанию': {
                'должна работать кнопка назад': makeCase({
                    id: 'marketfront-4024',
                    issue: 'MARKETPROJECT-3267',
                    async test() {
                        const {popup, popupForm} = this;

                        await popup.show();
                        await popupForm.selectOfferComplain();
                        await popupForm.selectOfferComplainReason();
                        await popupForm.clickToComplainFillingFormButton();
                        await popupForm.clickBackFromComplainFillingFormButton();
                        await popupForm.clickBackFromComplainChoosingCauseButton();

                        return popupForm.isIssueListVisible()
                            .should
                            .eventually
                            .be
                            .equal(true, 'Выбор предмета жалобы (оффер или магазин) отобразился');
                    },
                }),
            },
            'При успешной отправке': {
                'должна открыться форма с благодарностью': makeCase({
                    id: 'marketfront-2668',
                    issue: 'MARKETVERSTKA-30070',
                    async test() {
                        const {popup, popupForm} = this;

                        await popup.show();
                        await popupForm.selectOfferComplain();
                        await popupForm.selectOfferComplainReason();
                        await popupForm.clickToComplainFillingFormButton();
                        await popupForm.submit();
                        await popupForm.waitForSuccessScreenVisible();

                        return popupForm.isSuccessScreenVisible()
                            .should.eventually.be.equal(true, 'Форма с благодарностью отобразилась');
                    },
                }),
            },
        },

        'Жалоба на магазин': {
            async beforeEach() {
                const {popup, popupForm} = this;

                await popup.show();

                if (!this.params.skipFirstScreen) {
                    await popupForm.selectShopComplain();
                }

                await popupForm.selectShopComplainReason();
                await popupForm.clickToComplainFillingFormButton();
            },

            'При успешной отправке': {
                'должна открыться форма с благодарностью': makeCase({
                    id: 'marketfront-2669',
                    issue: 'MARKETVERSTKA-30070',
                    async test() {
                        const {popupForm} = this;

                        await popupForm.setComment('комментарий');
                        await popupForm.setName('Имя');
                        await popupForm.setEmail('my-mail@yandex.ru');
                        await popupForm.submit();
                        await popupForm.waitForSuccessScreenVisible();

                        return popupForm.isSuccessScreenVisible()
                            .should.eventually.be.equal(true, 'Форма с благодарностью отобразилась');
                    },
                }),
            },

            'При незаполненном поле с проблемой': {
                'должна отобразиться ошибка': makeCase({
                    id: 'marketfront-2670',
                    issue: 'MARKETVERSTKA-30070',
                    async test() {
                        const {popupForm} = this;

                        await popupForm.setComment('');
                        await popupForm.setName('Имя');
                        await popupForm.setEmail('my-mail@yandex.ru');
                        await popupForm.submit();

                        return popupForm.hasError('Введите текст проблемы')
                            .should.eventually.be.equal(true, 'Ошибка отобразилась');
                    },
                }),
            },

            'При незаполненном поле с именем': {
                'должна отобразиться ошибка': makeCase({
                    id: 'marketfront-2671',
                    issue: 'MARKETVERSTKA-30070',
                    async test() {
                        const {popupForm} = this;

                        await popupForm.setComment('Комментарий');
                        await popupForm.setName('');
                        await popupForm.setEmail('my-mail@yandex.ru');
                        await popupForm.submit();

                        return popupForm.hasError('Введите ваше имя')
                            .should.eventually.be.equal(true, 'Ошибка отобразилась');
                    },
                }),
            },

            'При незаполненном поле с почтой': {
                'должна отобразиться ошибка': makeCase({
                    id: 'marketfront-2672',
                    issue: 'MARKETVERSTKA-30070',
                    async test() {
                        const {popupForm} = this;

                        await popupForm.setComment('Комментарий');
                        await popupForm.setName('Имя');
                        await popupForm.setEmail('');
                        await popupForm.submit();

                        return popupForm.hasError('Введите электронный адрес')
                            .should.eventually.be.equal(true, 'Ошибка отобразилась');
                    },
                }),
            },

            'При невалидном электронном адресе': {
                'должна отобразиться ошибка': makeCase({
                    id: 'marketfront-2673',
                    issue: 'MARKETVERSTKA-30070',
                    async test() {
                        const {popupForm} = this;

                        await popupForm.setComment('Комментарий');
                        await popupForm.setName('Имя');
                        await popupForm.setEmail('sdadwijWDW@@@');
                        await popupForm.submit();

                        return popupForm.hasError('Неверный формат электронного адреса')
                            .should.eventually.be.equal(true, 'Ошибка отобразилась');
                    },
                }),
            },
        },
    },
});
