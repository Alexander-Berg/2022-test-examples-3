const skipUrlOptions = { skipProtocol: true, skipHostname: true, skipQuery: true };
const prefixUrl = '/tutor/subject/lesson';
const reportUrl = `${prefixUrl}/test/result/`;
const lessonUrl = `${prefixUrl}/test/`;
const lectureUrl = `${prefixUrl}/lecture/`;
const tasksUrl = `${prefixUrl}/tasks/`;
const homeUrl = `${prefixUrl}/home/`;
const AJAX_TIMEOUT = 10000;

const changeLessonSelect = async(browser, itemSelector, screenshotName) => {
    let prevCountTask;

    await browser
        .click(PO.LessonSelect.Button())
        .yaWaitForVisible(PO.visiblePopup.menu())
        .execute(function(selector) {
            document.querySelectorAll(selector).length;
        }, itemSelector)
        .then(count => prevCountTask = count);

    if (screenshotName) {
        await browser.assertView(screenshotName, PO.visiblePopup.menu());
    }

    return browser.click(PO.visiblePopup.lastMenuItem())
        .yaWaitForVisible(itemSelector, AJAX_TIMEOUT)
        .execute(function(selector) {
            document.querySelectorAll(selector).length;
        }, itemSelector)
        .then(count => assert.ok(prevCountTask !== count));
};

const clickLessonThemeSwitcher = (browser, expectedUrl, expectedTabName, itemSelector) => {
    return browser
        .yaWaitChangeUrl(() => browser.click(PO.LessonThemeSwitcher.button()))
        .yaCheckPageUrl(expectedUrl, skipUrlOptions)
        .yaWaitForVisible(itemSelector, AJAX_TIMEOUT)
        .getText(PO.LessonTabs.Select.active())
        .then(text => assert.strictEqual(text, expectedTabName));
};

