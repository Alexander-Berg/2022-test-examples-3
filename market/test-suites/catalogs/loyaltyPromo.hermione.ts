import 'hermione';
import {expect} from 'chai';

import {login, execScript, pause} from '../../helpers';
import Button from '../../page-objects/button';
import Error from '../../page-objects/error';

const GET_LOYALTY_PROMO_SCRIPT = `api.db.of('loyaltyPromo').withFilters{
                                                              eq('code', 'ocrm735')
                                                            }.get()`;
const UNARCHIVE_SCRIPT = (loyaltyPromoGid: string): string => {
    return `api.bcp.edit('${loyaltyPromoGid}', ['status': 'active'])`;
};
const GET_LOYALTY_PROMO_STATUS_SCRIPT = (loyaltyPromoGid: string): string => {
    return `api.db.get('${loyaltyPromoGid}').status`;
};

describe(`ocrm-377: Архивация акции лояльности, которая не используется в причине начисления бонуса`, () => {
    beforeEach(function() {
        return login(``, this);
    });

    it(`Акция лояльности успешно архивируется`, async function() {
        const loyaltyPromoGid = (await execScript(this.browser, GET_LOYALTY_PROMO_SCRIPT)).replace(/["\][\s]+/gi, '');

        await execScript(this.browser, UNARCHIVE_SCRIPT(loyaltyPromoGid));

        const archiveButton = new Button(
            this.browser,
            'body',
            '[data-ow-test-jmf-card-toolbar-action="edit-архивировать"]'
        );

        this.browser.url(`entity/${loyaltyPromoGid}`);

        await archiveButton.isDisplayed();

        await archiveButton.clickButton();

        let employeeStatus;

        for (let i = 0; i < 20; i++) {
            // eslint-disable-next-line no-await-in-loop
            await pause(2000);
            // eslint-disable-next-line no-await-in-loop
            employeeStatus = await execScript(this.browser, GET_LOYALTY_PROMO_STATUS_SCRIPT(loyaltyPromoGid));

            if (employeeStatus === '"archived"') break;
        }

        await execScript(this.browser, UNARCHIVE_SCRIPT(loyaltyPromoGid));

        expect(employeeStatus).to.equal('"archived"', 'Не удалось архивировать акцию лояльности');
    });
});

describe(`ocrm-378: Архивация акции лояльности, которая используется в причине начисления бонуса`, () => {
    beforeEach(function() {
        return login(`entity/loyaltyPromo@45267701`, this);
    });

    it(`Используемую акцию лояльности нельзя архивировать`, async function() {
        const archiveButton = new Button(
            this.browser,
            'body',
            '[data-ow-test-jmf-card-toolbar-action="edit-архивировать"]'
        );

        await archiveButton.isDisplayed();

        await archiveButton.clickButton();

        const error = new Error(this.browser, 'body', '[data-ow-test-global-request-error]');

        await error.isDisplayed();

        const errorText = await (await error.textContainer).getText();

        expect(errorText).to.include(
            'Запретить архивацию используемых',
            'Удается архивировать акцию, используемую в причине начисления бонуса'
        );
    });
});
