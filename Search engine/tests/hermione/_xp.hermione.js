const VALIDATION_TIMEOUT = 5000;

describe('_xp', function () {
    // const listViewWaitForFetching = async function (browser) {
    //     const firstExperimentInTable =
    //         '/html/body/section/div[2]/div/div[2]/div[2]';
    //     await browser.waitForExist(firstExperimentInTable, 10000);
    // };

    const basketViewWaitForFetching = async function (browser) {
        const title = '/html/body/section/div[1]/div/div[1]/div/h1';
        await browser.waitForExist(title, 10000);
    };

    beforeEach(async function () {
        await this.browser.url('/xp');
    });

    // const experimentIgnoreElements = ['.Experiment-Duration'];

    /**
     *  TODO можно реализовать на реактовой странице через ignoreElements
     *   (на странице со списком экспериментов обновляется столбец duration в зависимости от текущего времени,
     *   и всегда получается diff в скриншоте)
     */
    // it('should open list', async function () {
    //     await listViewWaitForFetching(this.browser);
    //     await this.browser.pause(2000);
    //     await this.browser.assertBodyView('_xp-list', {
    //         ignoreElements: experimentIgnoreElements,
    //     });
    //
    //     await this.browser
    //         .$('[data-test-id="owner-or-responsible-toggle"]')
    //         .click();
    //     await this.browser.assertBodyView(
    //         '_xp-list-owner-or-responsible-popup',
    //         {
    //             ignoreElements: experimentIgnoreElements,
    //         },
    //     );
    //
    //     await this.browser.$('[data-test-id="status-filter"]').click();
    //     await this.browser.assertBodyView('_xp-list-status-popup', {
    //         ignoreElements: experimentIgnoreElements,
    //     });
    //
    //     await this.browser.$('[data-test-id="age-filter"]').click();
    //     await this.browser.assertBodyView('_xp-list-age-popup', {
    //         ignoreElements: experimentIgnoreElements,
    //     });
    //
    //     await this.browser.$('[data-test-id="source-filter"]').click();
    //     await this.browser.assertBodyView('_xp-list-source-popup', {
    //         ignoreElements: experimentIgnoreElements,
    //     });
    // });
    //
    // it('should open list with params', async function () {
    //     await this.browser.url(
    //         '/xp/?status=COMPLETED&status=FAILED&days=90&source=NO_MLM&ownerOrResponsible=robot-uno',
    //     );
    //     await listViewWaitForFetching(this.browser);
    //     await this.browser.pause(2000);
    //     await this.browser.assertBodyView('_xp-list-with-params', {
    //         ignoreElements: experimentIgnoreElements,
    //     });
    //
    //     await this.browser.$('[data-test-id="reset-filters"]').click();
    //     const url = new URL(await this.browser.getUrl());
    //     assert.equal(
    //         (url.pathname + url.search + url.hash).replace(/\/$/, ''),
    //         '/xp',
    //     );
    // });
    //
    // it('should open list with params - 2', async function () {
    //     await this.browser.url(
    //         '/xp?source=MLM_ONLY&owner=robot-blendr-priemka&status=RUNNING&days=365',
    //     );
    //     await this.browser.pause(2000);
    //     await this.browser.assertBodyView('_xp-list-with-params-2', {
    //         ignoreElements: experimentIgnoreElements,
    //     });
    //
    //     await this.browser.$('[data-test-id="reset-filters"]').click();
    //     const url = new URL(await this.browser.getUrl());
    //     assert.equal(
    //         (url.pathname + url.search + url.hash).replace(/\/$/, ''),
    //         '/xp',
    //     );
    // });

    it('should open view - 1', async function () {
        await this.browser.url('/xp/016bae54fc6a723cb95148c4e36c4cb6');
        await basketViewWaitForFetching(this.browser);
        await this.browser.assertFullPageView('_xp-view-1', {
            screenshotDelay: 5000,
        });
    });

    it('should open view - 2', async function () {
        await this.browser.url('/xp/017ffaa0bde304efd3a9776fd9491da4');
        await basketViewWaitForFetching(this.browser);
        await this.browser.assertFullPageView('_xp-view-2', {
            screenshotDelay: 5000,
        });
    });

    it('should open clone view - 1', async function () {
        await this.browser.url('/xp/016bae54fc6a723cb95148c4e36c4cb6');
        await this.browser.url('/xp/016bae54fc6a723cb95148c4e36c4cb6/clone');
        await basketViewWaitForFetching(this.browser);
        await this.browser.pause(2000);
        await this.browser.assertFullPageView('_xp-clone-view-1', {
            closeAllToasts: true,
            screenshotDelay: 5000,
        });
    });

    it('should open clone view - 2', async function () {
        await this.browser.url('/xp/017ffaa0bde304efd3a9776fd9491da4');
        await this.browser.url('/xp/017ffaa0bde304efd3a9776fd9491da4/clone');
        await basketViewWaitForFetching(this.browser);
        await this.browser.pause(2000);
        await this.browser.assertFullPageView('_xp-clone-view-2', {
            closeAllToasts: true,
            screenshotDelay: 5000,
        });
    });

    const openHostsView = async browser => {
        await browser.url('/xp/017ffaa0bde304efd3a9776fd9491da4');
        await browser.url('/xp/017ffaa0bde304efd3a9776fd9491da4/clone');
        await basketViewWaitForFetching(browser);

        await browser.$('[data-test-id=expandableBlock]').click();
        await browser.pause(VALIDATION_TIMEOUT);
    };

    const disableFastsTemplateButton =
        '/html/body/section/div[2]/div/div[2]/div/div[2]/div[3]/div[7]/div/div[2]/div/button[1]';
    const disableGarbageTemplateButton =
        '/html/body/section/div[2]/div/div[2]/div/div[2]/div[3]/div[7]/div/div[2]/div/button[2]';
    const cgiParametersTextarea =
        '/html/body/section/div[2]/div/div[2]/div/div[2]/div[3]/div[8]/div/div[2]/div/textarea';

    it('hosts view', async function () {
        await openHostsView(this.browser);
        await this.browser.assertFullPageView('_xp-view-hosts', {
            selector: '[data-test-id=CroneFormHostSection]',
            closeAllToasts: true,
            screenshotDelay: 5000,
        });
    });

    it('hosts view - templates click', async function () {
        await openHostsView(this.browser);
        await this.browser.$(disableFastsTemplateButton).click();
        await this.browser.$(disableGarbageTemplateButton).click();
        await this.browser.pause(VALIDATION_TIMEOUT);
        await this.browser.assertFullPageView('_xp-view-hosts-with-templates', {
            selector: '[data-test-id=CroneFormHostSection]',
            closeAllToasts: true,
            screenshotDelay: 5000,
        });

        await this.browser.$(disableFastsTemplateButton).click();
        await this.browser.$(disableGarbageTemplateButton).click();
        await this.browser.pause(VALIDATION_TIMEOUT);
        await this.browser.assertFullPageView(
            '_xp-view-hosts-cancel-templates',
            {
                selector: '[data-test-id=CroneFormHostSection]',
                closeAllToasts: true,
                screenshotDelay: 5000,
            },
        );
    });

    it('hosts view - manually added template', async function () {
        await openHostsView(this.browser);
        await this.browser.setValue(cgiParametersTextarea, 'rearr=tiermask=1');
        await this.browser.$('[data-test-id=CroneFormHostSection]').click();
        await this.browser.pause(VALIDATION_TIMEOUT);
        await this.browser.assertFullPageView(
            '_xp-view-hosts-manually-added-template',
            {
                selector: '[data-test-id=CroneFormHostSection]',
                closeAllToasts: true,
                screenshotDelay: 5000,
            },
        );
    });
});
