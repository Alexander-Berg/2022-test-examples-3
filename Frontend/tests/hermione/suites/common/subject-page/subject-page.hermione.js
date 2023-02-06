const animationDuration = 500;
const checkUrlParams = { skipProtocol: true, skipHostname: true };

const subjectUrl = '/subject/?subject_id=2';
const AJAX_TIMEOUT = 10000;

function checkRecommendedTask(answer, expectedImmediateResult) {
    let prevText;

    return this.browser
        .getText(PO.RecommendedTasks.Task.Title())
        .then(text => prevText = text)
        .setValue(PO.RecommendedTasks.TaskControlLine.input(), answer)
        .click(PO.RecommendedTasks.TaskControlLine.button())
        .yaWaitForVisible(PO.RecommendedTasks.UserAttemptCol(), AJAX_TIMEOUT, 'результат решения задачи не отобразился')
        .getText(PO.RecommendedTasks.ImmediateResult())
        .then(text => assert.equal(text, expectedImmediateResult))
        .click(PO.RecommendedTasks.TaskControlLine.extraButton())
        .yaWaitUntil('задача не поменялась', () => {
            return this.browser
                .getText(PO.RecommendedTasks.Task.Title())
                .then(text => prevText !== text, _ => true);
        }, 3000);
}

function skipRecommendedTask(expectedImmediateResult) {
    let prevText;

    return this.browser
        .getText(PO.RecommendedTasks.Task.Title())
        .then(text => prevText = text)
        .click(PO.RecommendedTasks.TaskControlLine.extraButton())
        .yaWaitUntil('задача не поменялась', () => {
            return this.browser
                .getText(PO.RecommendedTasks.Task.Title())
                .then(text => prevText !== text, _ => true);
        }, 3000)
        .then(_ => {
            if (expectedImmediateResult) {
                return this.browser
                    .getText(PO.RecommendedTasks.ImmediateResult())
                    .then(text => expectedImmediateResult && assert.equal(text, expectedImmediateResult));
            }

            return true;
        });
}

