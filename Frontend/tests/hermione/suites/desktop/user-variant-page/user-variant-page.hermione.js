const checkUrlParams = { skipProtocol: true, skipHostname: true };

hermione.only.notIn(['linux-chrome-ipad', 'linux-firefox'], 'нет такой функциональности');

describe('Страница пользовательских вариантов', function() {
    const pUid = '&passport_uid=user_variant_test_no_variants';

    describe('Варианты не созданы', function() {
        beforeEach(function() {
            return this.browser.yaLogin();
        });

        it('Переход на страницу', function() {
            return this.browser
                .yaOpenPage('/user/statistics/?' + pUid)
                .assertView('my-variants-button', PO.CabinetRightCard())
                .yaWaitChangeUrl(() => this.browser.click(PO.CabinetRightCard.MyVariantsLink()))
                .assertView('empty-variants', PO.MyVariantsList());
        });
    });

    describe('Варианты созданы', function() {
        const pUid = 'passport_uid=user_variant_test';
        const additionalParams = '&mock=1';

        beforeEach(function() {
            return this.browser
                .yaLogin()
                .yaOpenPage('/user/variants/?' + pUid + additionalParams);
        });

        it('Страница пользовательских вариантов', function() {
            return this.browser.assertView('plain', PO.MyVariantsList());
        });

        it('Кнопка "Добавить вариант"', function() {
            return this.browser
                .click(PO.MyVariantsList.CreateButton())
                .yaShouldBeVisible(PO.CreateVariantForm())
                .assertView('create-variant-popup', PO.CreateVariantForm());
        });

        it('Проверка ссылки "Пройти вариант"', function() {
            return this.browser
                .yaCheckLink(
                    PO.MyVariantsList.FirstRow.MyVariant.ToSolution(),
                    '/tutor/subject/variant/?variant_id=3673',
                    checkUrlParams,
                    ['variant_id'],
                )
                .yaCheckTargetBlank(PO.MyVariantsList.FirstRow.MyVariant.ToSolution());
        });

        it('Версия для печати', function() {
            return this.browser
                .click(PO.MyVariantsList.FirstRow.MyVariant.More())
                .yaWaitForVisible(PO.MyVariantPopup())
                .yaShouldBeVisible(PO.MyVariantPopup.PrintAction())
                .assertView('print-popup', PO.MyVariantPopup.PrintAction())
                .yaCheckLink(
                    PO.MyVariantPopup.PrintAction(),
                    '/tutor/subject/variant/?print=1&variant_id=3673',
                    checkUrlParams,
                    ['variant_id', 'print'],
                )
                .yaCheckTargetBlank(PO.MyVariantPopup.PrintAction());
        });

        it('Проверка ссылки редактирование', function() {
            return this.browser
                .yaCheckLink(
                    PO.MyVariantsList.FirstRow.MyVariant.ToEditor(),
                    '/tutor/user/variants/edit/?variant_id=3673',
                    checkUrlParams,
                    ['variant_id'],
                )
                .yaCheckTargetBlank(PO.MyVariantsList.FirstRow.MyVariant.ToEditor());
        });

        it('Копирование ссылки', function() {
            return this.browser
                .click(PO.MyVariantsList.FirstRow.MyVariant.More())
                .yaWaitForVisible(PO.MyVariantPopup())
                .click(PO.MyVariantPopup.CopyAction())
                .yaWaitForVisible(PO.Alert());
        });

        it('Удаление варианта', function() {
            let count;

            return this.browser
                .elements(PO.MyVariantsList.Row())
                .then(({ value }) => { count = value.length })
                .click(PO.MyVariantsList.FirstRow.MyVariant.More())
                .yaWaitForVisible(PO.MyVariantPopup())
                .click(PO.MyVariantPopup.DeleteAction())
                .yaWaitForVisible(PO.DeleteVariantModal())
                .click(PO.DeleteVariantModal.ActionButton())
                .yaWaitForHidden(PO.DeleteVariantModal())
                .elements(PO.MyVariantsList.Row())
                .then(({ value }) => { assert.equal(value.length, count - 1, 'Вариант не удалился из списка') });
        });
    });

    describe('Редактирование пользовательского варианта', function() {
        const pUid = '&passport_uid=user_variant_test';
        const additionalParams = '&mock=1';

        beforeEach(function() {
            return this.browser.yaLogin();
        });

        describe('Пустой вариант', function() {
            beforeEach(function() {
                return this.browser
                    .yaOpenPage('/subject/variant?variant_id=3670');
            });

            it('Внешний вид страницы пустого варианта', function() {
                return this.browser
                    .assertView('plain', PO.PageLayout.Left());
            });
        });

        describe('Вариант с задачами', function() {
            beforeEach(function() {
                return this.browser
                    .yaOpenPage('/user/variants/edit/?variant_id=3671' + pUid + additionalParams);
            });

            hermione.only.notIn(['win-edge'], 'Особенности эмуляции не позволяют делать dragAndDrop');
            it('Порядок задач', function() {
                return this.browser
                    .waitForVisible(PO.GrabMover.Card())
                    .assertView('before', PO.GrabMover.Card())
                    .moveToObject(PO.GrabMover.Content.FirstItem())
                    .buttonDown(0)
                    .moveToObject(PO.GrabMover.Content.SecondItem(), 300, 0)
                    .moveToObject(PO.GrabMover.Content.FouthItem(), 300, 0)
                    .buttonUp(0)
                    .assertView('after', PO.GrabMover.Card());
            });
        });
    });

    it('Добавление варианта без логина c параметром integration=mes', function() {
        return this.browser
            .yaOpenPage('/user/variants/?integration=mes' + pUid)
            .yaShouldBeVisible(PO.MyVariantsList.CreateButton())
            .click(PO.MyVariantsList.CreateButton())
            .yaWaitForVisible(PO.CreateVariantForm())
            .assertView('create-variant-popup', PO.CreateVariantForm());
    });
});
