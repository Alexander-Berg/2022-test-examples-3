describe('Страница топика', () => {
    const waitingTime = 15000;
    const pageUrlDefaultParams = { skipProtocol: true, skipHostname: true, skipQuery: true };

    const getTopicUrl = (egeNumberId, tagId, path = '') => {
        return `${path}/subject/tag/problems/?ege_number_id=${egeNumberId}&tag_id=${tagId}`;
    };

    it('Проверка ссылки для жалобы', function() {
        const url = 'https://yandex.ru/support/tutor/troubleshooting/mistake.html?form-answer_short_text_24039=';

        return this.browser
            .yaOpenPage(getTopicUrl(176, 19))
            .getText(PO.TasksToSolveListFirstItem.Task.Title.Id())
            .then(text =>
                this.browser.yaCheckLink(PO.TasksToSolveListFirstItem.Task.ReportLink(), url + text.substr(1))
            );
    });

    it('Переход на страницу топика', function() {
        const mathUrl = '/tutor/subject/?subject_id=4';

        return this.browser
            .yaOpenPage('/ege/')
            .yaWaitForVisible(PO.SubjectItemFourth())
            .yaWaitChangeUrl(() => this.browser.click(PO.SubjectItemFourth()))
            .yaCheckPageUrl(
                mathUrl, pageUrlDefaultParams
            )
            .yaWaitForVisible(PO.TopicListCard.Catalog())
            .click(PO.TopicListCard.Catalog.RowFirst.TopicNameFirst())
            .yaWaitForVisible(PO.TopicListCard.Catalog.SubsFirstOpened())
            .yaWaitChangeUrl(() => this.browser.click(PO.TopicListCard.Catalog.SubsFirst.SubSectionFirst.Link()))
            .yaCheckPageUrl(
                getTopicUrl(46, 19, '/tutor'),
                pageUrlDefaultParams,
                ['ege_number_id', 'tag_id']
            );
    });

    describe('Breadcrumbs', () => {
        beforeEach(function() {
            return this.browser.yaOpenPage(getTopicUrl(370, 19));
        });

        it('Третья ссылка', function() {
            return this.browser.getTagName(PO.BreadCrumbs.last()).then(actualText => assert.notEqual(actualText, 'a'));
        });

        it('Вторая ссылка', function() {
            return this.browser.yaCheckLink(
                PO.BreadCrumbs.second(),
                '/tutor/subject/?subject_id=4',
                pageUrlDefaultParams,
                ['subject_id']
            );
        });

        it('Первая ссылка', function() {
            return this.browser.yaCheckLink(
                PO.BreadCrumbs.first(),
                '/tutor/?exam_id=1',
                pageUrlDefaultParams,
                ['exam_id']
            );
        });
    });

    hermione.skip.in([/./], 'https://st.yandex-team.ru/YOUNGLINGS-2678');
    it('Фильтр по авторам', function() {
        const getTasksCount = selector => document.querySelectorAll(selector).length;

        const checkTasksCount = (actual, expected) =>
            assert.strictEqual(actual, expected, 'Количество задач не соответствуют');

        return this.browser
            .yaOpenPage(getTopicUrl(176, 19))
            .execute(getTasksCount, PO.TasksToSolveList())
            .then(result => checkTasksCount(result.value, 53))
            .click(PO.FiltersAuthorSelect())
            .waitForExist(PO.FiltersAuthorPopup.LastOption(), waitingTime)
            .assertView('plain', PO.FiltersAuthorPopup())
            .click(PO.FiltersAuthorPopup.LastOption())
            .pause(waitingTime)
            .execute(getTasksCount, PO.TasksToSolveList())
            .then(result => checkTasksCount(result.value, 1));
    });
});
