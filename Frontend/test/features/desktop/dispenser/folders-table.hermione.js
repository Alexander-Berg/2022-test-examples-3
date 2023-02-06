const PO = require('./PO');

const USERNAME = 'robot-abc-002';

describe('Dispenser: Квоты', function() {
    for (const lang of ['ru', 'en']) {
        describe(lang, function() {
            describe('Таблица фолдеров', function() {
                beforeEach(async function() {
                    const { browser } = this;

                    await browser
                        .openIntranetPage({
                            pathname: '/services/shifts223/folders',
                            query: { lang },
                        }, {
                            user: USERNAME,
                        })
                        .waitForVisible(PO.folderTable(), 15000);
                });

                it('Внешний вид', async function() {
                    const { browser } = this;

                    await browser
                        .assertView(`folder-table-${lang}`, PO.folderTable());
                });

                it('Тип ресурса - Стрелка', async function() {
                    const { browser } = this;

                    await browser
                        .assertView(`folder-table-row-before-${lang}`, PO.folderTable.folderRow3())
                        .click(PO.folderTable.folderRow3.providerRow1.resourceRow2.rootResource.arrow())
                        .moveToObject(PO.folderTable())
                        .assertView(`folder-table-row-after-${lang}`, PO.folderTable.folderRow3());
                });

                describe('Сворачивание/разворачивание таблицы фолдеров', function() {
                    const assertVisiblePartOptions = { allowViewportOverflow: true, compositeImage: false };

                    it('фолдера', async function() {
                        const { browser } = this;

                        await browser
                            .scroll(PO.folderTable.folderRow5(), 0, -100)
                            .assertView('folder-table-folder-row-expanded', PO.folderTable.folderRow5())
                            .click(PO.folderTable.folderRow5.folderCell.chevron())
                            .waitForVisible(PO.folderTable.folderRow5Collapsed())
                            .moveToObject(PO.folderTable.header())
                            .assertView('folder-table-folder-row-collapsed', PO.folderTable.folderRow5());
                    });

                    it('провайдера', async function() {
                        const { browser } = this;

                        await browser
                            .scroll(PO.folderTable.folderRow3(), 0, -100)
                            .assertView('folder-table-folder-row-provider-expanded', PO.folderTable.folderRow3())
                            .click(PO.folderTable.folderRow3.providerRow1.providerCell.chevron())
                            .waitForVisible(PO.folderTable.folderRow3.providerRow1Collapsed())
                            .moveToObject(PO.folderTable.header())
                            .assertView('folder-table-folder-row-provider-collapsed', PO.folderTable.folderRow3());
                    });

                    it('всех фолдеров', async function() {
                        const { browser } = this;

                        await browser
                            .click(PO.folderTable.header.folderCell.collapseButton())
                            .waitForVisible(PO.folderTable.folderRow5Collapsed())
                            .assertView('folder-table-folders-collapsed', PO.folderTable(), assertVisiblePartOptions)
                            .click(PO.folderTable.header.folderCell.expandButton())
                            .assertView('folder-table-folders-expanded', PO.folderTable(), assertVisiblePartOptions);
                    });

                    it('всех провайдеров', async function() {
                        const { browser } = this;

                        await browser
                            .click(PO.folderTable.header.providerCell.collapseButton())
                            .waitForVisible(PO.folderTable.folderRow3.providerRow1Collapsed())
                            .assertView('folder-table-providers-collapsed', PO.folderTable(), assertVisiblePartOptions)
                            .click(PO.folderTable.header.providerCell.expandButton())
                            .assertView('folder-table-providers-expanded', PO.folderTable(), assertVisiblePartOptions);
                    });
                });
            });
        });
    }
});
