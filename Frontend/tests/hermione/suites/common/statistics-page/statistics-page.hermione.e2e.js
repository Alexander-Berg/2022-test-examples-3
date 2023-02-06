const getTaskUrl = id => `/subject/problem/?problem_id=T${id}`;
const getVariantUrl = id => `/subject/variant/?variant_id=${id}&passport_uid=${Date.now()}`;
const statsUrl = '/user/statistics/';

describe('Страница статистики', () => {
    it('Решение задачи', async function() {
        let attemptText;

        await this.browser
            .yaOpenPage(getTaskUrl('333'))
            .yaWaitForVisible(PO.TaskControlLine.button())
            .click(PO.TaskControlLine.button())
            .yaWaitForVisible(PO.TextInputNone())
            .yaWaitForHidden(PO.TextInputNone(), 5000);

        attemptText = await this.browser.getText(PO.UserAttemptCol.Text());

        assert.equal(attemptText, 'Задание было решено неверно', 'Неверный результат решения задания');

        await this.browser
            .click(PO.TaskControlLine.input())
            .yaKeyPress('13')
            .click(PO.TaskControlLine.button())
            .yaWaitForVisible(PO.TextInputNone())
            .yaWaitForHidden(PO.TextInputNone(), 5000);

        attemptText = await this.browser.getText(PO.UserAttemptCol.Text());

        assert.equal(attemptText, 'Задание было решено неверно', 'Неверный результат решения задания');

        await this.browser
            .click(PO.TaskControlLine.input())
            .yaKeyPress(['BACKSPACE', 'BACKSPACE', '33000'])
            .click(PO.TaskControlLine.button())
            .yaWaitForVisible(PO.TextInputNone())
            .yaWaitForHidden(PO.TextInputNone(), 5000);

        attemptText = await this.browser.getText(PO.UserAttemptCol.Text());

        assert.equal(attemptText, 'Задание было решено верно', 'Неверный результат решения задания');

        await this.browser
            .yaOpenPage(statsUrl)
            .yaWaitForVisible(PO.UserStatistics.StatRowFirstRow.Control())
            .click(PO.UserStatistics.StatRowFirstRow.Control())
            .yaWaitForVisible(PO.TutorTable.Others());

        const firstAttemptText = await this.browser.getText(PO.TutorTable.Others() + ':nth-of-type(4)');
        const secondAttemptText = await this.browser.getText(PO.TutorTable.Others() + ':nth-of-type(3)');
        const thirdAttemptText = await this.browser.getText(PO.TutorTable.Row() + ':nth-of-type(2)');

        assert.match(firstAttemptText, /\d\d:\d\d\n—\n33000/, 'Неверная информация о попытке решения №1 в статистике');
        assert.match(secondAttemptText, /\d\d:\d\d\n13\n33000/, 'Неверная информация о попытке решения №2 в статистике');
        assert.match(thirdAttemptText, /\d\d:\d\d\n#T333\n3\n33000\n33000/, 'Неверная информация о попытке решения №3 в статистике');
    });

    it('Решение варианта (все верно)', async function() {
        const answers = [
            '3', '5', '2', '8,8', '6', '12', '-5', '6350', '2143', '300',
            '-5,5', '4180', '3', '4', '14,4', '243', '2314', '23', '28', '3267',
        ];

        await this.browser
            .yaOpenPage(getVariantUrl(1))
            .yaWaitForVisible(PO.Task());

        for (let i = 0; i < 20; i++) {
            const taskNumber = i + 1;

            await this.browser
                .click(PO.VariantSectionAB.ItemNth(taskNumber).input())
                .yaKeyPress(answers[i]);
        }

        await this.browser
            .click(PO.FinishButton())
            .yaWaitForVisible(PO.FinishModal())
            .yaWaitChangeUrl(() => this.browser.click(PO.FinishModal.Accept()))
            .yaWaitForVisible(PO.Report.Statistic())
            .click(PO.Report.Statistic())
            .yaWaitForVisible(PO.UserStatistics.TabFirstButton())
            .click(PO.UserStatistics.TabFirstButton())
            .yaWaitForVisible(PO.UserStatistics.StatRowFirstRow());

        const statRowText = await this.browser.getText(PO.UserStatistics.StatRowFirstRow());

        assert.match(statRowText, /\d\d:\d\d\n1\n\d+ сек\n20 из 20\n5 из 5/, 'Неверная информация о попытке решения варианта в статистике');
    });

    it('Решение варианта (половина верно)', async function() {
        const answers = [
            '3', '5', '2', '8,8', '6', '12', '-5', '6350', '2143', '300',
            'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong',
        ];

        await this.browser
            .yaOpenPage(getVariantUrl(1))
            .yaWaitForVisible(PO.Task());

        for (let i = 0; i < 20; i++) {
            const taskNumber = i + 1;

            await this.browser
                .click(PO.VariantSectionAB.ItemNth(taskNumber).input())
                .yaKeyPress(answers[i]);
        }

        await this.browser
            .click(PO.FinishButton())
            .yaWaitForVisible(PO.FinishModal())
            .yaWaitChangeUrl(() => this.browser.click(PO.FinishModal.Accept()))
            .yaWaitForVisible(PO.Report.Statistic())
            .click(PO.Report.Statistic())
            .yaWaitForVisible(PO.UserStatistics.TabFirstButton())
            .click(PO.UserStatistics.TabFirstButton())
            .yaWaitForVisible(PO.UserStatistics.StatRowFirstRow());

        const statRowText = await this.browser.getText(PO.UserStatistics.StatRowFirstRow());

        assert.match(statRowText, /\d\d:\d\d\n1\n\d+ сек\n10 из 20\n3 из 5/, 'Неверная информация о попытке решения варианта в статистике');
    });

    it('Решение варианта (НЕверно)', async function() {
        const answers = [
            'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong',
            'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong', 'wrong',
        ];

        await this.browser
            .yaOpenPage(getVariantUrl(1))
            .yaWaitForVisible(PO.Task());

        for (let i = 0; i < 20; i++) {
            const taskNumber = i + 1;

            await this.browser
                .click(PO.VariantSectionAB.ItemNth(taskNumber).input())
                .yaKeyPress(answers[i]);
        }

        await this.browser
            .click(PO.FinishButton())
            .yaWaitForVisible(PO.FinishModal())
            .yaWaitChangeUrl(() => this.browser.click(PO.FinishModal.Accept()))
            .yaWaitForVisible(PO.Report.Statistic())
            .click(PO.Report.Statistic())
            .yaWaitForVisible(PO.UserStatistics.TabFirstButton())
            .click(PO.UserStatistics.TabFirstButton())
            .yaWaitForVisible(PO.UserStatistics.StatRowFirstRow());

        const statRowText = await this.browser.getText(PO.UserStatistics.StatRowFirstRow());

        assert.match(statRowText, /\d\d:\d\d\n1\n\d+ сек\n0 из 20\n2 из 5/, 'Неверная информация о попытке решения варианта в статистике');
    });

    it('Решение варианта (не поддерживается)', async function() {
        await this.browser
            .yaOpenPage(getVariantUrl(9812))
            .yaWaitForVisible(PO.Task())
            .click(PO.VariantSectionAB.ItemNth(1).input())
            .yaKeyPress('answer')
            .click(PO.FinishButton())
            .yaWaitForVisible(PO.FinishModal())
            .yaWaitChangeUrl(() => this.browser.click(PO.FinishModal.Accept()))
            .yaWaitChangeUrl(() => this.browser.click(PO.FinishButton()), 10000)
            .yaWaitForVisible(PO.Report.Statistic())
            .click(PO.Report.Statistic())
            .yaWaitForVisible(PO.UserStatistics.TabFirstButton(), 10000)
            .click(PO.UserStatistics.TabFirstButton())
            .yaWaitForVisible(PO.UserStatistics.StatRowFirstRow(), 10000);

        const statRowText = await this.browser.getText(PO.UserStatistics.StatRowFirstRow());

        assert.match(statRowText, /\d\d:\d\d\n9812\n\d+ сек\n0 из 19\nНе поддерживается/, 'Неверная информация о попытке решения варианта в статистике');
    });
});
