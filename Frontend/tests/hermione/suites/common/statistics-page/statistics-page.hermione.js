const animationDuration = 200;

describe('Страница статистики', () => {
    describe('Нет данных статистики', () => {
        beforeEach(function() {
            return this.browser
                .yaOpenPage(
                    '/user/statistics/?passport_uid=hermione_test-run_empty'
                )
                .getText(PO.UserStatistics.Tab.activeButton())
                .then(text =>
                    assert.equal(text.trim(), 'По заданиям', 'Неверный пункт меню')
                );
        });

        it('Статистика заданий', function() {
            return this.browser
                .click(PO.UserStatistics.TabLastButton())
                .getText(PO.UserStatistics.Tab.activeButton())
                .then(text =>
                    assert.equal(text, 'По заданиям', 'Неверный пункт меню')
                )
                .getText(PO.UserStatistics.EmptyScreen.Text())
                .then(text =>
                    assert.equal(
                        text,
                        'Не нашлось никаких данных',
                        'Не должно быть данных'
                    )
                )
                .yaShouldBeVisible(PO.UserStatistics.Filters(), false)
                .assertView('plain', PO.UserStatistics())
            ;
        });

        it('Статистика вариантов', function() {
            return this.browser
                .click(PO.UserStatistics.TabFirstButton())
                .getText(PO.UserStatistics.Tab.activeButton())
                .then(text =>
                    assert.equal(text.trim(), 'По вариантам', 'Неверный пункт меню') // в edge17 в строке остаются переводы строк
                )
                .getText(PO.UserStatistics.EmptyScreen.Text())
                .then(text =>
                    assert.equal(
                        text,
                        'Не нашлось никаких данных',
                        'Не должно быть данных'
                    )
                )
                .yaShouldBeVisible(PO.UserStatistics.Filters(), false)
                .assertView('plain', PO.UserStatistics())
            ;
        });
    });

    describe('Статистика множества заданий', () => {
        beforeEach(function() {
            return this.browser
                .yaOpenPage(
                    '/user/statistics/?passport_uid=hermione_test-run_first'
                )
                .getText(PO.UserStatistics.Tab.activeButton())
                .then(text =>
                    assert.equal(text.trim(), 'По заданиям', 'Неверный пункт меню')
                );
        });

        it('Проверка внешнего вида', function() {
            return this.browser.assertView('plain', PO.UserStatistics());
        });

        it('Выбор предмета', function() {
            let filterItemText;

            return (
                this.browser
                    .click((PO.UserStatistics.Filters.control()))
                    .pause(animationDuration)
                    .assertView('plain', PO.UserStatistics())
                    // сохранить текст пункта меню
                    .getText(PO.FilterSecond())
                    .then(text => {
                        filterItemText = text.trim();
                    })
                    .click(PO.FilterSecond())
                    .getText(PO.UserStatistics.Tab.activeButton())
                    .then(text =>
                        assert.equal(text.trim(), 'По заданиям', 'Неверный пункт меню')
                    )
                    // проверить на соответствие активного и выбранного
                    .getText(PO.UserStatistics.Filters.control())
                    .then(text =>
                        assert.equal(
                            text.trim(),
                            filterItemText,
                            'Неверный пункт меню'
                        )
                    )
                    .yaWaitUntil('страница не обновилась', () => {
                        return this.browser
                            .isExisting(PO.UserStatisticsPendingScreenSuccess());
                    }, 3000)
                    .pause(animationDuration)
                    .getText(PO.UserStatistics.StatRowFirstRow.Identifier())
                    .then(text =>
                        assert.equal(text.trim(), '#T7468', 'Неожиданная задача')
                    )
            );
        });

        it('Кол-во попыток решения задачи', function() {
            let filterItemText;

            return this.browser
                .click((PO.UserStatistics.Filters.control()))
                .getText(PO.FilterFirst())
                .then(text => {
                    filterItemText = text;
                })
                .click(PO.FilterFirst())
                .getText(PO.UserStatistics.Filters.control())
                .then(text =>
                    assert.equal(
                        text.trim(),
                        filterItemText,
                        'Неверный пункт меню'
                    )
                )
                .getText(PO.UserStatistics.Tab.activeButton())
                .then(text =>
                    assert.equal(text.trim(), 'По заданиям', 'Неверный пункт меню')
                )
                .getText(PO.UserStatistics.StatRowFirstRow.Identifier())
                .then(text =>
                    assert.equal(text.trim(), '#T7397', 'Неожиданная задача')
                )
                .assertView('plain', PO.UserStatistics())
                .getText(PO.UserStatistics.StatRowSecondRow.YourAnswer())
                .then(text =>
                    assert.notEqual(text.trim(), '', 'Нет ответа пользователя')
                )
                .yaShouldBeVisible(PO.UserStatistics.StatRowSecondRow.Control(), false)
                .getText(PO.UserStatistics.StatRowFirstRow.YourAnswer())
                .then(text =>
                    assert.notEqual(text.trim(), '', 'Нет ответа пользователя')
                )
                .yaShouldBeVisible(PO.UserStatistics.StatRowFirstRow.Control(), true)
                .click(PO.UserStatistics.StatRowFirstRow.Control())
                .pause(animationDuration)
                .yaCheckCount(PO.TutorTable.Others(), 1, 'неверное количество')
                .assertView('plain2', PO.UserStatistics())
            ;
        });

        it('Ссылка на задачу', function() {
            const taskLink =
                '/tutor/subject/problem/?passport_uid=hermione_test-run_first&problem_id=T7397';
            return this.browser
                .getAttribute(
                    PO.UserStatistics.StatRowFirstRow.Identifier.link(),
                    'target'
                )
                .then(text =>
                    assert.equal(text, '_blank', 'Не откроется в новой вкладке')
                )
                .yaCheckLink(
                    PO.UserStatistics.StatRowFirstRow.Identifier.link(),
                    taskLink,
                    {
                        skipProtocol: true,
                        skipHostname: true,
                    },
                    ['problem_id']
                )
            ;
        });

        it('Кнопка показать еще', function() {
            let filterItemText;

            return this.browser
                .click((PO.UserStatistics.Filters.control()))
                .getText(PO.FilterFirst())
                .then(text => {
                    filterItemText = text;
                })
                .click(PO.FilterFirst())
                .getText(PO.UserStatistics.Filters.control())
                .then(text =>
                    assert.equal(
                        text.trim(),
                        filterItemText,
                        'Неверный пункт меню'
                    )
                )
                .getText(PO.UserStatistics.StatRowFirstRow.Identifier())
                .then(text =>
                    assert.equal(text, '#T7397', 'Неожиданная задача')
                )
                .yaCheckCount(PO.TutorTable.Row(), 11, 'неверное количество')
                .yaShouldBeVisible(PO.UserStatistics.More(), false)
                .click((PO.UserStatistics.Filters.control()))
                .getText(PO.FilterThird())
                .then(text => {
                    filterItemText = text;
                })
                .click(PO.FilterThird())
                .getText(PO.UserStatistics.Filters.control())
                .then(text =>
                    assert.equal(
                        text.trim(),
                        filterItemText,
                        'Неверный пункт меню'
                    )
                )
                .yaWaitUntil('страница не обновилась', () => {
                    return this.browser
                        .isExisting(PO.UserStatisticsPendingScreenSuccess());
                }, 3000)
                .pause(animationDuration)
                .getText(PO.UserStatistics.StatRowFirstRow.Identifier())
                .then(text =>
                    assert.equal(text.trim(), '#T7192', 'Неожиданная задача')
                )
                .yaCheckCount(PO.TutorTable.Row(), 20, 'неверное количество')
                .yaShouldBeVisible(PO.UserStatistics.More(), true)
                .assertView('plain', PO.UserStatistics())
                .click(PO.UserStatistics.More.Button())
                .yaWaitForHidden(PO.UserStatistics.More.Button(), 3000, 'страница не обновилась')
                .pause(animationDuration)
                .yaCheckCount(PO.TutorTable.Row(), 26, 'неверное количество')
                .assertView('plain2', PO.UserStatistics())
            ;
        });
    });

    describe('Статистика множества вариантов', () => {
        let filterItemText;

        beforeEach(function() {
            return this.browser
                .yaOpenPage(
                    '/user/statistics/?passport_uid=hermione_test-run_second'
                )
                .getText(PO.UserStatistics.Tab.activeButton())
                .then(text =>
                    assert.equal(text.trim(), 'По заданиям', 'Неверный пункт меню')
                )
                .click(PO.UserStatistics.TabFirstButton());
        });

        it('Выбор предмета', function() {
            return this.browser
                .click((PO.UserStatistics.Filters.control()))
                .pause(animationDuration)
                .assertView('plain', PO.menu())
                .getText(PO.FilterFirst())
                .then(text => {
                    filterItemText = text;
                })
                .click(PO.FilterFirst())
                .getText(PO.UserStatistics.Tab.activeButton())
                .then(text =>
                    assert.equal(text.trim(), 'По вариантам', 'Неверный пункт меню')
                )
                // проверить на соответствие активного и выбранного
                .getText(PO.UserStatistics.Filters.control())
                .then(text =>
                    assert.equal(
                        text.trim(),
                        filterItemText,
                        'Неверный пункт меню'
                    )
                )
                .getText(PO.UserStatistics.StatRowFirstRow.link())
                .then(text =>
                    assert.equal(text.trim(), '129', 'Неожиданная задача')
                );
        });

        it('Кнопка показать еще', function() {
            return this.browser
                .click((PO.UserStatistics.Filters.control()))
                .getText(PO.FilterSecond())
                .then(text => {
                    filterItemText = text;
                })
                .click(PO.FilterSecond())
                .getText(PO.UserStatistics.Filters.control())
                .then(text =>
                    assert.equal(
                        text.trim(),
                        filterItemText,
                        'Неверный пункт меню'
                    )
                )
                .yaWaitUntil('страница не обновилась', () => {
                    return this.browser
                        .isExisting(PO.UserStatisticsPendingScreenSuccess());
                }, 3000)
                .pause(animationDuration)
                .yaCheckCount(PO.UserStatistics.StatRow(), 1, 'неверное количество')
                .yaShouldBeVisible(PO.UserStatistics.More(), false)
                // больше 20
                .click((PO.UserStatistics.Filters.control()))
                .getText(PO.FilterFirst())
                .then(text => {
                    filterItemText = text;
                })
                .click(PO.FilterFirst())
                .getText(PO.UserStatistics.Filters.control())
                .then(text =>
                    assert.equal(
                        text.trim(),
                        filterItemText,
                        'Неверный пункт меню'
                    )
                )
                .yaWaitUntil('страница не обновилась', () => {
                    return this.browser
                        .isExisting(PO.UserStatisticsPendingScreenSuccess());
                }, 3000)
                .yaCheckCount(PO.UserStatistics.StatRow(), 20, 'неверное количество')
                .yaShouldBeVisible(PO.UserStatistics.More(), true)
                .assertView('plain', PO.UserStatistics())
                .click(PO.UserStatistics.More.Button())
                .yaWaitForHidden(PO.UserStatistics.More.Button(), 3000, 'страница не обновилась')
                .pause(animationDuration)
                .yaCheckCount(PO.TutorTable.Row(), 22, 'неверное количество')
                .assertView('plain2', PO.UserStatistics())
            ;
        });

        it('Ссылка на репорт', function() {
            const taskLink =
                '/tutor/subject/variant/report/?passport_uid=hermione_test-run_second&report_id=d2a125b495fcc25a0865f9491a09c0c1';

            return this.browser
                .yaWaitChangeUrl(() => this.browser.click(PO.UserStatistics.StatRowFirstRow.link()))
                .yaCheckPageUrl(
                    taskLink,
                    { skipProtocol: true, skipHostname: true },
                    ['report_id']
                )
            ;
        });
    });
});
