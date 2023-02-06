const PO = require('./PO');

const USERNAME = 'robot-abc-002';

describe('Dispenser: Квоты', function() {
    for (const lang of ['ru', 'en']) {
        describe(lang, function() {
            describe('Сайдбар', function() {
                beforeEach(async function() {
                    const { browser } = this;

                    await browser
                        .openIntranetPage({
                            pathname: '/services/shifts223/folders',
                            query: {
                                noBlockingBeforeUnload: 1,
                                lang,
                            },
                        }, {
                            user: USERNAME,
                        })
                        .waitForVisible(PO.folderTable(), 15000)
                        .disableBeforeUnload();
                });
                describe('Из фолдера', function() {
                    beforeEach(async function() {
                        const { browser } = this;

                        await browser
                            .click(PO.folderTable.folderRow5.folderCell.sidebarIcon())
                            .waitForVisible(PO.folderSidebar(), 5000);
                    });

                    it('Внешний вид', async function() {
                        const { browser } = this;

                        await browser
                            .assertView(`sidebar-from-folder-${lang}`, PO.folderSidebar(), {
                                ignoreElements: [
                                    PO.toolsLamp(),
                                    PO.yndxBug(),
                                ],
                            });
                    });

                    it('Сворачивание/разворачивание аккаунтов', async function() {
                        const { browser } = this;

                        await browser
                            .click(PO.folderSidebar.accounts1.header.collapseButton())
                            .moveToObject(PO.folderSidebar.accounts1.header.collapseButton())
                            .assertView(`sidebar-accounts-collapsed-${lang}`, PO.folderSidebar(), {
                                ignoreElements: [
                                    PO.toolsLamp(),
                                    PO.yndxBug(),
                                ],
                            })
                            .click(PO.folderSidebar.accounts1.account1_mode_view.accordionChevron())
                            .assertView(`sidebar-accounts-first-expanded-${lang}`, PO.folderSidebar(), {
                                ignoreElements: [
                                    PO.toolsLamp(),
                                    PO.yndxBug(),
                                ],
                            })
                            .click(PO.folderSidebar.accounts1.header.expandButton())
                            .assertView(`sidebar-accounts-expanded-${lang}`, PO.folderSidebar(), {
                                ignoreElements: [
                                    PO.toolsLamp(),
                                    PO.yndxBug(),
                                ],
                            });
                    });
                });

                describe('Из провайдера', function() {
                    beforeEach(async function() {
                        const { browser } = this;

                        await browser
                            .click(PO.folderTable.folderRow3.providerRow1.providerCell.sidebarIcon())
                            .waitForVisible(PO.folderSidebar(), 5000);
                    });

                    it('Внешний вид', async function() {
                        const { browser } = this;

                        await browser
                            .assertView(`sidebar-from-provider-${lang}`, PO.folderSidebar(), {
                                ignoreElements: [
                                    PO.toolsLamp(),
                                    PO.yndxBug(),
                                ],
                            });
                    });

                    it('Форма спуска квот', async function() {
                        const { browser } = this;

                        await browser
                            .click(PO.folderSidebar.accounts1.account1_mode_view.changeButton())
                            .waitForVisible(PO.folderSidebar.accounts1.account1_mode_edit.editQuota(), 5000)
                            .assertView(
                                `quotas-form-${lang}`,
                                PO.folderSidebar.accounts1.account1_mode_edit(),
                            );
                    });

                    it('Форма создания аккаунта', async function() {
                        const { browser } = this;

                        await browser
                            .click(PO.folderSidebar.accounts1.header.addButton())
                            .waitForVisible(PO.folderSidebar.accounts1.accountCreationForm(), 5000)
                            .assertView(
                                `account-creation-form-${lang}`,
                                PO.folderSidebar.accounts1.accountCreationForm(),
                            );
                    });
                });
            });

            describe('Форма передачи квот', function() {
                it('Внешний вид', async function() {
                    const { browser } = this;

                    await browser
                        .openIntranetPage({
                            pathname: '/folders/transfers/create',
                            query: {
                                noBlockingBeforeUnload: 1,
                                lang,
                                variant: 'betweenFolders',
                                service_out: '3405',
                                folder_out: '9e2c90d4-19f5-42c0-bf23-02799699fadd',
                                service_in: '1357',
                                folder_in: 'c758629f-5465-4442-8b1b-b79231de6fc2',
                            },
                        }, {
                            user: USERNAME,
                        })
                        .waitForVisible(PO.transferForm.quotas(), 5000)
                        .disableBeforeUnload()
                        .assertView(`transfer-form-${lang}_before-provider-select`, PO.transferForm())
                        .setYCSelectValueByIndex(PO.transferForm.quotas.providerSelect(), 1)
                        .waitForVisible(PO.transferForm.quotas.table.head(), 5000)
                        .moveToObject(PO.transferForm.quotas.providerSelect())
                        .assertView(`transfer-form-${lang}_before-resource-select`, PO.transferForm())
                        .setYCSelectValueByIndex(PO.transferForm.quotas.providerSelect(), 1, 1)
                        .waitForVisible(PO.transferForm.quotas.table.resource(), 5000)
                        .assertView(`transfer-form-${lang}`, PO.transferForm());
                });
            });

            describe('Форма запроса квот из резерва', function() {
                it('Внешний вид', async function() {
                    const { browser } = this;

                    await browser
                        .openIntranetPage({
                            pathname: '/folders/transfers/create',
                            query: {
                                noBlockingBeforeUnload: 1,
                                lang,
                                variant: 'reserve',
                                provider: 'b9b52d73-86ed-4c54-98ab-ca6a64d6337b',
                                service_in: '1357',
                                folder_in: 'c758629f-5465-4442-8b1b-b79231de6fc2',
                            },
                        }, {
                            user: USERNAME,
                        })
                        .waitForVisible(PO.transferForm.quotas(), 5000)
                        .disableBeforeUnload()
                        .assertView(`reserve-form-${lang}_before-resource-select`, PO.transferForm())
                        .setYCSelectValueByIndex(PO.transferForm.quotas.providerSelect(), 1, 1)
                        .customSetValue(PO.transferForm.quotas.table.resource.deltaInput(), 1)
                        .click(PO.transferForm.quotas.addResourceButton())
                        .setYCSelectValueByIndex(PO.transferForm.quotas.providerSelect(), 1, 2)
                        .customSetValue(PO.transferForm.quotas.table.secondResource.deltaInput(), 2)
                        .assertView(`reserve-form-${lang}`, PO.transferForm())
                        .click(PO.transferForm.footer.submitButton())
                        .waitForVisible(PO.transferForm.errorMessage())
                        .assertView(`reserve-form-${lang}-errors`, PO.transferForm())
                        .click(PO.transferForm.quotas.table.resource.deleteButton())
                        .assertView(`reserve-form-${lang}-no-errors`, PO.transferForm());
                });
            });

            describe('Форма просмотра заявки', function() {
                it('Внешний вид', async function() {
                    const { browser } = this;

                    await browser
                        .openIntranetPage({
                            pathname: '/folders/transfers/dee835b6-fb44-49e1-862b-4a54087a0ee0',
                            query: {
                                noBlockingBeforeUnload: 1,
                                lang,
                            },
                        }, {
                            user: USERNAME,
                        })
                        .waitForVisible(PO.transferRequest(), 10000)
                        .disableBeforeUnload()
                        .assertView(`transfer-request-${lang}`, PO.transferRequest(), { screenshotDelay: 2000 });
                });
            });

            describe('Форма просмотра списка заявок', function() {
                it('Внешний вид', async function() {
                    const { browser } = this;

                    await browser
                        .openIntranetPage({
                            pathname: '/approves/quota-transfer',
                            query: {
                                noBlockingBeforeUnload: 1,
                                lang,
                            },
                        }, {
                            user: USERNAME,
                        })
                        .waitForVisible(PO.transferRequestsList(), 10000)
                        .assertView(
                            `transfer-requests-list-${lang}`,
                            PO.transferRequestsList(),
                            { allowViewportOverflow: true, compositeImage: false },
                        );
                });
            });
        });
    }
});
