describe('Эксперименты', function() {
    describe('Test-ids в url', function() {
        const testIds = [1, 4, 212, 3061, 2218, 1283];
        const testIdUrl = `/?param=34&test-id=${testIds.join('_')}`;

        beforeEach(function() {
            const { browser, PO } = this;

            return browser
                .yaOpenPage(testIdUrl)
                .yaWaitForVisible(PO.YndxBug())
                .click(PO.YndxBug())
                .click(PO.Tabs.AB())
                .yaWaitForVisible(PO.AB.CurExpTab())
                .click(PO.AB.CurExpTab())
                .yaWaitForVisible(PO.AB.CurExpPage.Input());
        });

        it('Внешний вид вкладки Текущие', function() {
            const { browser, PO } = this;

            return browser
                .yaAssertView('TestIdPage', PO.AB.CurExpPage());
        });

        it('Внешний вид вкладки Залипание', function() {
            const { browser, PO } = this;

            return browser
                .click(PO.AB.StuckTab())
                .yaWaitForVisible(PO.AB.StuckPage.Input())
                .yaAssertView('StuckPage', PO.AB.StuckPage());
        });

        it('Количество отображаемых экспериментов', function() {
            const { browser, PO } = this;

            return browser
                .yaVisibleCount(`${PO.AB.CurExpPage()} ${PO.UnstuckUrl()}`)
                .then(count => {
                    assert.isTrue(count === testIds.length,
                        `Должно отображаться ${testIds.length} экспериментов из url`);
                });
        });

        it('Удалить из url один эксперимент', function() {
            const { browser, PO } = this;

            let expectedTestIdUrl = '';
            const host = 'https://butterfly.ru';

            return browser
                .getAttribute(`${PO.AB.CurExpPage()} ${PO.UnstuckUrl()}`, 'data-test-id')
                .then(expIds => {
                    const curExp = Number(expIds[0]);
                    const queryExps = testIds.filter(id => id !== curExp).join('_');
                    expectedTestIdUrl = `${host}/?param=34&test-id=${queryExps}`;
                })
                .click(`${PO.AB.CurExpPage()} ${PO.UnstuckUrl()}`)
                .yaWaitForLoadPage()
                .getUrl()
                .then(url => {
                    const noUknownQueriesUrl = url.replace(/&tpid=.*/, '');
                    return browser
                        .yaCheckURL(noUknownQueriesUrl, expectedTestIdUrl, '',
                            { skipProtocol: true, skipHostname: true, skipPathname: true }
                        );
                });
        });

        it('Удалить из url все эксперименты', function() {
            const { browser, PO } = this;

            const expectedTestIdUrl = 'https://butterfly.ru/?param=34';

            return browser
                .click(PO.AB.UnstuckAllBtn())
                .yaWaitForLoadPage()
                .getUrl()
                .then(url => {
                    const noUknownQueriesUrl = url.replace(/&tpid=.*/, '');
                    return browser
                        .yaCheckURL(noUknownQueriesUrl, expectedTestIdUrl, '',
                            { skipProtocol: true, skipHostname: true, skipPathname: true }
                        );
                });
        });
    });
});
