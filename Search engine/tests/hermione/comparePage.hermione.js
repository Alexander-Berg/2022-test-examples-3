describe('ComparePage', function () {
    beforeEach(async function () {
        await this.browser.execute(function () {
            window.localStorage.setItem(
                '_use_beta',
                JSON.stringify(['ComparePage']),
            );
        });
    });

    const addEnrichmentButton = '[data-test-id=addEnrichment]';

    it('should open empty', async function () {
        await this.browser.url('/compare');
        await this.browser.waitForFetching();
        await this.browser.assertBodyView('ComparePage-calculations-empty');

        await this.browser.waitAndClick('[data-test-id=enrichmentsTab]');
        await this.browser.assertBodyView('ComparePage-enrichments-empty');
    });

    it('should open 1 serpset', async function () {
        await this.browser.url(
            '/compare?regional=RU&evaluation=WEB&aspect=default&serpset=36102585&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();
        await this.browser.assertFullPageView(
            'ComparePage-calculations-1-serpset',
        );
    });

    it('should open 5 serpsets', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36778029&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );

        await this.browser.waitForFetching();
        await this.browser.assertFullPageView(
            'ComparePage-calculations-5-serpsets',
        );
    });

    it('should open with metric search', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36778029&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&metric=v1',
        );

        await this.browser.waitForFetching();
        await this.browser.assertFullPageView(
            'ComparePage-calculations-metric-search',
        );
    });

    it('should open metric link', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36778029&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers#judged-sinsig-kc-no-turbo-judgement-values-5',
        );

        await this.browser.waitForFetching();
        await this.browser.assertFullPageView(
            'ComparePage-calculations-metric-link',
        );
    });

    it('should open with default component filters', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=IMAGES&aspect=default&pre-filter=db-6&pre-filter=country-UA&pre-filter=region-143&serpset=33891175&serpset=29641874',
        );

        await this.browser.waitForFetching();
        await this.browser.assertFullPageView(
            'ComparePage-calculations-default-component-filters',
        );
    });

    hermione.skip.in(/.*/, 'show screenshot diff but no change');
    it('should open addSerpset modal', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=IMAGES&aspect=default&pre-filter=db-6&pre-filter=country-UA&pre-filter=region-143&serpset=33891175&serpset=29641874',
        );

        await this.browser.waitForFetching();
        await this.browser.pause(1000);
        await this.browser.waitAndClick(
            '[data-test-id=SerpsetModalAddSerpset]',
        );
        await this.browser.pause(1000);
        await this.browser.waitAndClick('[data-test-id=UserSuggestMeButton]');
        await this.browser.waitForFetching();
        await this.browser.assertBodyView('ComparePage-addSerpset-modal');
    });

    it('should expand group', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36778029&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();
        await this.browser.waitAndClick('[data-test-id=diffGroup]');
        await this.browser.assertFullPageView(
            'ComparePage-calculations-expanded-group',
        );
    });

    it('should show metric controls', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=IMAGES&aspect=default&serpset=29685379&serpset=29685379&serpset-filter=onlySearchResult&serpset-filter=onlySearchResult',
        );
        await this.browser.waitForFetching();
        await this.browser.waitForExist(addEnrichmentButton, 10000);
        await this.browser.moveToObject(addEnrichmentButton);
        await this.browser.assertBodyView(
            'ComparePage-calculations-metric-controls',
        );
    });

    it('should add enrichment', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=IMAGES&aspect=default&serpset=29685379&serpset=29685379&serpset-filter=onlySearchResult&serpset-filter=onlySearchResult',
        );
        await this.browser.waitForFetching();
        await this.browser.waitForExist(addEnrichmentButton, 10000);
        await this.browser.pause(1000);
        await this.browser.moveToObject(addEnrichmentButton);
        await this.browser.$(addEnrichmentButton).click();
        await this.browser.waitForExist('[data-test-id=startTasks]', 10000);
        await this.browser.assertFullPageView(
            'ComparePage-enrichments-added-from-calculations',
        );
    });

    it('should add enrichment with custom fields', async function () {
        await this.browser.url(
            '/compare?regional=RU&evaluation=WEB&aspect=default&serpset=30681692&serpset-filter=onlySearchResult',
        );
        await this.browser.waitForFetching();
        await this.browser.waitForExist(addEnrichmentButton, 10000);
        await this.browser.pause(1000);
        await this.browser.moveToObject(addEnrichmentButton);
        await this.browser.$(addEnrichmentButton).click();
        await this.browser.waitForExist('[data-test-id=startTasks]', 10000);
        await this.browser.assertFullPageView(
            'ComparePage-enrichments-custom-fields',
        );
    });

    it('should open enrichments modal', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=29942609&serpset=30043517&serpset-filter=onlySearchResult&serpset-filter=onlySearchResult',
        );
        await this.browser.waitForFetching();
        await this.browser.waitAndClick(
            '[data-test-id=enrichmentsTabAddButton]',
        );
        await this.browser.waitForFetching();
        await this.browser.assertBodyView('ComparePage-enrichments-modal');
    });

    it('should open enrichments tab', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=29942609&serpset=30043517&serpset-filter=onlySearchResult&serpset-filter=onlySearchResult',
        );
        await this.browser.waitForFetching();
        await this.browser.waitAndClick('[data-test-id=enrichmentsTab]');
        await this.browser.assertFullPageView('ComparePage-enrichments');
    });

    hermione.skip.in(/.*/, 'element still not existing after 20000ms');
    it('should expand enrichment', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=38007895&serpset=38007911&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();
        await this.browser.waitAndClick('[data-test-id=enrichmentsTab]');
        await this.browser.waitAndClick('[data-test-id=listItem]');
        await this.browser.assertFullPageView(
            'ComparePage-enrichments-expanded',
        );
    });

    it('should open details', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=35816935&serpset=33273608&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();
        await this.browser.waitAndClick('[data-test-id=serpsetsInfoDetails]');
        await this.browser.assertBodyView('ComparePage-serpsetsInfo-details');
        const {value: localStorageValue} = await this.browser.execute(() =>
            window.localStorage.getItem('compare_page'),
        );
        assert.equal(JSON.parse(localStorageValue).showDetails, true);

        await this.browser.moveToObject('.TruncateArrow-Button');
        await this.browser.$('.TruncateArrow-Button').click();
        await this.browser.assertBodyView(
            'ComparePage-serpsetsInfo-details-cgi',
        );

        await this.browser.execute(() => {
            window.localStorage.removeItem('compare_page');
        });
    });

    it('should open comparePage with opened details', async function () {
        await this.browser.execute(() => {
            window.localStorage.setItem(
                'compare_page',
                JSON.stringify({showDetails: true}),
            );
        });
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=35816935&serpset=33273608&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();
        await this.browser.assertBodyView(
            'ComparePage-serpsetsInfo-details-localstorage',
        );

        await this.browser.execute(() => {
            window.localStorage.removeItem('compare_page');
        });
    });

    it('should change componentFilters', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=35816935&serpset=33273608&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();

        await this.browser.waitAndClick(
            '[data-test-id=overallComponentFilterSelect',
        );
        await this.browser.waitAndClick(
            '[data-test-id="overallComponentFilterSelect-0-option"]',
        );
        await this.browser.waitForFetching();
        await this.browser.assertBodyView(
            'ComparePage-calculations-changed-overall-component-filter',
        );

        await this.browser.waitAndClick(
            '[data-test-id="componentFilterSelect-33273608"]',
        );
        await this.browser.waitAndClick(
            '[data-test-id="componentFilterSelect-33273608-8-option"]',
        );
        await this.browser.waitForFetching();
        // reset cursor
        await this.browser.waitAndClick(
            '//*[@id="root"]/div/div[1]/div[1]/nav/ol/li',
        );
        await this.browser.assertBodyView(
            'ComparePage-calculations-changed-component-filter',
        );
    });

    it('should open SerpsetsInfo popups', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36778029&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();

        await this.browser.waitAndClick(
            '[data-test-id=serpsetsInfoRecalculateDropdown]',
        );
        await this.browser.assertBodyView(
            'ComparePage-serpsetsInfo-recalculate',
        );

        await this.browser
            .$('[data-test-id=serpsetsInfoDownloadDropdown]')
            .click();
        await this.browser.assertBodyView('ComparePage-serpsetsInfo-download');
        await this.browser
            .$('[data-test-id=serpsetsInfoDownloadDropdown]')
            .click();

        await this.browser
            .$('[data-test-id=serpsetsInfoMoreOptionsDropdown]')
            .click();
        await this.browser.assertBodyView(
            'ComparePage-serpsetsInfo-more-options',
        );
    });

    hermione.skip.in(/.*/, 'Not stable');
    it('should open change serpset modal', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36778029&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();

        await this.browser
            .$('[data-test-id=serpsetsInfoChangeSerpset]')
            .click();
        await this.browser.waitForFetching();
        await this.browser.pause(1000);
        await this.browser.waitAndClick(
            '/html/body/div[3]/div[1]/div/div/div/div[1]/div/div/span',
        );
        await this.browser.assertBodyView('ComparePage-change-serpset-modal');
    });

    it('should close serpset', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36778029&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();

        await this.browser.waitAndClick('.CloseSerpsetButton');
        await this.browser.waitForFetching();
        await this.browser.assertBodyView('ComparePage-closed-serpset');
    });

    it('should set as baseline', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36778029&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();

        await this.browser.waitAndClick('.HeaderSerpsetInfo-SetAsBaseline');
        await this.browser.waitForFetching();
        await this.browser.assertBodyView('ComparePage-set-as-baseline');
    });

    it('should expand/compress all groups', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36778029&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();

        await this.browser.waitAndClick('[data-test-id=ExpandAllGroups]');
        await this.browser.assertBodyView(
            'ComparePage-calculations-all-groups-expanded',
        );

        await this.browser.$('[data-test-id=CompressAllGroups]').click();
        await this.browser.assertBodyView(
            'ComparePage-calculations-all-groups-compressed',
        );

        await this.browser.$('[data-test-id=OnlySignificantToggle]').click();
        await this.browser.assertBodyView(
            'ComparePage-calculations-only-significant',
        );

        await this.browser.$('[data-test-id=ExpandAllGroups]').click();
        await this.browser.assertBodyView(
            'ComparePage-calculations-only-significant-expanded',
        );
    });

    it('should open metric description', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset=36778029&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();

        const metricName = 'metrica-click-likelihood-query-5';
        await this.browser.waitForExist(
            `[data-test-id="MetricGroupRow-${metricName}"]`,
            10000,
        );
        await this.browser.moveToObject(
            `[data-test-id="MetricGroupRow-${metricName}"]`,
        );
        await this.browser
            .$(`[data-test-id="MetricDescription-${metricName}"`)
            .click();
        await this.browser.assertBodyView(
            'ComparePage-calculations-metric-description',
        );
    });

    it('should expand metric', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset=36778029&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();

        const metricName = 'proxima-v11';
        await this.browser.waitAndClick(
            `[data-test-id="MetricGroupRow-metricName-${metricName}"]`,
        );
        await this.browser.assertBodyView(
            'ComparePage-calculations-metric-expanded',
        );
    });

    it('should open metric value popups', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36794708&serpset=36778029&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();

        const metricName = 'proxima-v11';
        await this.browser.waitAndClick(
            `[data-test-id="MetricGroupRow-metricName-${metricName}"]`,
        );

        const metricValue =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[3]/td[2]/div/div';
        const metricDiff =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[3]/td[3]/div/div[2]';
        const metricDiffPercent =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[3]/td[4]/div';
        const metricBaseline =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[3]/td[6]/div/div';
        const checkerValue =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[6]/td[2]/div/div[2]';
        const checkerDiff =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[6]/td[3]/div/div[2]';
        const checkerDiffPercent =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[6]/td[4]/div';
        const checkerBaseline =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[6]/td[6]/div/div[2]';
        const metricLight =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[3]/td[1]/div/div[2]/div/div';
        const checkerLight =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[6]/td[1]/div/span[1]/div/div';
        const checkerValueLight =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[6]/td[2]/div/div[1]/div';
        const checkerBaselineLight =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[6]/td[6]/div/div[1]/div';

        await this.browser.moveToObject(metricValue);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-metric-value',
        );
        await this.browser.moveToObject(metricDiff);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-metric-diff',
        );
        await this.browser.moveToObject(metricDiffPercent);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-metric-diff-precent',
        );
        await this.browser.moveToObject(metricBaseline);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-metric-baseline',
        );

        await this.browser.moveToObject(checkerValue);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-checker-value',
        );
        await this.browser.moveToObject(checkerDiff);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-checker-diff',
        );
        await this.browser.moveToObject(checkerDiffPercent);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-checker-diff-precent',
        );
        await this.browser.moveToObject(checkerBaseline);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-checker-baseline',
        );

        await this.browser.moveToObject(metricLight);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-light-metric',
        );
        await this.browser.moveToObject(checkerLight);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-light-checker',
        );
        await this.browser.moveToObject(checkerValueLight);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-light-checker-value',
        );
        await this.browser.moveToObject(checkerBaselineLight);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-light-checker-baseline',
        );
    });

    it('should open n/a popup', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36794708&serpset=36778029&serpset-filter=true&serpset-filter=true&absolute=false',
        );
        await this.browser.waitForFetching();

        const naValue =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[3]/td[2]/div/div';
        await this.browser.waitForExist(naValue, 10000);
        await this.browser.moveToObject(naValue);
        await this.browser.assertBodyView('ComparePage-calculations-popup-na');
    });

    it('should open undef popup', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36794708&serpset=36778029&serpset-filter=true&serpset-filter=true&absolute=false&pre-filter=country-VI',
        );
        await this.browser.waitForFetching();

        const undefValue =
            '//*[@id="root"]/div/div[3]/div[2]/div/div[2]/div/div/div[1]/div[2]/table/tbody/tr[3]/td[2]/div/div';
        await this.browser.waitForExist(undefValue, 10000);
        await this.browser.moveToObject(undefValue);
        await this.browser.assertBodyView(
            'ComparePage-calculations-popup-undef',
        );
    });

    it('should open with filters', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36778029&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&absolute=true&pre-filter=db-5&pre-filter=query-511786&pre-filter=labels-foo&pre-filter=country-AL-AD-AU&pre-filter=region-6&post-filter=nodiff-404_in_limus_relevance',
        );
        await this.browser.waitForFetching();

        // wait for scrollbar
        await this.browser.pause(1000);
        await this.browser.assertBodyView(
            'ComparePage-calculations-with-filters',
        );
    });

    it('should open with filters - 2', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36778029&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&absolute=true&pre-filter=db-5&post-filter=nodiff-404_in_limus_relevance',
        );
        await this.browser.waitForFetching();

        // wait for scrollbar
        await this.browser.pause(1000);
        await this.browser.assertBodyView(
            'ComparePage-calculations-with-filters-2',
        );
    });

    it('should change aspect', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=36778029&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();

        await this.browser.waitAndClick('[data-test-id=aspectSuggest]');
        await this.browser.waitAndClick(
            '[data-test-id=aspectSuggest-3-option]',
        );
        await this.browser.waitForFetching();
        await this.browser.assertBodyView(
            'ComparePage-calculations-changed-aspect',
        );
    });

    it('should scroll to the right border', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=38142349&serpset=38142750&serpset=38142752&serpset=38142758&serpset=38142765&serpset=38142768&serpset=38142779&serpset=38142784&serpset=38142788&serpset=38142825&serpset=38142832&serpset=38142924&serpset=38142932&serpset=38142942&serpset=38142963&serpset=36778029&serpset=36794708&serpset=36814132&serpset=36832231&serpset=36849258&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();

        const rightBorder =
            '//*[@id="root"]/div/div[3]/div[1]/div/div/div/div[1]/table/tbody/tr[2]/td[22]';

        await this.browser.waitAndClick(rightBorder);
        await this.browser.assertBodyView(
            'ComparePage-calculations-scrolled-to-right',
        );
    });

    it('should expand 2 metrics', async function () {
        await this.browser.url(
            '/compare?regional=RU&evaluation=WEB&aspect=default&serpset=36102585&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();

        const firstMetric = 'proxima-v11-light';
        await this.browser.waitAndClick(
            `[data-test-id="MetricGroupRow-metricName-${firstMetric}"]`,
        );

        await this.browser.pause(1000);

        const secondMetric = 'proxima-v10';
        await this.browser.waitAndClick(
            `[data-test-id="MetricGroupRow-metricName-${secondMetric}"]`,
        );

        const checkerName = 'proxima-v10.error';
        await this.browser.waitAndClick(
            `[data-test-id="CheckerRow-checkerName-${checkerName}"]`,
        );

        await this.browser.assertFullPageView(
            'ComparePage-calculations-2-metrics-expanded',
        );
    });

    it('should remember expanded groups on filter change', async function () {
        await this.browser.url(
            '/compare?regional=RU&evaluation=WEB&aspect=default&serpset=36102585&serpset-filter=wizardInplaceOrganicStealers',
        );
        await this.browser.waitForFetching();
        await this.browser.waitAndClick('[data-test-id=diffGroup]');

        const allModeFilter =
            '//*[@id="root"]/div/div[2]/div[2]/div/span[2]/label[1]/input';
        await this.browser.waitAndClick(allModeFilter);
        await this.browser.waitForFetching();

        await this.browser.assertFullPageView(
            'ComparePage-calculations-remember-groups',
        );
    });
});
