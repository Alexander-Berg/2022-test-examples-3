const VALIDATE_TIMEOUT = 10000;

describe('_crons', function () {
    beforeEach(async function () {
        await this.browser.url('/crons');
    });

    it('should open list', async function () {
        await this.browser.url('/crons?page.size=25');
        await this.browser.pause(5000);
        await this.browser.assertFullPageView('_crons-list', {
            closeAllToasts: true,
        });
    });

    it('should open runs', async function () {
        await this.browser.url('/crons/101527/runs');
        await this.browser.pause(2000);
        await this.browser.assertFullPageView('_crons-runs');
    });

    it('should open revisions', async function () {
        await this.browser.url('/crons/101527/revisions');
        await this.browser.pause(2000);
        await this.browser.assertFullPageView('_crons-revisions');
    });

    it('should open clone form', async function () {
        await this.browser.url('/crons/101987/clone');
        await this.browser.pause(5000);
        await this.browser.assertFullPageView('_crons-clone-form', {
            closeAllToasts: true,
        });
    });

    hermione.skip.in('firefox', 'Element could not be scrolled into view');
    it('should change specification', async function () {
        await this.browser.url('/crons/101987/clone');

        await this.browser.waitForExist(
            '[data-test-id=specificationSelect]',
            10000,
        );
        await this.browser.$('[data-test-id=specificationSelect]').click();

        const changeTo =
            '[data-test-id=specificationIdAndRevisionSelectOption17]';
        await this.browser.waitForExist(changeTo, 10000);
        await this.browser.$(changeTo).click();

        await this.browser.pause(VALIDATE_TIMEOUT);
        await this.browser.assertFullPageView('_crons-change-specification', {
            closeAllToasts: true,
        });
    });

    it('should open new form', async function () {
        await this.browser.url('/crons/new');

        await this.browser.pause(VALIDATE_TIMEOUT);
        await this.browser.assertFullPageView('_crons-new-form', {
            closeAllToasts: true,
        });
    });

    hermione.skip.in('firefox', 'Element could not be scrolled into view');
    it('should open new form - hosts', async function () {
        await this.browser.url('/crons/new');

        await this.browser.waitForExist('[data-test-id=template]', 10000);
        await this.browser.$('[data-test-id=template]').click();

        await this.browser.waitForExist(
            '[data-test-id=templateSelectOption0]',
            10000,
        );
        await this.browser.$('[data-test-id=templateSelectOption0]').click();

        await this.browser.waitForExist(
            '[data-test-id=expandableBlock]',
            10000,
        );
        await this.browser.$('[data-test-id=expandableBlock]').click();
        await this.browser.waitForExist(
            '[data-test-id=CroneFormHostSection]',
            10000,
        );

        await this.browser.pause(VALIDATE_TIMEOUT);
        await this.browser.assertFullPageView('_crons-new-form-hosts', {
            selector: '[data-test-id=CroneFormHostSection]',
            closeAllToasts: true,
        });
    });
});
