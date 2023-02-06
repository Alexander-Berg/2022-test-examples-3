describe('experimentsPage', () => {
    // beforeEach(async ({browser}) => {
    //     await browser.url('/experiments');
    //     await browser.waitForExist(
    //         "[data-testid='experiments-table-item-name']",
    //         10000,
    //     );
    // });
    // it('should open experiments list', async function () {
    //     await this.browser.url('/experiments');
    //     await this.browser.waitForExist(
    //         "[data-testid='experiments-table-item-name']",
    //         30000,
    //     );
    //     this.browser.assertBodyView('experiments-list', {
    //         ignoreElements: ["[data-testid='experiments-list-item-duration']"],
    //     });
    // });
    // it('should show valid list of owner', async function () {
    //     await this.browser.url('/experiments');
    //     await this.browser.waitForExist(
    //         "[data-testid='experiments-table-item-name']",
    //         10000,
    //     );
    //     await this.browser.$('[data-testid="owner-control-owner-btn"]').click();
    //     await this.browser.assertBodyView('select-owner', {
    //         ignoreElements: experimentIgnoreElements,
    //     });
    // });
    // it('should show valid list of owner/responsible', async function () {
    //     await this.browser.url('/experiments');
    //     await this.browser.waitForExist(
    //         "[data-testid='experiments-table-item-name']",
    //         10000,
    //     );
    //     await this.browser
    //         .$('[data-testid="owner-control-ownerOrResponsible-btn"]')
    //         .click();
    //     await this.browser.assertBodyView('select-owner-or-responsible', {
    //         ignoreElements: experimentIgnoreElements,
    //     });
    // });
    // it('should show valid list of status filter', async function () {
    //     await this.browser
    //         .$('[data-test-id="owner-control-ownerOrResponsible-btn"]')
    //         .click();
    //     await this.browser.assertBodyView('select-owner-or-responsible', {
    //         ignoreElements: experimentIgnoreElements,
    //     });
    // });
    // it('should show valid list of source typs', async function () {
    //     await this.browser
    //         .$('[data-test-id="owner-control-ownerOrResponsible-btn"]')
    //         .click();
    //     await this.browser.assertBodyView('select-owner-or-responsible', {
    //         ignoreElements: experimentIgnoreElements,
    //     });
    // });
    // it('should show valid list', async () => {
    //     await this.browser.url(
    //         '/experiments/?status=COMPLETED&status=FAILED&days=90&source=NO_MLM&ownerOrResponsible=robot-uno',
    //     );
    //     await this.browser.waitForExist(
    //         "[data-testid='experiments-table-item-name']",
    //         10000,
    //     );
    //     this.browser.assertBodyView('should-show-valid-list', {
    //         ignoreElements: experimentIgnoreElements,
    //     });
    // });
    // it('should correclty reset filter', async () => {
    //     await this.browser.url(
    //         '/experiments/?status=COMPLETED&status=FAILED&days=90&source=NO_MLM&ownerOrResponsible=robot-uno',
    //     );
    //     await this.browser.$('[data-test-id="reset-filters"]').click();
    //     assert.equal(
    //         (url.pathname + url.search + url.hash).replace(/\/$/, ''),
    //         '/experiments?status=all&days=90&pageSize=100&ownerOrResponsible=robot-uno&page=0',
    //     );
    // });
});
