describe('GraphsPage', async function () {
    beforeEach(async function () {
        await this.browser.execute(function () {
            window.localStorage.removeItem('graph_page_side_menu');
        });
    });

    it('empty parameters', async function () {
        await this.browser.url('/graphs/chart');
        await this.browser.pause(1000);

        await this.browser.assertBodyView('GraphsPage-empty-parameters');

        await this.browser.$('.Mx-Modal-CloseBtn').click();
        await this.browser.assertBodyView(
            'GraphsPage-empty-parameters-closed-modal',
        );
    });

    it('empty parameters with hidden filters', async function () {
        await this.browser.execute(function () {
            window.localStorage.setItem('graph_page_side_menu', 'false');
        });

        await this.browser.url('/graphs/chart');
        await this.browser.pause(1000);

        await this.browser.$('.Mx-Modal-CloseBtn').click();
        await this.browser.assertBodyView(
            'GraphsPage-empty-parameters-hidden-filters',
        );
    });

    it('rich parameters set - multi points selected', async function () {
        await this.browser.url(
            '/graphs/chart?crons=102446&systems=8792_102446&systems=8794_102446&filters=default.2021-01&metrics=proxima-v9&dates=2021-09-17T00:00:00&dates=2021-09-18T00:00:00&dates=2021-09-21T00:00:00&dates=2021-09-23T00:00:00&confidentOnly=true&raw=false&lines=102446,8794,default.2021-01,proxima-v9&lines=102446,8792,default.2021-01,proxima-v9&endDate=2021-09-25T00:00:00&startDate=2021-08-04T00:00:00&baseline=102446,8792,default.2021-01,proxima-v9&unselected=102446,8794,default.2021-01,proxima-v9',
        );

        await this.browser.waitForFetching();

        await this.browser.assertBodyView('GraphsPage-rich-parameters-set');

        await this.browser
            .$('[content="Скрыть фильтры"]')
            .$('.Badge')
            .$('.Button2')
            .click();
        await this.browser.$('.Mx-Layout-Main').click();

        await this.browser.assertBodyView('GraphsPage-hidden-filters');
    });

    hermione.skip.in(
        'chrome',
        'скипаем из-за плавающей верстки в Хроме, пока не разобрались',
    );
    it('rich parameters set - single point selected', async function () {
        await this.browser.url(
            '/graphs/chart?crons=102446&systems=8792_102446&systems=8794_102446&filters=default.2021-01&metrics=proxima-v9&dates=2021-09-17T06:10:00&confidentOnly=false&raw=true&lines=102446,8794,default.2021-01,proxima-v9&lines=102446,8792,default.2021-01,proxima-v9&endDate=2021-09-25T00:00:00&startDate=2021-08-04T00:00:00&averagingPeriod=P1D&baselineDate=2021-09-17T06:10:00',
        );

        await this.browser.waitForFetching();

        await this.browser.assertFullPageView(
            'GraphsPage-rich-parameters-set-single-point',
        );
    });

    it('table view', async function () {
        await this.browser.url(
            '/graphs/table?crons=102446&systems=8792_102446&systems=8794_102446&filters=default.2021-01&metrics=proxima-v9&dates=2021-09-17T00:00:00&dates=2021-09-18T00:00:00&dates=2021-09-21T00:00:00&dates=2021-09-23T00:00:00&confidentOnly=true&raw=false&lines=102446,8794,default.2021-01,proxima-v9&lines=102446,8792,default.2021-01,proxima-v9&endDate=2021-09-25T00:00:00&startDate=2021-08-04T00:00:00&baseline=102446,8792,default.2021-01,proxima-v9',
        );

        await this.browser.waitForFetching();

        await this.browser.assertBodyView('GraphsPage-table-view');
    });
});
