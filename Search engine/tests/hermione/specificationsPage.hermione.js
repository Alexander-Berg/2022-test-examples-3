describe('SpecificationsPage', function () {
    it('should open list', async function () {
        await this.browser.url('/admin/specifications');
        await this.browser.waitForFetching();
        await this.browser.assertBodyView('SpecificationsPage-list');
    });

    it('should open list with params', async function () {
        await this.browser.url(
            '/admin/specifications?text=geo&owner=kostapenko',
        );
        await this.browser.waitForFetching();
        await this.browser.assertBodyView(
            'SpecificationsPage-list-with-params',
        );
    });

    it('should open specification', async function () {
        await this.browser.url('/admin/specifications/5OverlappedRelevance');
        await this.browser.waitForFetching();
        await this.browser.assertFullPageView(
            'SpecificationsPage-specification',
        );
    });

    it('should open specification multipart', async function () {
        await this.browser.url(
            '/admin/specifications/5OverlappedRelevance#multipart',
        );
        await this.browser.waitForFetching();
        await this.browser.assertFullPageView(
            'SpecificationsPage-specification-multipart',
        );
    });

    it('should open import form', async function () {
        await this.browser.url('/admin/specifications/import');
        await this.browser.assertBodyView('SpecificationsPage-import');
    });
});
