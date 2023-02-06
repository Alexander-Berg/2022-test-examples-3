describe('Эксперименты', function() {
    describe('Залипание', function() {
        beforeEach(function() {
            const { browser, PO } = this;

            return browser
                .yaOpenPage('/')
                .yaWaitForVisible(PO.YndxBug())
                .click(PO.YndxBug())
                .click(PO.Tabs.AB())
                .yaWaitForVisible(PO.AB.StuckPage());
        });

        it('Количество экспериментов на старте', function() {
            const { browser, PO } = this;

            return browser
                .yaVisibleCount(PO.Exp())
                .then(count => {
                    assert.isTrue(count === 0, 'Не должно быть экспериментов');
                });
        });

        it('Залипнуть в чистом яндексе', function() {
            const { browser, PO } = this;

            return browser
                .setValue(PO.AB.StuckPage.Input(), 'Чистый Яндекс')
                .yaWaitForVisible(PO.ExpList.CleanYandexBtn())
                .click(PO.ExpList.CleanYandexBtn())
                .yaWaitForVisible(PO.ExpStuck.CleanYandexBtn())
                .yaWaitForHidden(PO.ExpList.CleanYandexBtn())
                .yaCheckCookie()
                .yaCheckExpIds([1]);
        });

        it('Отлипнуть от чистого яндекса', function() {
            const { browser, PO } = this;

            return browser
                .setValue(PO.AB.StuckPage.Input(), 'Чистый Яндекс')
                .yaWaitForVisible(PO.ExpList.CleanYandexBtn())
                .click(PO.ExpList.CleanYandexBtn())
                .yaWaitForVisible(PO.ExpStuck.CleanYandexBtn())
                .yaCheckExpIds([1])
                .click(PO.ExpStuck.CleanYandexBtn())
                .yaWaitForHidden(PO.ExpStuck.CleanYandexBtn())
                .yaCheckCookie(false)
                .yaCheckExpIds([1], false, false)
                .yaShouldBeVisible(PO.ExpList.CleanYandexBtn());
        });

        it('Отлипнуть от всех', function() {
            const { browser, PO } = this;

            return browser
                .setValue(PO.AB.StuckPage.Input(), '1')
                .yaWaitForVisible(PO.ExpList())
                .click(`${PO.FirstExp()} ${PO.StuckBtn()}`)
                .click(`${PO.ThirdExp()} ${PO.StuckBtn()}`)
                .click(`${PO.LastExp()} ${PO.StuckBtn()}`)
                .yaWaitForVisible(PO.ExpStuck.ExpList())
                .click(PO.AB.UnstuckAllBtn())
                .yaWaitForHidden(PO.ExpStuck.ExpList())
                .yaCheckCookie(false)
                .yaCheckExpIds([], false, false);
        });

        it('Скрыть залипнутые эксперименты', function() {
            const { browser, PO } = this;

            return browser
                .setValue(PO.AB.StuckPage.Input(), '1')
                .yaWaitForVisible(PO.FirstExp())
                .click(`${PO.FirstExp()} ${PO.StuckBtn()}`)
                .yaWaitForVisible(PO.ExpStuck.ExpList())
                .click(PO.ExpStuck.HideBtn())
                .yaShouldNotBeVisible(PO.ExpStuck.ExpList());
        });

        it('Показать залипнутые эксперименты', function() {
            const { browser, PO } = this;

            const stuckedExps = [];

            return browser
                .setValue(PO.AB.StuckPage.Input(), '1')
                .yaWaitForVisible(PO.FirstExp())
                .getAttribute(`${PO.AB.StuckPage()} ${PO.FirstExp()} ${PO.StuckBtn()}`, 'data-test-id')
                .then(expId => {
                    stuckedExps.push(expId);
                })
                .click(`${PO.FirstExp()} ${PO.StuckBtn()}`)
                .yaWaitForVisible(PO.ExpStuck.ExpList())
                .click(PO.ExpStuck.HideBtn())
                .yaShouldNotBeVisible(PO.ExpStuck.ExpList())
                .click(PO.ExpStuck.HideBtn())
                .yaWaitForVisible(PO.ExpStuck.ExpList())
                .getAttribute(`${PO.AB.StuckPage()} ${PO.FirstExp()} ${PO.UnstuckBtn()}`, 'data-test-id')
                .then(expId => {
                    assert.isTrue(stuckedExps.includes(expId),
                        'В залипнутых отображается не тот эксперимент, в котором залипали');
                });
        });
    });
});
