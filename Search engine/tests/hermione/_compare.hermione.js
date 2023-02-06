describe('_compare', function () {
    const waitForCalcsTable = async browser => {
        const firstMetricInTable =
            '/html/body/section/div[6]/div/table/tbody/tr[3]';
        await browser.waitForExist(firstMetricInTable, 20000);
    };

    it('should open empty select page', async function () {
        await this.browser.url('/mc/select');
        await this.browser.assertBodyView('_compare-select-empty');
    });

    hermione.skip.in(/.*/, 'list diff');
    it('should open select page', async function () {
        await this.browser.url(
            '/mc/select?regional=WORLD&evaluation=WEB&aspect=default&serpset=22551186&serpset=22558374&serpset=22558373&serpset-filter=onlySearchResult&serpset-filter=skipRightAlign&serpset-filter=onlySearchResult&view=default&metric=empty-serp&sort-field=diff&sort-direction=asc&page-size=100',
        );
        await this.browser.assertBodyView('_compare-select');

        await this.browser.$('[data-test-id=addSerpset]').click();
        await this.browser.pause(1000);
        await this.browser.waitAndClick('[data-test-id=UserSuggestMeButton]');
        await this.browser.waitForFetching();
        await this.browser.assertBodyView(
            '_compare-select-add-serpsets-modal',
            {screenshotDelay: 10000},
        );
    });

    it('should open empty', async function () {
        await this.browser.url('/mc/compare2');
        await this.browser.assertBodyView('_compare-empty');
    });

    it('should open with default component filters', async function () {
        await this.browser.url(
            '/mc/compare?regional=WORLD&evaluation=IMAGES&aspect=default&pre-filter=db-6&pre-filter=country-UA&pre-filter=region-143&serpset=33891175&serpset=29641874',
        );
        await waitForCalcsTable(this.browser);
        await this.browser.assertFullPageView(
            '_compare-default-component-filters',
            {
                closeAllToasts: true,
            },
        );
    });

    hermione.skip.in(/.*/, 'shows diff but there is no change');
    it('should open addSerpset modal', async function () {
        await this.browser.url(
            '/mc/compare2?regional=WORLD&evaluation=IMAGES&aspect=default&pre-filter=db-6&pre-filter=country-UA&pre-filter=region-143&serpset=33891175&serpset=29641874',
        );
        await waitForCalcsTable(this.browser);
        await this.browser.closeAllToasts();
        await this.browser.$('[data-test-id=SerpsetModalAddSerpset]').click();
        await this.browser.pause(1000);
        await this.browser.waitAndClick('[data-test-id=UserSuggestMeButton]');
        await this.browser.waitForFetching();
        await this.browser.assertBodyView('_compare-addSerpset-modal');
    });

    hermione.skip.in('firefox', 'Element could not be scrolled into view');
    it('should expand group', async function () {
        await this.browser.url(
            '/mc/compare2?regional=WORLD&evaluation=WEB&aspect=default&serpset=23137052&serpset=22920536&serpset-filter=onlySearchResult&serpset-filter=searchResultsAndWizardsLeftBlacklisted',
        );
        await waitForCalcsTable(this.browser);
        await this.browser.$('[data-test-id=diffGroup]').click();
        await this.browser.assertFullPageView('_compare-expanded-group');
    });

    hermione.skip.in(/.*/, 'no change in screens but show error');
    it('should open enrichments modal', async function () {
        await this.browser.url(
            '/mc/compare2?regional=WORLD&evaluation=WEB&aspect=default&serpset=29942609&serpset=30043517&serpset-filter=onlySearchResult&serpset-filter=onlySearchResult',
        );
        await waitForCalcsTable(this.browser);
        await this.browser
            .$('[data-test-id=enrichmentTasksModalOpenBtn]')
            .click();
        await this.browser.waitForFetching();
        await this.browser.assertBodyView('_compare-enrichments-modal');
    });

    it('should open enrichments tab', async function () {
        await this.browser.url(
            '/mc/compare2?regional=WORLD&evaluation=WEB&aspect=default&serpset=29942609&serpset=30043517&serpset-filter=onlySearchResult&serpset-filter=onlySearchResult',
        );
        await waitForCalcsTable(this.browser);
        await this.browser.$('[data-test-id=enrichments]').click();
        await this.browser.assertFullPageView('_compare-enrichments-tab');
    });

    hermione.skip.in(/.*/, 'element still not existing after 20000ms');
    it('should expand enrichment', async function () {
        await this.browser.url(
            '/compare?regional=WORLD&evaluation=WEB&aspect=default&serpset=38007895&serpset=38007911&serpset-filter=wizardInplaceOrganicStealers&serpset-filter=wizardInplaceOrganicStealers',
        );
        await waitForCalcsTable(this.browser);
        await this.browser.waitAndClick('[data-test-id=enrichments]');
        await this.browser.waitAndClick('[data-test-id=listItem]');
        await this.browser.assertFullPageView('_compare-expanded-enrichment', {
            closeAllToasts: true,
        });
    });

    it('should add enrichment', async function () {
        await this.browser.url(
            '/mc/compare2?regional=WORLD&evaluation=IMAGES&aspect=default&serpset=29685379&serpset=29685379&serpset-filter=onlySearchResult&serpset-filter=onlySearchResult',
        );
        await waitForCalcsTable(this.browser);
        await this.browser.waitAndClick('[data-test-id=addEnrichment]');
        await this.browser.waitForExist('[data-test-id=startTasks]', 10000);
        await this.browser.assertFullPageView('_compare-add-enrichment', {
            closeAllToasts: true,
        });
    });

    hermione.skip.in(/.*/, 'selects first enrichment');
    it('should add enrichment with custom fields', async function () {
        await this.browser.url(
            '/mc/compare2?regional=RU&evaluation=WEB&aspect=default&serpset=30681692&serpset-filter=onlySearchResult',
        );
        await waitForCalcsTable(this.browser);
        await this.browser.waitAndClick('[data-test-id=addEnrichment]');
        await this.browser.waitForExist('[data-test-id=startTasks]', 10000);
        await this.browser.assertFullPageView(
            '_compare-add-enrichment-custom-fields',
            {
                closeAllToasts: true,
            },
        );
    });

    it('should open calcs with selected metric', async function () {
        await this.browser.url(
            '/mc/compare2?regional=WORLD&evaluation=WEB&aspect=default&serpset=23137052&serpset=22920536&serpset-filter=onlySearchResult&serpset-filter=searchResultsAndWizardsLeftBlacklisted#retries_count',
        );
        const selectedMetric =
            '/html/body/section/div[6]/div/table/tbody/tr[12]';
        await this.browser.waitForExist(selectedMetric, 10000);
        await this.browser.assertFullPageView('_compare-calcs-selected-metric');
    });

    // TODO Всплывающее окно (tour) иногда рендерится со смещением
    // it('should open with tour', async function () {
    //     await this.browser.execute(function () {
    //         window.localStorage.setItem(
    //             'mx_tour_steps_storage',
    //             JSON.stringify({
    //                 neverShowTours: false,
    //             }),
    //         );
    //     });
    //     await this.browser.url(
    //         '/mc/compare2?regional=WORLD&evaluation=WEB&aspect=default&serpset=23137052&serpset=22920536&serpset-filter=onlySearchResult&serpset-filter=searchResultsAndWizardsLeftBlacklisted',
    //     );
    //     await waitForCalcsTable(this.browser);
    //     await this.browser.assertFullPageView('_compare-with-tour');
    // });

    const waitForQueriesTable = async browser => {
        const firstQuery = '/html/body/section/div/div/div[5]/div[2]/div[2]';
        await browser.waitForExist(firstQuery, 10000);
    };

    it('should open queries page', async function () {
        await this.browser.url(
            '/mc/queries2?regional=WORLD&evaluation=IMAGES&aspect=default&metric=judged-images-decision-tree-v4-10&serpset=29685379&serpset=29685379&serpset-filter=onlySearchResult&serpset-filter=onlySearchResult&left-serpset=29685379&right-serpset=29685379&left-component-filter=onlySearchResult&right-component-filter=onlySearchResult&pre-filter=db-4&pre-filter=country-UZ&sort-field=diff&sort-direction=desc&offset=0&page-size=25',
        );
        await waitForQueriesTable(this.browser);
        await this.browser.assertFullPageView('_compare-queries', {
            closeAllToasts: true,
        });
    });

    it('should open queries page with label filter', async function () {
        await this.browser.url(
            '/mc/queries2?regional=WORLD&evaluation=WEB&metric=esearch-coverage&serpset=30059416&serpset-filter=true&left-serpset=30059416&right-serpset=30059416&left-component-filter=true&right-component-filter=true&pre-filter=labels-ann_4%20AND%20cluster_1&aspect=default&sort-field=diff&sort-direction=asc&offset=0&page-size=25',
        );
        await waitForQueriesTable(this.browser);
        await this.browser.assertFullPageView('_compare-queries-label-filter');
    });

    it('should open queries page with undef metrics', async function () {
        await this.browser.url(
            '/mc/queries2?regional=RU&evaluation=VIDEO&metric=video-player-pfound-5&aspect=default&absolute=true&serpset=25199924&serpset=25199906&serpset=25189843&serpset-filter=wizardOnly&serpset-filter=onlySearchResult&serpset-filter=onlySearchResult&left-serpset=25199906&right-serpset=25189843&left-component-filter=onlySearchResult&right-component-filter=onlySearchResult&sort-field=diff&sort-direction=asc&offset=0&page-size=25',
        );
        await waitForQueriesTable(this.browser);
        await this.browser.assertFullPageView(
            '_compare-queries-undef-metrics',
            {
                closeAllToasts: true,
            },
        );
    });

    it('should open queries page with dist graph', async function () {
        await this.browser.url(
            '/mc/queries2?regional=WORLD&evaluation=IMAGES&metric=images-corsa-unity-v2-10&serpset=29685379&serpset=29641874&serpset-filter=onlySearchResult&serpset-filter=onlySearchResult&left-serpset=29685379&right-serpset=29641874&left-component-filter=onlySearchResult&right-component-filter=onlySearchResult&pre-filter=db-6&pre-filter=country-UA&pre-filter=region-143&aspect=default&sort-field=diff&sort-direction=asc&offset=0&page-size=25',
        );
        await waitForQueriesTable(this.browser);

        await this.browser.$('[data-test-id=distBtn]').click();
        await this.browser.assertFullPageView('_compare-queries-dist-graph', {
            closeAllToasts: true,
        });
    });

    const waitForQJTable = async browser => {
        const qjTable = '/html/body/section/div/div/table';
        await browser.waitForExist(qjTable, 20000);
        await browser.pause(1000);
    };

    it('should open QJ expanded', async function () {
        await this.browser.url(
            '/mc/qjudgement2?left-serpset=23137052&right-serpset=22920536&left-component-filter=true&right-component-filter=true&metric=_404-5&view-mode=expanded&pre-filter=country-RU&page-size=100&sort-field=diff&sort-direction=desc&regional=WORLD&evaluation=WEB&aspect=object_answer&query=новости&region-id=39&device=ANDROID&country=RU&serpset=23137052&serpset=22920536&serpset-filter=true&serpset-filter=true&offset=0&component-fields=url&component-fields=originalUrl&component-fields=title&component-fields=viewUrl&component-fields=imageUrl&component-fields=organizationUrl&component-fields=snippet&component-fields=videoPreview&component-fields=siteLinks&serp-metrics=_404-5&component-metrics=_404-5&queryIdx=1&serp-scales=metricTags.failed&component-scales=Relevance&component-scales=RELEVANCE',
        );

        await waitForQJTable(this.browser);
        await this.browser.assertFullPageView('_compare-qj-expanded');
    });

    it('should open QJ compact', async function () {
        await this.browser.url(
            '/mc/qjudgement2?left-serpset=23137052&right-serpset=22920536&left-component-filter=true&right-component-filter=true&metric=_404-5&pre-filter=country-RU&page-size=100&sort-field=diff&sort-direction=desc&regional=WORLD&evaluation=WEB&aspect=object_answer&query=новости&region-id=39&device=ANDROID&country=RU&serpset=23137052&serpset=22920536&serpset-filter=true&serpset-filter=true&offset=0&component-fields=url&component-fields=originalUrl&component-fields=title&component-fields=viewUrl&component-fields=imageUrl&component-fields=organizationUrl&component-fields=snippet&component-fields=videoPreview&component-fields=siteLinks&serp-metrics=_404-5&component-metrics=_404-5&view-mode=collapsed&queryIdx=1&serp-scales=metricTags.failed&component-scales=Relevance&component-scales=RELEVANCE',
        );

        await waitForQJTable(this.browser);
        await this.browser.assertFullPageView('_compare-qj-compact');
    });

    it('should open QJ deprecated metric', async function () {
        await this.browser.url(
            '/mc/qjudgement2?pre-filter=query-296149&serpset=24264934&serpset=24250063&serpset-filter=onlySearchResult&serpset-filter=onlySearchResult&metric=proxima-rel-kernel-2019-10&sort-field=leftMetric&sort-direction=asc&offset=0&page-size=100&left-serpset=24264934&right-serpset=24264934&left-component-filter=onlySearchResult&right-component-filter=onlySearchResult&query=%2B79009379343&country=RU&device=ANDROID&region-id=2&component-metrics=proxima-rel-kernel-2019-10&view-mode=expanded&component-scales=Relevance&regional=WORLD&evaluation=WEB&aspect=default&component-fields=url&component-fields=originalUrl&component-fields=title&component-fields=viewUrl&component-fields=imageUrl&component-fields=organizationUrl&component-fields=snippet&component-fields=videoPreview&component-fields=siteLinks&expression=%2B79009379343&queryIdx=1',
        );

        await waitForQJTable(this.browser);
        await this.browser.$('[data-test-id=showSettings]').click();
        await this.browser.assertFullPageView('_compare-qj-deprecated-metric');
    });

    hermione.skip.in('firefox', "Couldn't ignore geoMap");
    it('should open QJ json metric', async function () {
        await this.browser.url(
            '/mc/qjudgement2?regional=WORLD&evaluation=GEO_SEARCH&aspect=ugc&pre-filter=labels-ru+AND+rubric&serpset=28702247&serpset-filter=onlySearchResult&metric=judged10-org_snippet_tags&sort-field=leftMetric&sort-direction=desc&offset=0&page-size=100&left-serpset=28702247&right-serpset=28702247&left-component-filter=onlySearchResult&right-component-filter=onlySearchResult&query=Аптеки&country=RU&device=DESKTOP&uid=1560941900091200-2869035517-sas1-1464&region-id=2&map-info=30.403723%2C59.93919%40%400.005574%2C0.003019&view-mode=collapsed&component-scales=Relevance&component-scales=org_review_tags&component-fields=url&component-fields=originalUrl&component-fields=title&component-fields=viewUrl&component-fields=imageUrl&component-fields=organizationUrl&component-fields=snippet&component-fields=videoPreview&component-fields=siteLinks&serp-metrics=judged10-org_snippet_tags&component-metrics=judged10-org_snippet_tags&queryIdx=1',
        );

        await waitForQJTable(this.browser);
        await this.browser.assertFullPageView('_compare-qj-json-metric', {
            screenshotDelay: 5000,
            ignoreElements: ['.geoMap'],
        });
    });

    it('should open QJ details', async function () {
        await this.browser.url(
            '/mc/qjudgement2/details?side=left&index=6&sequenceNumber=6&regional=WORLD&evaluation=WEB&aspect=object_answer&preFilter=country-RU&preFilter=region-213&serpset=23137052&serpsetFilter=true&serpsetFilter=true&metric=_404-adult-60&sortField=diff&sortDirection=asc&offset=0&pageSize=100&query=знак%20квадратного%20корня&country=RU&device=DESKTOP&regionId=213&serpMetrics=_404-adult-60&componentMetrics=_404-adult-60&componentFields=url&componentFields=originalUrl&componentFields=title&componentFields=viewUrl&componentFields=imageUrl&componentFields=snippet&componentFields=videoPreview&componentFields=siteLinks&viewMode=expanded&secondarySerpset=22920536&componentFilter=true&secondaryComponentFilter=true',
        );

        await this.browser.waitForExist(
            '/html/body/section/div/div/div[1]/div/h1',
            10000,
        );
        await this.browser.assertFullPageView('_compare-qj-details');
    });

    it('should open Sbs', async function () {
        await this.browser.url(
            '/mc/sbs?regional=WORLD&evaluation=WEB&aspect=default&serpset=25562581&serpset=25563888&serpset-filter=onlySearchResult&serpset-filter=onlySearchResult&left-serpset=25562581&right-serpset=25563888&left-component-filter=onlySearchResult&right-component-filter=onlySearchResult&metric=proximaKC&sort-field=diff&sort-direction=asc&offset=0&page-size=100&query=king&country=KZ&device=ANDROID&region-id=29575&left-serp-url=https:%2F%2Fwww.google.kz%2Fsearch%3Fq%3Dking%26hl%3Dru%26num%3D20&right-serp-url=https:%2F%2Fwww.google.kz%2Fsearch%3Fq%3Dking%26hl%3Dru%26num%3D20&component-fields=url&component-fields=originalUrl&component-fields=title&component-fields=viewUrl&component-fields=imageUrl&component-fields=organizationUrl&component-fields=snippet&component-fields=videoPreview&component-fields=siteLinks',
        );

        await this.browser.assertFullPageView('_compare-sbs', {
            ignoreElements: ['a'],
        });
    });
});
