const AJAX_TIMEOUT = 10000;

describe('Страница репорта', function() {
    const getReportUrl = id => `/subject/variant/report/?report_id=${id}`;
    const checkUrlParams = { skipProtocol: true, skipHostname: true, skipQuery: true };
    const requestDuration = 10000;

    describe('Breadcrumbs', () => {
        beforeEach(function() {
            return this.browser.yaOpenPage(getReportUrl('9ec3eb7c5099b3fe842a6d7691b4274d'));
        });

        it('Внешний вид', function() {
            return this.browser.assertView('plain', PO.BreadCrumbs());
        });

        it('Четвертая ссылка', function() {
            return this.browser
                .getTagName(PO.BreadCrumbs.last())
                .then(actualText => assert.notEqual(actualText, 'a'));
        });

        it('Третья ссылка', function() {
            return this.browser.yaCheckLink(
                PO.BreadCrumbs.third(),
                '/tutor/subject/variant/?variant_id=1',
                checkUrlParams,
                ['variant_id']
            );
        });

        it('Вторая ссылка', function() {
            return this.browser.yaCheckLink(
                PO.BreadCrumbs.second(),
                '/tutor/subject/?subject_id=1',
                checkUrlParams,
                ['subject_id']
            );
        });

        it('Первая ссылка', function() {
            return this.browser.yaCheckLink(
                PO.BreadCrumbs.first(),
                '/tutor/?exam_id=1',
                checkUrlParams,
                ['exam_id']
            );
        });
    });

    it('Карточка связанных вариантов', function() {
        return this.browser
            .yaOpenPage(getReportUrl('9ec3eb7c5099b3fe842a6d7691b4274d'))
            .assertView('plain', PO.RelatedLinks())
            .yaCheckLink(
                PO.RelatedLinks.FirstLink(),
                '/tutor/subject/variant/?variant_id=366',
                checkUrlParams,
                ['variant_id'],
            );
    });

    describe('Карточка результатов', () => {
        const scrollDuration = 1200;

        beforeEach(function() {
            return this.browser.yaOpenPage(getReportUrl('9ec3eb7c5099b3fe842a6d7691b4274d'));
        });

        it('Внешний вид', function() {
            return this.browser.assertView('plain', PO.Report(), {
                ignoreElements: [PO.Report.Time()],
            });
        });

        it('Переход к первому заданию', function() {
            return this.browser
                .click(PO.ReportTable.FirstTaskLink())
                .pause(scrollDuration)
                .yaVisibleOnViewport(PO.TasksToSolveListFirstItem());
        });

        it('Переход к последнему заданию', function() {
            return this.browser
                .click(PO.ReportTable.LastTaskLink())
                .pause(scrollDuration)
                .yaVisibleOnViewport(PO.TasksToSolveListLastItem());
        });

        it('Ссылка "Моя статистика"', function() {
            return this.browser.yaCheckLink(
                PO.Report.Statistic(),
                '/tutor/user/statistics/',
                checkUrlParams,
            );
        });
    });

    it('Отображение только неверно решенных задач', function() {
        const getTasksCount = selector => document.querySelectorAll(selector).length;

        const checkTasksCount = (actual, expected) =>
            assert.strictEqual(actual, expected, 'Количество задач не соответствуют');

        return this.browser
            .yaOpenPage(getReportUrl('9ec3eb7c5099b3fe842a6d7691b4274d'))
            .execute(getTasksCount, PO.TasksToSolveList())
            .then(result => checkTasksCount(result.value, 20))
            .click(PO.ReportTasks.Checkbox())
            .execute(getTasksCount, PO.TasksToSolveList())
            .then(result => checkTasksCount(result.value, 16));
    });

    it('Карточка результатов - порог не пройден', function() {
        return this.browser
            .yaOpenPage(getReportUrl('12fbdf9b0f4527139620c07863804c68'))
            .assertView('plain', PO.Report());
    });

    it('Карточка результатов - порог пройден', function() {
        return this.browser
            .yaOpenPage(getReportUrl('e6ffc520a129f7f157d23d2678009e01'))
            .assertView('plain', PO.Report());
    });

    it('Карточка задачи - верно', function() {
        const wrongAnswer = 'wrong_answer';

        return this.browser
            .yaOpenPage(getReportUrl('e6ffc520a129f7f157d23d2678009e01'))
            .scroll(PO.TasksToSolveListFirstItem())
            .assertView('plain', PO.TasksToSolveListFirstItem())
            .setValue(PO.TasksToSolveListFirstItem.Input(), wrongAnswer)
            .click(PO.TasksToSolveListFirstItem.TaskControlLine.Button())
            .waitForExist(
                PO.TasksToSolveListFirstItem.InputWrong(),
                requestDuration,
            );
    });

    it('Карточка задачи - неверно', function() {
        const correctAnswer = '3';

        return this.browser
            .yaOpenPage(getReportUrl('12fbdf9b0f4527139620c07863804c68'))
            .scroll(PO.TasksToSolveListLastItem())
            .assertView('plain', PO.TasksToSolveListLastItem())
            .setValue(PO.TasksToSolveListLastItem.Input(), correctAnswer)
            .click(PO.TasksToSolveListLastItem.TaskControlLine.Button())
            .waitForExist(
                PO.TasksToSolveListLastItem.InputCorrect(),
                requestDuration,
            );
    });

    it('Результаты контрольной работы до открытия ответов', function() {
        return this.browser
            .yaOpenPage(getReportUrl('3b364f428c8ecab4a9c519acb9353fbb') + '&server_time=1556259200')
            .assertView('plain', PO.Report(), {
                ignoreElements: [PO.Report.Time()],
            })
            .yaCheckCount(PO.TaskTitle.Id(), 0, 'на странице есть id заданий')
            .yaCheckCount(PO.TaskResultLine.ToggleAnswer(), 0, 'на странице есть ответы к заданиям');
    });

    it('Результаты с дополнительной информацией', function() {
        return this.browser
            .yaOpenPage(getReportUrl('1ba782ad15d8268c07a075e9406eef0a'))
            .assertView('plain', PO.Report.PointsInfo());
    });

    describe('Рекомендации', function() {
        const reportUrl = getReportUrl('3fb1695a147b618d02cc91b189e1ca8b');

        it('Без авторизации', function() {
            return this.browser
                .yaOpenPage(reportUrl)
                .yaShouldBeVisible(PO.RecommendedTasks())
                .yaScroll(PO.RecommendedTasks())
                .yaWaitForVisible(PO.RecommendedTasks.EmptyButton(), AJAX_TIMEOUT)
                .assertView('plain', [PO.RecommendedTasks(), PO.RecommendedTasks.EmptyButton()]);
        });

        it('С авторизацией', function() {
            const params = '&passport_uid=222355007&problems_count=5';

            return this.browser
                .yaLogin()
                .yaOpenPage(reportUrl + params)
                .yaShouldBeVisible(PO.RecommendedTasks())
                .yaScroll(PO.RecommendedTasks())
                .yaWaitForVisible(PO.RecommendedTasks.Button(), AJAX_TIMEOUT)
                .assertView('plain', [PO.RecommendedTasks(), PO.RecommendedTasks.Button()]);
        });
    });

    describe('ПДД', function() {
        it('Общий вид', function() {
            return this.browser
                .yaOpenPage(getReportUrl('1aa328e53467218e87afbadcea69ec0c'))
                .assertView('switcher', PO.ReportWizardPage.ReportTasks())
                .assertView('fisrt', PO.ReportWizardPage.TasksToSolveList());
        });
    });
});
