const getProblemUrl = id => `/subject/problem/?problem_id=T${id}`;

describe('Карточка задачи', function() {
    function checkTask(problemId, ignore) {
        return this.browser
            .yaOpenPage(getProblemUrl(problemId))
            .then(() => {
                return ignore ? this.browser : this.browser.assertView('plain', PO.Task());
            });
    }

    it('Задача с картинкой', function() {
        return checkTask.call(this, 811);
    });

    it('Задача с таблицей', function() {
        return checkTask.call(this, 1339)
            .yaCheckTargetBlank('.Task-Author a')
            .yaCheckLink('.Task-Author a', 'http://fipi.ru', { skipProtocol: true, skipQuery: true, skipPathname: true });
    });

    it('Задача с блоком кода с табуляцией', function() {
        return checkTask.call(this, 4819)
            .click('.Tabs-Item:last-of-type')
            .moveToObject(PO.UserEnter())
            .getText('.Tabs-Item_selected')
            .then(text => assert(text === 'C++', 'Выбран не C++ таб'))
            .assertView('another-tab', PO.Task());
    });

    it('Задача с блоком кода', function() {
        return checkTask.call(this, 787);
    });

    it('Задача с блоком текста - >500 символов', function() {
        return checkTask.call(this, 70)
            .click(PO.TaskQuoteLink())
            .yaWaitForVisible(PO.TaskBlockQuote.Visible())
            .assertView('open', PO.Task());
    });

    it('Задача с блоком текста - <500 символов', function() {
        return checkTask.call(this, 398);
    });

    it('Задача со списком', function() {
        return checkTask.call(this, 7745);
    });

    it('Задача с буквенным списком', function() {
        return checkTask.call(this, 7536);
    });

    it('Задача с множественным списком', function() {
        return checkTask.call(this, 388);
    });

    it('Задача с критериями', function() {
        return this.browser
            .yaOpenPage(getProblemUrl(7295))
            .click(PO.TaskToggleAnswerText())
            .yaWaitForVisible(PO.TaskResultLine.VisibleAnswer())
            .assertView('plain', PO.TaskCriteria());
    });

    describe('Проверка ответа - неверный', () => {
        const errMess = 'Надпись не соотвествует "Задание было решено неверно"';

        beforeEach(function() {
            return this.browser.yaOpenPage(getProblemUrl(979));
        });

        it('Ввод пустого ответа', function() {
            return this.browser
                .click(PO.TaskControlLine.button())
                .yaWaitForVisible(PO.TextInputWrong())
                .assertView('plain', PO.Task())
                .getText(PO.UserAttemptCol.Text())
                .then(text => assert.equal(text, 'Задание было решено неверно', errMess));
        });

        it('Ввод неверного ответа', function() {
            const wrongAnswer = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

            return this.browser
                .setValue(PO.TaskControlLine.input(), wrongAnswer)
                .click(PO.TaskControlLine.button())
                .yaWaitForVisible(PO.TextInputWrong())
                .assertView('plain', PO.Task())
                .getText(PO.UserAttemptCol.Text())
                .then(text => assert.equal(text, 'Задание было решено неверно', errMess));
        });
    });

    it('Проверка ответа - верный', function() {
        const rightAnswer = 3241;

        return this.browser
            .yaOpenPage(getProblemUrl(979))
            .setValue(PO.TaskControlLine.input(), rightAnswer)
            .click(PO.TaskControlLine.button())
            .yaWaitForVisible(PO.TextInputCorrect())
            .assertView('plain', PO.Task())
            .getText(PO.UserAttemptCol.Text())
            .then(text => assert.equal(text, 'Задание было решено верно', 'Неверная подпись о попытке'))
            .getText(PO.TaskToggleAnswerText())
            .then(text => assert.equal(text, 'Показать ответ'))
            .click(PO.TaskToggleAnswerText())
            .yaWaitForVisible(PO.TaskResultLine.VisibleAnswer())
            .assertView('opened-answer', PO.Task())
            .getText(PO.TaskToggleAnswerText())
            .then(text => assert.equal(text, 'Скрыть ответ'))
            .click(PO.TaskToggleAnswerText())
            .getText(PO.TaskToggleAnswerText())
            .then(text => assert.equal(text, 'Показать ответ'));
    });

    it('Проверка ссылки на форму жалобы', function() {
        const taskNumber = 978;
        const expectedURL = `/support/tutor/troubleshooting/mistake.html?form-answer_short_text_24039=T${taskNumber}`;

        return this.browser
            .yaOpenPage(getProblemUrl(taskNumber))
            .yaCheckLink(
                PO.Task.ReportLink(),
                expectedURL,
                {
                    skipProtocol: true,
                    skipHostname: true,
                }
            )
            .yaCheckTargetBlank(PO.Task.ReportLink());
    });

    describe('Валидация правильного формата ответа', function() {
        const params = { allowViewportOverflow: true };

        function checkValid(problemId, incorrect, correct) {
            return this.browser
                .yaOpenPage(getProblemUrl(problemId))
                .setValue(PO.TaskControlLine.input(), incorrect)
                .yaWaitForVisible(PO.TaskMistakeInfoPopup())
                .assertView('plain', PO.TaskMistakeInfoPopup(), params)
                .yaShouldBeVisible(PO.TextInputRed())
                .assertView('input-red', PO.TextInputRed())
                .setValue(PO.TaskControlLine.input(), correct)
                .yaWaitForHidden(PO.TaskMistakeInfoPopup());
        }

        it('Показывать попап по клику', function() {
            return this.browser
                .yaOpenPage(getProblemUrl(900))
                .click(PO.Task.InfoIcon())
                .yaWaitForVisible(PO.TaskMistakeInfoPopup())
                .assertView('plain', PO.Task(), params)
                // кликаем в пустое место
                .click(PO.TaskTitle())
                .click(PO.TaskToggleAnswerText())
                .yaWaitForHidden(PO.TaskMistakeInfoPopup());
        });

        it('Показывать попап при вводе пробела в русском', function() {
            return checkValid.call(this, 69, 'a b', 'ab');
        });

        it('Показывать попап при вводе точки в математике', function() {
            return checkValid.call(this, 333, '1.2', '1,2');
        });

        it('Показывать попап при вводе умляутов во французском', function() {
            return checkValid.call(this, 1218, 'aäa', 'aaa');
        });
    });
});
