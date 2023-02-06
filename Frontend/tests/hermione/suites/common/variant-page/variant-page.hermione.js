const skipUrlOptions = { skipProtocol: true, skipHostname: true, skipQuery: true };
const animationDelay = 300;

describe('Страница варианта', function() {
    it('Сохранение ответов при перезагрузке страницы', function() {
        const answer = '123';
        const selector = PO.VariantSectionAB.ItemFirst.input();

        return this.browser
            .yaOpenPage('/subject/variant/?subject_id=2&variant_id=331')
            .setValue(selector, answer)
            .refresh()
            .yaWaitForVisible(selector)
            .getValue(selector)
            .then(value => assert.strictEqual(value, answer));
    });

    describe('Модальное окно', function() {
        const reportUrl = '/tutor/subject/variant/report/';

        describe('Завершение теста', function() {
            const expectedText = 'Сдать тест на проверку?';

            hermione.skip.in([/./], 'https://st.yandex-team.ru/YOUNGLINGS-1915');
            it('Первая часть', function() {
                return this.browser
                    .yaOpenPage('/subject/variant/?variant_id=1&subject_id=16')
                    .click(PO.FinishButton())
                    .pause(animationDelay)
                    .yaWaitForVisible(PO.FinishModal())
                    .getText(PO.FinishModal.Text()).then(actualText => assert.equal(actualText, expectedText))
                    .assertView('final-modal', PO.FinishModal.Content())
                    .click(PO.FinishModal.Decline())
                    .pause(animationDelay)
                    .click(PO.FinishButton())
                    .yaWaitForVisible(PO.FinishModal())
                    .yaWaitChangeUrl(() => this.browser.click(PO.FinishModal.Accept()), 10000)
                    .yaCheckPageUrl(reportUrl, skipUrlOptions);
            });

            hermione.skip.in([/./], 'https://st.yandex-team.ru/YOUNGLINGS-1873');
            it('Вторая часть', function() {
                return this.browser
                    .yaOpenPage('/subject/variant/?subject_id=2&variant_id=197')
                    .click(PO.FinishButton())
                    .pause(animationDelay)
                    .yaWaitForVisible(PO.FinishModal())
                    .getText(PO.FinishModal.Text()).then(actualText => assert.equal(actualText, expectedText))
                    .yaWaitChangeUrl(() => this.browser.click(PO.FinishModal.Accept()))
                    .yaCheckPageUrl('/tutor/subject/variant/check/', skipUrlOptions)
                    .yaWaitChangeUrl(() => this.browser.click(PO.FinishButton()), 8000)
                    .yaCheckPageUrl(reportUrl, skipUrlOptions);
            });
        });
    });

    describe('Контрольная работа', function() {
        const getVarianturl = serverTime =>
            '/subject/variant/?variant_id=326&server_time=' + serverTime;

        it('До открытия', function() {
            return this.browser
                .yaOpenPage(getVarianturl('1555718400'))
                .yaShouldBeVisible(PO.VariantTest(), false)
                .yaShouldBeVisible(PO.TimerCard(), false)
                .assertView('plain', PO.LockTest());
        });

        it('До открытия ответов', function() {
            return this.browser
                .yaOpenPage(getVarianturl('1556259200'))
                .yaCheckCount(PO.TaskTitle.Id(), 0, 'на странице есть id заданий');
        });
    });

    it('Количество решенных заданий в варианте не меняется при переходе ко второй части', function() {
        const expectedText = '0 из 19';

        return this.browser
            .yaOpenPage('/subject/variant/?subject_id=2&variant_id=197')
            .click(PO.FinishButton())
            .pause(animationDelay)
            .yaWaitForVisible(PO.FinishModal())
            .yaWaitChangeUrl(() => this.browser.click(PO.FinishModal.Accept()))
            .yaCheckPageUrl('/tutor/subject/variant/check/', skipUrlOptions)
            .getText(PO.TimerClock.SolvedCounter())
            .then(actualText => assert.match(actualText, new RegExp(expectedText)));
    });

    describe('ПДД', function() {
        const getVarianturl = (subjectId, variantId) =>
            `/subject/variant/?passport_uid=222355007&subject_id=${subjectId}&variant_id=${variantId}`;

        it('Общий вид', function() {
            return this.browser
                .yaOpenPage(getVarianturl(30, 1338))
                .assertView('plain', PO.VariantWizard(), { invisibleElements: [PO.TimerCard(), PO.VariantTestPageStickyContent()] });
        });
    });
});