describe('Страница предмета', () => {
    it('Переход на страницу предмета', function() {
        const link = '/tutor/subject/?subject_id=2';

        return this.browser
            .yaOpenPage('/ege/')
            .yaWaitChangeUrl(() => this.browser.click(PO.SubjectItemSecond()))
            .pause(animationDuration)
            .getText(PO.BreadCrumbs.last.ItemText())
            .then(text =>
                assert.equal(text, 'Математика (профильный уровень)', 'Oткрыта неверная страница предмета'),
            )
            .yaWaitForVisible(PO.EduCardSwitcher())
            .yaCheckPageUrl(
                link,
                checkUrlParams,
                ['subject_id'],
            );
    });

    describe('Карточка вариантов', function() {
        beforeEach(function() {
            return this.browser.yaOpenPage(subjectUrl);
        });

        it('Просмотр всех вариантов', function() {
            return this.browser
                .yaWaitForVisible(PO.EduCardSwitcher())
                .assertView('plain', PO.UpperCard())
                .click(PO.EduCardSwitcher())
                .pause(animationDuration)
                .getText(PO.EduCardSwitcher())
                .then(text => assert.equal(text.trim(), 'Скрыть', 'Текст кнопки не изменился'))
                .moveToObject(PO.UserEnter())
                .assertView('plain2', PO.EduCardVariant())
                .getText(PO.EduCardVariant.Container.ColLasts.Button())
                .then(text => assert.strictEqual(text.trim(), '1', 'Не все варианты отобразились'))
                .click(PO.EduCardSwitcher())
                .pause(animationDuration)
                .getText(PO.EduCardSwitcher())
                .then(text => assert.equal(text.search(/Ещё \d+ вариант(а|ов)/), 0, 'Текст кнопки не изменился'))
                .yaShouldBeVisible(PO.EduCardVariant.Container.ColLasts.Button(), false);
        });

        it('Фильтр по авторам', function() {
            return this.browser
                .click(PO.FiltersAuthorSelect())
                .yaShouldBeVisible(PO.FiltersAuthorPopup())
                .getText(PO.FiltersAuthorPopup.hovered())
                .then(text => assert.equal(text, 'Все авторы', 'Фильтр по умолчанию неверен'))
                .assertView('plain', PO.FiltersAuthorPopup())
                .click(PO.FiltersAuthorPopup.MenuSecond())
                .yaShouldBeVisible(PO.FiltersAuthorPopup(), false);
        });

        hermione.skip.in([/appium-chrome-phone/]);
        it('Фильтр по году', function() {
            return this.browser
                .click(PO.FiltersYearSelect())
                .yaShouldBeVisible(PO.FiltersYearPopup())
                .getText(PO.FiltersYearPopup.hovered())
                .then(text => assert.equal(text, '2020 год', 'Фильтр по умолчанию неверен'))
                .assertView('plain', PO.FiltersYearPopup(), {
                    invisibleElements: [PO.ManualAd()],
                })
                .click(PO.FiltersYearPopup.LastOption())
                .yaShouldBeVisible(PO.FiltersYearPopup(), false);
        });
    });

    describe('Список топиков', function() {
        beforeEach(function() {
            return this.browser.yaOpenPage(subjectUrl);
        });

        it('Проверка внешнего вида карточки', function() {
            return this.browser.assertView('plain', PO.TopicListCard());
        });

        hermione.skip.in([/./], 'https://st.yandex-team.ru/YOUNGLINGS-1943');
        it('Проверка работы расхлопа', function() {
            const link = '/tutor/subject/tag/problems/?ege_number_id=176&tag_id=19';

            return this.browser
                .click(PO.TopicListCard.Catalog.RowFirst.TopicNameFirst())
                .yaShouldBeVisible(PO.TopicListCard.CatalogSubs())
                .getText(PO.TopicListCard.Catalog.SubsFirst.SubSectionFirst.Link())
                .then(text => assert.equal(text, 'Все задания', 'Невверный текст пункта'))
                .pause(animationDuration)
                .yaShouldBeVisible(PO.TopicListCard.Catalog.SubsFirst.SubSectionFirst.StatusCount())
                .getText(PO.TopicListCard.Catalog.SubsFirst.SubSectionFirst.StatusCount())
                .then(text => assert.ok(text.includes('55')))
                .assertView('plain', PO.TopicListCard())
                .yaWaitChangeUrl(() => this.browser.click(PO.TopicListCard.Catalog.SubsFirst.SubSectionFirst.Link()))
                .yaCheckPageUrl(
                    link,
                    checkUrlParams,
                    ['ege_number_id', 'tag_id'],
                );
        });
    });

    describe('Карточка подписки', function() {
        it('Проверка внешнего вида карточка подписки', function() {
            const link = 'https://forms.yandex.ru/surveys/10010361/';

            return this.browser
                .yaOpenPage(subjectUrl)
                .assertView('plain', PO.PromoSubscribe())
                .getAttribute(
                    PO.PromoSubscribe.Btn(),
                    'target',
                )
                .then(text =>
                    assert.equal(text, '_blank', 'Не откроется в новой вкладке'),
                )
                .yaCheckLink(
                    PO.PromoSubscribe.Btn(),
                    link,
                    { ...checkUrlParams, skipQuery: true },
                );
        });
    });

    describe('Список уроков', function() {
        it('Проверка внешнего вида карточки со списком уроков', function() {
            return this.browser
                .yaOpenPage(subjectUrl)
                .assertView('plain', PO.LessonsListCard());
        });

        it('Переход на урок', function() {
            const link = '/tutor/subject/lesson/test/?lesson_id=5&subject_id=2';

            return this.browser
                .yaOpenPage(subjectUrl)
                .click(PO.LessonsListCard.Catalog.RowFirst.TopicNameFirst())
                .yaShouldBeVisible(PO.LessonsListCard.CatalogSubs())
                .assertView('expanded', [PO.LessonsListCard.Catalog.RowFirst.TopicNameFirst(), PO.LessonsListCard.CatalogSubs()])
                .yaWaitChangeUrl(() => this.browser.click(PO.LessonsListCard.Catalog.SubsFirst.SubSectionFirst.Link()))
                .yaCheckPageUrl(
                    link,
                    checkUrlParams,
                    ['lesson_id', 'subject_id'],
                );
        });
    });

    describe('Карточка с контрольными', function() {
        it('Проверка внешнего вида карточки с контрольными', function() {
            return this.browser
                .yaOpenPage(subjectUrl + '&server_time=1555718400')
                .assertView('plain', PO.StatGradSpecialVariants());
        });
    });

    describe('Рекомендации', function() {
        describe('Без авторизации', function() {
            beforeEach(function() {
                return this.browser
                    .yaOpenPage(subjectUrl)
                    .yaScroll(PO.RecommendedTasks())
                    .yaWaitForVisible(PO.RecommendedTasks.EmptyButton(), AJAX_TIMEOUT);
            });

            it('Блок онбординга', function() {
                return this.browser
                    .assertView('plain', [PO.RecommendedTasks(), PO.RecommendedTasks.EmptyButton()]);
            });

            it('Стартовый тест', function() {
                const link = 'https://yandex.ru/tutor/subject/variant/';

                return this.browser
                    .yaCheckLink(
                        PO.RecommendedTasks.EmptyButton(),
                        link,
                        { ...checkUrlParams, skipQuery: true },
                    );
            });
        });

        describe('С авторизацией', function() {
            const params = '&passport_uid=222355007&problems_count=5&check_only=1';

            beforeEach(function() {
                return this.browser
                    .yaLogin()
                    .yaOpenPage(subjectUrl.replace('subject_id=2', 'subject_id=1') + params)
                    .yaScroll(PO.RecommendedTasks())
                    .yaWaitForVisible(PO.RecommendedTasks.Button(), AJAX_TIMEOUT);
            });

            hermione.skip.in([/appium-chrome-phone|linux-chrome-iphone/]);
            it('Кнопка "Решить подборку"', function() {
                return this.browser
                    .click(PO.RecommendedTasks.Button())
                    // чтобы не было ховера на кнопке, перемещаем куда нибудь
                    .moveToObject(PO.RecommendedTasks())
                    .assertView('opened', [PO.RecommendedTasks(), PO.RecommendedTasks.Button()])
                    .click(PO.RecommendedTasks.Button())
                    .assertView('closed', [PO.RecommendedTasks(), PO.RecommendedTasks.Button()]);
            });

            hermione.skip.in([/appium-chrome-phone/]);
            it('Решение Верно', function() {
                const rightAnswer = -12;

                return this.browser
                    .click(PO.RecommendedTasks.Button())
                    .setValue(PO.RecommendedTasks.TaskControlLine.input(), rightAnswer)
                    .click(PO.RecommendedTasks.TaskControlLine.button())
                    .yaWaitForVisible(PO.RecommendedTasks.UserAttemptCol(), AJAX_TIMEOUT, 'результат решения задачи не отобразился')
                    .assertView('passed', [PO.RecommendedTasks(), PO.RecommendedTasks.Button()]);
            });

            it('Решение Неверно', function() {
                const wrongAnswer = 0;

                return this.browser
                    .click(PO.RecommendedTasks.Button())
                    .setValue(PO.RecommendedTasks.TaskControlLine.input(), wrongAnswer)
                    .click(PO.RecommendedTasks.TaskControlLine.button())
                    .yaWaitForVisible(PO.RecommendedTasks.UserAttemptCol(), AJAX_TIMEOUT, 'результат решения задачи не отобразился')
                    .assertView('failed', [PO.RecommendedTasks(), PO.RecommendedTasks.Button()]);
            });

            it('Решение Пропустить', function() {
                let prevImmediateResultText;

                return this.browser
                    .click(PO.RecommendedTasks.Button())
                    .getText(PO.RecommendedTasks.Task.Title())
                    .then(text => prevImmediateResultText = text)
                    .then(_ => skipRecommendedTask.bind(this, prevImmediateResultText));
            });

            it('Верный процент', function() {
                const firstRightTaskAnswer = 23;
                const firstExpectedImmediateResult = 'Решено 1 задание из 5 (верно – 100%)';
                const thirdWrongTaskAnswer = 'wrong';
                const thirdExpectedImmediateResult = 'Решено 2 задания из 5 (верно – 50%)';
                const fourthWrongTaskAnswer = 'wrong';
                const fourthExpectedImmediateResult = 'Решено 3 задания из 5 (верно – 33%)';
                const fifthRightTaskAnswer = '15,4';
                const fifthExpectedImmediateResult = 'Решено 4 задания из 5 (верно – 50%)';

                return this.browser
                    .click(PO.RecommendedTasks.Button())
                    .then(_ => checkRecommendedTask.bind(this, firstRightTaskAnswer, firstExpectedImmediateResult)())
                    .then(_ => skipRecommendedTask.bind(this, firstExpectedImmediateResult)())
                    .then(_ => checkRecommendedTask.bind(this, thirdWrongTaskAnswer, thirdExpectedImmediateResult)())
                    .then(_ => checkRecommendedTask.bind(this, fourthWrongTaskAnswer, fourthExpectedImmediateResult)())
                    .then(_ => checkRecommendedTask.bind(this, fifthRightTaskAnswer, fifthExpectedImmediateResult)())
                    .yaWaitForVisible(PO.RecommendedTasks.Result());
            });

            it('Карточка мини-репорта рекомендаций', function() {
                return this.browser
                    .click(PO.RecommendedTasks.Button())
                    .then(_ => skipRecommendedTask.bind(this)())
                    .then(_ => skipRecommendedTask.bind(this)())
                    .then(_ => skipRecommendedTask.bind(this)())
                    .then(_ => skipRecommendedTask.bind(this)())
                    .then(_ => skipRecommendedTask.bind(this)())
                    .yaWaitForVisible(PO.RecommendedTasks.Result())
                    .assertView('plain', PO.RecommendedTasks.Result());
            });
        });

        it('ПДД', function() {
            const params = '&passport_uid=222355007&problems_count=5&check_only=1';

            return this.browser
                .yaLogin()
                .yaOpenPage(subjectUrl.replace('subject_id=2', 'subject_id=30') + params)
                .yaScroll(PO.RecommendedTasks())
                .yaWaitForVisible(PO.RecommendedTasks.Button(), AJAX_TIMEOUT)
                .click(PO.RecommendedTasks.Button())
                .assertView('plain', [PO.RecommendedTasks(), PO.RecommendedTasks.Button()])
                .click(PO.RecommendedTasks.Task.AnswerButtons.secondButton())
                .click(PO.RecommendedTasks.TaskControlLine.button())
                .yaWaitForVisible(PO.RecommendedTasks.UserAttemptCol(), AJAX_TIMEOUT, 'результат решения задачи не отобразился')
                .then(_ => skipRecommendedTask.bind(this, 'Решено 1 задание из 5 (верно – 100%)')())
                .click(PO.RecommendedTasks.Task.AnswerButtons.secondButton())
                .click(PO.RecommendedTasks.TaskControlLine.button())
                .yaWaitForVisible(PO.RecommendedTasks.UserAttemptCol(), AJAX_TIMEOUT, 'результат решения задачи не отобразился')
                .then(_ => skipRecommendedTask.bind(this, 'Решено 2 задания из 5 (верно – 50%)')())
                .then(_ => skipRecommendedTask.bind(this)())
                .then(_ => skipRecommendedTask.bind(this)())
                .then(_ => skipRecommendedTask.bind(this)())
                .yaWaitForVisible(PO.RecommendedTasks.Result())
                .assertView('result', PO.RecommendedTasks.Result());
        });
    });
});
