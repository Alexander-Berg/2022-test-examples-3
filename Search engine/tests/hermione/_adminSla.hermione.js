describe('_admin-SLA', async function () {
    const ACCEPTANCE_SLA = '017177d0f41b863d6ad5d6649fc770c1';

    beforeEach(async function () {
        await this.browser.url('/admin/sla');
    });

    it('should open list', async function () {
        await this.browser.assertBodyView('_admin-SLA-list');
    });

    it('should open new', async function () {
        await this.browser.url('/admin/sla/new');

        await this.browser.assertBodyView('_admin-SLA-new');
    });

    it('should open view', async function () {
        await this.browser.url(`/admin/sla/${ACCEPTANCE_SLA}`);

        await this.browser.assertBodyView('_admin-SLA-view');

        await this.browser.$('[data-test-id=expandableBlock]').click();
        await this.browser.assertBodyView('_admin-SLA-view-expanded-block');
    });

    it('should open clone view', async function () {
        await this.browser.url(`/admin/sla/${ACCEPTANCE_SLA}`);

        await this.browser.$('button=Clone').click();
        await this.browser.pause(2000);
        await this.browser.assertBodyView('_admin-SLA-clone');
        await this.browser.$('button=Delete').click();
    });
});
