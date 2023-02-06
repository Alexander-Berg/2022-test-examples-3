const pUid = '&passport_uid=user_variant_test';
const additionalParams = '&mock=1';

hermione.only.notIn(['linux-chrome-ipad', 'linux-firefox'], 'нет такой функциональности');

describe('Попап добавления/редактирования пользовательского варианта', () => {
    describe('Добавление задачи', () => {
        beforeEach(function() {
            return this.browser
                .yaLogin()
                .yaOpenPage('/subject/problem/?problem_id=T9194' + pUid + additionalParams)
                .yaShouldBeVisible(PO.Task.AddButton())
                .click(PO.Task.AddButton())
                .yaWaitForVisible(PO.AddToVariantModal());
        });

        it('Внешний вид', function() {
            return this.browser.assertView('popup', PO.AddToVariantModal());
        });

        it('Закрытие попапа по кнопке', function() {
            return this.browser
                .click(PO.AddToVariantModal.Close())
                .yaShouldBeVisible(PO.AddToVariantModal(), false)
                .yaShouldBeVisible(PO.Task());
        });

        it('Закрытие попапа по клику вне окна', function() {
            return this.browser
                .leftClick(PO.Modal(), 10, 10)
                .yaShouldBeVisible(PO.AddToVariantModal(), false)
                .yaShouldBeVisible(PO.Task());
        });

        it('Скролл списка вариантов', function() {
            return this.browser
                .yaScroll(PO.AddToVariantModal.LastVariant())
                .yaVisibleOnViewport(PO.AddToVariantModal.LastVariant());
        });

        it('Ховер на эл-те списка вариантов', function() {
            return this.browser
                .moveToObject(PO.AddToVariantModal.FirstVariant())
                .assertView('hovered', PO.AddToVariantModal.FirstVariant());
        });

        it('В существующий вариант', function() {
            return this.browser
                .click(PO.AddToVariantModal.SecondVariant())
                .yaWaitForHidden(PO.AddToVariantModal.Spinner(), 6000)
                .yaWaitForVisible(PO.Alert(), 5000)
                .yaShouldBeVisible(PO.Alert())
                .assertView('variant', PO.Alert());
        });
    });

    it('Добавление задачи без логина c параметром integration=mes', function() {
        return this.browser
            .yaOpenPage('/subject/problem/?problem_id=T9194&integration=mes' + pUid + additionalParams)
            .yaShouldBeVisible(PO.Task.AddButton())
            .click(PO.Task.AddButton())
            .yaWaitForVisible(PO.AddToVariantModal())
            .assertView('popup', PO.AddToVariantModal());
    });

    describe('Добавление варианта', () => {
        beforeEach(function() {
            return this.browser
                .yaLogin()
                .yaOpenPage('/subject/problem/?problem_id=T9194' + pUid + additionalParams)
                .yaShouldBeVisible(PO.Task.AddButton())
                .click(PO.Task.AddButton())
                .yaWaitForVisible(PO.AddToVariantModal.AddFariant(), 5000)
                .click(PO.AddToVariantModal.AddFariant())
                .yaShouldBeVisible(PO.CreateVariantForm());
        });

        it('Внешний вид', function() {
            return this.browser
                .assertView('popup', PO.CreateVariantForm());
        });

        it('Закрытие попапа по крестику', function() {
            return this.browser
                .click(PO.CreateVariantForm.Close())
                .yaShouldBeVisible(PO.CreateVariantForm(), false)
                .yaShouldBeVisible(PO.Task());
        });

        it('Закрытие попапа по кнопке Отмена', function() {
            return this.browser
                .click(PO.CreateVariantForm.Cancel())
                .yaShouldBeVisible(PO.CreateVariantForm(), false)
                .yaShouldBeVisible(PO.Task());
        });
    });

    describe('Попап редактирования варианта', () => {
        beforeEach(function() {
            return this.browser
                .yaLogin()
                .yaOpenPage('/user/variants/edit/?variant_id=3671' + pUid + additionalParams)
                .yaShouldBeVisible(PO.EditVariantButton())
                .click(PO.EditVariantButton())
                .yaShouldBeVisible(PO.CreateVariantForm.Submit());
        });

        it('Внешний вид', function() {
            return this.browser
                .getValue(PO.CreateVariantForm.TimeInput.input())
                .then(text => assert.isTrue(Boolean(text.trim())))
                .assertView('plain', PO.CreateVariantForm());
        });

        it('Закрытие попапа по клику в крестик', function() {
            return this.browser
                .click(PO.CreateVariantForm.Close())
                .yaShouldBeVisible(PO.CreateVariantForm(), false)
                .yaShouldBeVisible(PO.UpperCard());
        });

        it('Закрытие попапа по кнопке Отмена', function() {
            return this.browser
                .click(PO.CreateVariantForm.Cancel())
                .yaShouldBeVisible(PO.CreateVariantForm(), false)
                .yaShouldBeVisible(PO.UpperCard());
        });
    });
});
