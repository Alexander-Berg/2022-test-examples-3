const defaultCheckPageConf = { skipProtocol: true, skipHostname: true, skipQuery: true };
const waitingTime = 15000; // т.к. за данными ходим на сервер
const getTaskUrl = id => `/subject/problem/?problem_id=T${id}`;
const AJAX_TIMEOUT = 10000;

describe('Страница задания', function() {
    it('Переход на страницу задачи', function() {
        return this.browser
            .yaOpenPage(getTaskUrl('333'))
            .waitForExist(PO.SingleTaskPage(), waitingTime);
    });

    describe('Breadcrumbs', () => {
        beforeEach(function() {
            return this.browser.yaOpenPage(getTaskUrl('333'));
        });

        it('Первая ссылка', function() {
            return this.browser.yaCheckLink(
                PO.BreadCrumbs.first(),
                '/tutor/?exam_id=1',
                defaultCheckPageConf,
                ['exam_id']
            );
        });

        it('Вторая ссылка', function() {
            return this.browser.yaCheckLink(
                PO.BreadCrumbs.second(),
                '/tutor/subject/?subject_id=2',
                defaultCheckPageConf,
                ['subject_id']
            );
        });

        it('Третья ссылка', function() {
            return this.browser.yaCheckLink(
                PO.BreadCrumbs.third(),
                '/tutor/subject/tag/problems/?ege_number_id=176&tag_id=98',
                defaultCheckPageConf,
                ['ege_number_id', 'tag_id']
            );
        });

        it('Четвертая ссылка', function() {
            return this.browser
                .getTagName(PO.BreadCrumbs.last())
                .then(actualText => assert.notEqual(actualText, 'a'));
        });

        it('Проверка внешнего вида', function() {
            return this.browser.assertView('plain', PO.BreadCrumbs());
        });
    });

    describe('Карточка связанных разделов/вариантов', () => {
        beforeEach(function() {
            return this.browser.yaOpenPage(getTaskUrl('333'));
        });

        it('Переход на страницу раздела', function() {
            return this.browser
                .assertView('plain', PO.RelatedLinks())
                .yaCheckLink(
                    PO.RelatedLinksFragmentFirstLink(),
                    '/tutor/subject/tag/problems/?ege_number_id=176&tag_id=19',
                    defaultCheckPageConf,
                    ['ege_number_id', 'tag_id']
                );
        });

        it('Переход на страницу варианта', function() {
            return this.browser.yaCheckLink(
                PO.RelatedLinksFragmentLastLink(),
                '/tutor/subject/variant/?variant_id=20',
                defaultCheckPageConf,
                ['variant_id']
            );
        });
    });

    it('Аналоги', function() {
        return this.browser
            .yaOpenPage(getTaskUrl('269'))
            .click(PO.PrototypesLinkItem())
            .yaWaitUntil('страница не обновилась', () => {
                return this.browser
                    .isExisting(PO.PrototypesItem());
            }, 3000)
            .click(PO.PrototypesLinkItem())
            .yaWaitForHidden(PO.PrototypesItem(), 3000, 'страница не обновилась');
    });

    describe('Задача из контрольной работы', () => {
        const taskUrl = getTaskUrl('7730');

        it('До открытия ответов', function() {
            return this.browser
                .yaOpenPage(taskUrl + '&server_time=1556259200')
                .yaShouldBeVisible(PO.SingleTaskPage(), false)
                .getText(PO.EduCardTitle())
                .then(text => assert.equal(text, 'Ошибка 404. Нет такой страницы'));
        });

        it('После открытия ответов', function() {
            return this.browser
                .yaOpenPage(taskUrl)
                .yaShouldBeVisible(PO.SingleTaskPage());
        });
    });

    describe('Блок ближайшего достижения', () => {
        const taskUrl = getTaskUrl('1');
        const AchievementCard = PO.AchievementCard();

        it('Без авторизации', function() {
            return this.browser
                .yaOpenPage(taskUrl)
                .yaShouldBeVisible(AchievementCard, false);
        });

        it('С авторизацией', function() {
            const rightAnswer = '3';
            const params = '&passport_uid=hermione_achievement_progress&server_time=1573042084&testing_achievements=1';

            return this.browser
                .yaLogin()
                .yaOpenPage(taskUrl + params)
                .yaWaitForVisible(AchievementCard)
                .assertView('first', AchievementCard)
                .scroll(AchievementCard)
                .click(PO.AchievementCard.Icon())
                .yaShouldBeVisible(PO.AchievementPopup())
                .setValue(PO.TaskControlLine.input(), rightAnswer)
                .click(PO.TaskControlLine.button())
                .yaWaitForVisible(PO.TextInputCorrect())
                .assertView('second', AchievementCard);
        });
    });

    describe('Рекомендации', function() {
        const taskUrl = getTaskUrl('1');

        it('Без авторизации', function() {
            return this.browser
                .yaOpenPage(taskUrl)
                .yaShouldBeVisible(PO.RecommendedTasks())
                .yaScroll(PO.RecommendedTasks())
                .yaWaitForVisible(PO.RecommendedTasks.EmptyButton(), AJAX_TIMEOUT)
                .assertView('plain', [PO.RecommendedTasks(), PO.RecommendedTasks.EmptyButton()]);
        });

        it('С авторизацией', function() {
            const params = '&passport_uid=222355007&problems_count=5';

            return this.browser
                .yaLogin()
                .yaOpenPage(taskUrl + params)
                .yaShouldBeVisible(PO.RecommendedTasks())
                .yaScroll(PO.RecommendedTasks())
                .yaWaitForVisible(PO.RecommendedTasks.Button(), AJAX_TIMEOUT)
                .assertView('plain', [PO.RecommendedTasks(), PO.RecommendedTasks.Button()]);
        });
    });
});
