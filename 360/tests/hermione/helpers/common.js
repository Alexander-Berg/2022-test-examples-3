const noop = require('lodash/noop');
const { URL } = require('url');
const registrationPage = require('../../../../hermione/pages/master-registration');

/**
 * @param {Browser} bro
 * @param silent
 * @param indexOrg
 * @param getParams
 */
async function gotoOrganization(bro, silent, indexOrg = 0, getParams = []) {
    const gotoOrganizationInner = async function () {
        const broUrl = await bro.getUrl();
        const url = new URL(broUrl);
        if (getParams.length !== 0) {
            for (const { key, value } of getParams) {
                url.searchParams.set(key, value);
            }
        }
        await bro.url(`/select-organization?${url.searchParams.toString()}`);
        const orgSelector = registrationPage.selectOrgButton(indexOrg);
        await bro.yaWaitForVisible(
            orgSelector,
            'Не отобразилась кнопка выбора организации'
        );

        await bro.click(orgSelector);
    };

    if (silent) {
        try {
            await gotoOrganizationInner();
        } catch (_e) {
            noop();
        }
    } else {
        await gotoOrganizationInner();
    }
}

module.exports = {
    gotoOrganization
};