describe('Страница урока', () => {
    beforeEach(function() {
        return this.browser
            .yaOpenPage('/subject/lesson/test/?lesson_id=1&subject_id=1')
            .yaFixStickyOnPage();
    });

    it('Табы', function() {
        return this.browser
            .getText(PO.LessonTabs.Select.active())
            .then(text => {
                assert.strictEqual(text, 'Тест');
            })
            .moveToObject(PO.UserEnter())
            .assertView('tabs', PO.LessonTabs())
            .yaWaitChangeUrl(() => this.browser.click(PO.LessonTabs.second()))
            .yaCheckPageUrl(lectureUrl, skipUrlOptions)
            .yaWaitForVisible(PO.TheoryMiniViewer(), AJAX_TIMEOUT)
            .moveToObject(PO.UserEnter())
            .getText(PO.LessonTabs.Select.active())
            .then(text => {
                assert.strictEqual(text, 'Лекция');
            })
            .assertView('tabs-selected', PO.LessonTabs.Main());
    });

    describe('Раздел Тест', function() {
        it('Есть верно решенные задачи', function() {
            return this.browser
                .setValue(PO.LessonContentResolver.FirstRow.Task.input(), '7295331')
                .yaWaitChangeUrl(() => this.browser.click(PO.FinishButton()))
                .yaCheckPageUrl(reportUrl, skipUrlOptions)
                .yaWaitForVisible(PO.Report(), AJAX_TIMEOUT)
                .getText(PO.Report.Title())
                .then(text => assert.strictEqual(text, 'Вы правильно решили: 1 из 9'));
        });

        it('Все задания не решены', function() {
            return this.browser
                .yaWaitChangeUrl(() => this.browser.click(PO.FinishButton()))
                .yaCheckPageUrl(reportUrl, skipUrlOptions)
                .yaWaitForVisible(PO.Report(), AJAX_TIMEOUT)
                .getText(PO.Report.Title())
                .then(text => assert.strictEqual(text, 'Вы правильно решили: 0 из 9'));
        });

        it('Кнопка "Пройти еще раз"', function() {
            return this.browser
                .yaWaitChangeUrl(() => this.browser.click(PO.FinishButton()))
                .yaCheckPageUrl(reportUrl, skipUrlOptions)
                .yaWaitForVisible(PO.Report(), AJAX_TIMEOUT)
                .yaWaitChangeUrl(() => this.browser.click(PO.InfoLine.RepeatButton()))
                .yaCheckPageUrl(lessonUrl, skipUrlOptions)
                .yaWaitForVisible(PO.LessonTabs(), AJAX_TIMEOUT)
                .yaWaitChangeUrl(() => this.browser.click(PO.FinishButton()))
                .yaCheckPageUrl(reportUrl, skipUrlOptions)
                .yaWaitForVisible(PO.Report(), AJAX_TIMEOUT);
        });

        it('Кнопка "К лекциям"', function() {
            return this.browser
                .yaWaitChangeUrl(() => this.browser.click(PO.FinishButton()))
                .yaCheckPageUrl(reportUrl, skipUrlOptions)
                .yaWaitForVisible(PO.Report(), AJAX_TIMEOUT)
                .yaWaitChangeUrl(() => this.browser.click(PO.LessonThemeSwitcher.button()))
                .yaCheckPageUrl(lectureUrl, skipUrlOptions)
                .yaWaitForVisible(PO.TheoryMiniViewer(), AJAX_TIMEOUT)
                .getText(PO.LessonTabs.Select.active())
                .then(text => {
                    assert.strictEqual(text, 'Лекция');
                });
        });
    });

    describe('Раздел Лекция', function() {
        beforeEach(function() {
            return this.browser
                .click(PO.LessonTabs.second())
                .yaWaitForVisible(PO.TheoryMiniViewer(), AJAX_TIMEOUT)
                .getText(PO.LessonTabs.Select.active())
                .then(text => assert.strictEqual(text, 'Лекция'));
        });

        it('Кнопка "К задачам"', function() {
            return clickLessonThemeSwitcher(this.browser, tasksUrl, 'Задачи', PO.LessonContentResolver.Task());
        });

        it('Выбор уровня сложности', function() {
            return changeLessonSelect(this.browser, PO.LessonContentResolver.Task());
        });
    });

    describe('Раздел Задачи', function() {
        beforeEach(function() {
            return this.browser
                .click(PO.LessonTabs.third())
                .yaWaitForVisible(PO.LessonContentResolver.Task(), 10000)
                .getText(PO.LessonTabs.Select.active())
                .then(text => assert.strictEqual(text, 'Задачи'));
        });

        it('Кнопка "К домашнему заданию"', function() {
            return clickLessonThemeSwitcher(this.browser, homeUrl, 'На дом', PO.LessonContentResolver.Task());
        });

        it('Выбор уровня сложности', function() {
            return changeLessonSelect(this.browser, PO.LessonContentResolver.Task());
        });
    });

    describe('Раздел На дом', function() {
        beforeEach(function() {
            return this.browser
                .click(PO.LessonTabs.forth())
                .yaWaitForVisible(PO.LessonContentResolver.Task(), AJAX_TIMEOUT)
                .getText(PO.LessonTabs.Select.active())
                .then(text => assert.strictEqual(text, 'На дом'));
        });

        it('Выбор уровня сложности', function() {
            return changeLessonSelect(this.browser, PO.LessonContentResolver.Task());
        });

        it('Сохранение уровня сложности при переходах', async function() {
            await changeLessonSelect(this.browser, PO.LessonContentResolver.Task());

            const homeFilter = await this.browser
                .click(PO.LessonSelect.Button())
                .getText(PO.visiblePopup.lastMenuItem());

            await this.browser
                .click(PO.LessonTabs.second())
                .yaWaitForVisible(PO.TheoryMiniViewer(), AJAX_TIMEOUT);

            await changeLessonSelect(this.browser, PO.LessonContentResolver.Task());

            const lessonFilter = await this.browser
                .click(PO.LessonSelect.Button())
                .yaWaitForVisible(PO.visiblePopup.menu())
                .getText(PO.visiblePopup.lastMenuItem());

            return this.browser.click(PO.LessonTabs.forth())
                .yaWaitForVisible(PO.LessonContentResolver.Task(), AJAX_TIMEOUT)
                .click(PO.LessonSelect.Button())
                .yaWaitForVisible(PO.visiblePopup.menu())
                .getText(PO.visiblePopup.lastMenuItem())
                .then(text => assert.strictEqual(text, homeFilter))
                .click(PO.LessonTabs.second())
                .yaWaitForVisible(PO.TheoryMiniViewer(), AJAX_TIMEOUT)
                .click(PO.LessonSelect.Button())
                .yaWaitForVisible(PO.visiblePopup.menu())
                .getText(PO.visiblePopup.lastMenuItem())
                .then(text => assert.strictEqual(text, lessonFilter));
        });
    });
});
