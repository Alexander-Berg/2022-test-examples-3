const registrationPage = require('../../../../hermione/pages/master-registration');
const { gotoOrganization } = require('./common');
const { URL } = require('url');

/**
 * @param {Browser} bro
 * @param params
 */
async function gotoMasterRegistration(bro, params = {}) {
    const broUrl = await bro.getUrl();
    const url = new URL(broUrl);
    const { free, edu, soft, partnerReg, partner } = params;
    if (free) {
        url.searchParams.set('productId', '1');
    }
    if (edu) {
        url.searchParams.set('education', '1');
    }
    if (partnerReg) {
        url.searchParams.set('partner-registration', '1');
    }
    if (partner) {
        url.searchParams.set('partner', String(partner));
    }

    await bro.url(`/master-registration?${url.searchParams.toString()}`);

    if (!soft) {
        await bro.yaWaitForVisible(
            registrationPage.chooseOwnerStep(),
            'Не отобразился шаг выбора владельца'
        );
    }
}

/**
 * @param {Browser} bro
 * @param {string} userNumber
 */
async function gotoPaymentStubPage(bro, userNumber = '024') {
    // юзер с заглушкой на 3 шаге yndx-sarah-test-024
    await bro.yaLoginFast(
        `yndx-sarah-test-${userNumber}`,
        `pass-yndx-sarah-test-${userNumber}`
    );
    await gotoOrganization(bro);

    await bro.yaWaitForVisible(
        registrationPage.paymentStep(),
        'Не появился шаг оплаты'
    );
}

module.exports = {
    gotoMasterRegistration,
    gotoPaymentStubPage
};
