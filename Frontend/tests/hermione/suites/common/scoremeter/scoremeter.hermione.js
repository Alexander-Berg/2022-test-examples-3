const requestDuration = 10000;

describe('Балломер', function() {
    describe('Неавторизованный пользователь', function() {
        it('Чистый прогресс', function() {
            return this.browser
                .yaOpenPage('/ege/')
                .assertView('plain', PO.Scoremeter());
        });

        it('Решение задачи верно', function() {
            return this.browser
                .yaOpenPage('/subject/problem/?problem_id=T194')
                .setValue(PO.TaskControlLine.input(), '2')
                .click(PO.TaskControlLine.button())
                .waitForExist(
                    PO.TextInputCorrect(),
                    requestDuration,
                )
                .assertView('plain', PO.Scoremeter())
                .yaOpenPage('/subject/problem/?problem_id=T4172') // за это задание дают 2 балла
                .setValue(PO.TaskControlLine.input(), '123')
                .click(PO.TaskControlLine.button())
                .waitForExist(
                    PO.TextInputCorrect(),
                    requestDuration,
                )
                .execute(function() {
                    document.body.style.pointerEvents = 'none';
                    document.body.style.touchAction = 'none';
                })
                .assertView('has-3-subjects', PO.Scoremeter());
        });

        it('Решение задачи неверно', function() {
            return this.browser
                .yaOpenPage('/subject/problem/?problem_id=T194')
                .setValue(PO.TaskControlLine.input(), '1')
                .click(PO.TaskControlLine.button())
                .waitForExist(
                    PO.TextInputWrong(),
                    requestDuration,
                )
                .assertView('plain', PO.Scoremeter());
        });

        it('Начисление баллов', function() {
            return this.browser
                .yaOpenPage('/subject/problem/?problem_id=T666')
                .setValue(PO.TaskControlLine.input(), '126')
                .click(PO.TaskControlLine.button())
                .waitForExist(PO.TextInputCorrect(), requestDuration)
                .getText(PO.Scoremeter.Subject.Number())
                .then(text => assert.strictEqual(text, '1', 'Значение балломера должно быть равно единице'))
                .yaOpenPage('/subject/problem/?problem_id=T667')
                .setValue(PO.TaskControlLine.input(), '1')
                .click(PO.TaskControlLine.button())
                .waitForExist(PO.TextInputWrong(), requestDuration)
                .getText(PO.Scoremeter.Subject.Number())
                .then(text => assert.strictEqual(text, '1', 'Значение балломера не должно измениться'))
                .yaOpenPage('/subject/problem/?problem_id=T266')
                .setValue(PO.TaskControlLine.input(), '10')
                .click(PO.TaskControlLine.button())
                .waitForExist(PO.TextInputCorrect(), requestDuration)
                .getText(PO.Scoremeter.Subject.Number())
                .then(text => assert.strictEqual(text, '1', 'Значение балломера должно возрасти к 2м'));
        });
    });

    it('Авторизованный пользователь', function() {
        return this.browser
            .yaOpenPage('/ege/?passport_uid=ballomerCase')
            .assertView('plain', PO.Scoremeter());
    });
});
