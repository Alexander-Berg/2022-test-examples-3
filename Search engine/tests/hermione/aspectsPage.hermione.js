describe('AspectsPage', function () {
    it('should open list', async function () {
        await this.browser.url('/admin/aspect-metrics');
        await this.browser.waitForFetching();
        await this.browser.pause(1000);
        await this.browser.assertBodyView('AspectsPage-list');
    });

    it('should open list with params', async function () {
        await this.browser.url(
            '/admin/aspect-metrics?evaluation=IMAGES&text=d',
        );
        await this.browser.waitForFetching();
        await this.browser.pause(1000);
        await this.browser.assertBodyView('AspectsPage-list-params');
    });

    it('should open list with owner', async function () {
        await this.browser.url('/admin/aspect-metrics?owner=nata-tischenko');
        await this.browser.waitForFetching();
        await this.browser.pause(1000);
        await this.browser.assertBodyView('AspectsPage-list-owner');

        await this.browser.execute(function () {
            window.localStorage.clear();
        });
    });

    it('should open creation form', async function () {
        await this.browser.url('/admin/aspect-metrics/new');
        await this.browser.waitForFetching();
        await this.browser.pause(1000);
        await this.browser.assertFullPageView('AspectsPage-new');

        await this.browser
            .$('[name=aspect]')
            .addValue('technical name-123_321');
        await this.browser.$('.Mx-Breadcrumbs-Li=New').click();
        await this.browser.assertFullPageView('AspectsPage-new-filled-id');

        await this.browser.$('[name=aspect]').addValue(' (aka id)');
        await this.browser.$('.Mx-Breadcrumbs-Li=New').click();
        await this.browser.assertFullPageView(
            'AspectsPage-new-filled-id-error',
        );
    });

    it('should open aspect', async function () {
        await this.browser.url('/admin/aspect-metrics/default?evaluation=CV');
        await this.browser.waitForFetching();
        await this.browser.pause(1000);
        await this.browser.assertFullPageView('AspectsPage-existing-aspect');
    });
});
