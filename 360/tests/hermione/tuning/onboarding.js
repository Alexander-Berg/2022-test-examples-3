const pageObjects = require('../page-objects/tuning').objects;

hermione.only.notIn('chrome-desktop', 'убрать, когда будет писаться полноценный тест');

describe('Онбординг', function() {
    it('Внешний вид', async function() {
        const bro = this.browser;

        await bro.yaLoginFast('yndx-ufo-test-tuning', 'pass-yndx-tuning-onboarding');
        await bro.url('?onboarding_paid_features=hooray_mailing_storeRemovedMail_beautifulEmail_downloadApp&hooray_delay=100&test-id=613931');

        await bro.yaWaitForVisible(pageObjects.trust());
        await bro.click(pageObjects.trust());
        await bro.yaWaitForVisible(pageObjects.paymentFormClose());
        await bro.click(pageObjects.paymentFormClose());
        await bro.yaWaitForVisible(pageObjects.Modal());
        await bro.assertView('onboarding-hooray', pageObjects.Modal(), {
            invisibleElements: [pageObjects.Modal.Lottie()]
        });
        await bro.click(pageObjects.Modal.button());

        await bro.assertView('onboarding-mailing', pageObjects.Modal());
        await bro.click(pageObjects.Modal.skipWrapper.button());
        await bro.assertView('onboarding-storeRemovedMail', pageObjects.Modal());
        await bro.click(pageObjects.Modal.skipWrapper.button());
        await bro.assertView('onboarding-beautifulEmail', pageObjects.Modal());
        await bro.click(pageObjects.Modal.skipWrapper.button());
        await bro.assertView('onboarding-downloadApp', pageObjects.Modal());
    });
});
