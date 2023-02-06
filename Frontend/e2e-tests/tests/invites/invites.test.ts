import { test, expect } from '@playwright/test';

import { getBrowser } from '../../browser';
import { InvitesPage } from '../../pages/invites';

let agencyPage: InvitesPage;
let clientPage: InvitesPage;

let clientEmail: string;

test.describe('Создание и принятие приглашений адвертайзеров', () => {
    test.beforeAll(async() => {
        agencyPage = new InvitesPage((await getBrowser()).page);
        await agencyPage.register();

        clientPage = new InvitesPage((await getBrowser()).page);
        const clientCredentials = await clientPage.register();
        clientEmail = clientCredentials.email;
    });

    test('Агентство может создать адвертайзера для клиента', async() => {
        await agencyPage.createAdvertiser('Агентство');

        await agencyPage.openCreateInvitePopup();
        await agencyPage.clickRadio('Создать кабинет');
        await agencyPage.clickContinueButton();
        await agencyPage.fillAdvertiserName(`${clientEmail}_adv`);
        await agencyPage.fillClientEmail(clientEmail);
        await agencyPage.clickSubmitInviteButton();
        const inviteId = (await agencyPage.getSubmittedInvite()).id;

        await expect(await agencyPage.getSuccessInviteSubmitNotification()).toBeVisible();

        await clientPage.openInvite(inviteId);
        await clientPage.clickAcceptButton();

        await clientPage.waitForRedirect();
    });

    test('Агентство может пригласить адвертайзера используя его ID', async() => {
        const id = await clientPage.createAdvertiser('Рекламодатель');
        await agencyPage.openCreateInvitePopup();

        await agencyPage.clickRadio('Пригласить по ID');
        await agencyPage.clickContinueButton();
        await agencyPage.fillClientId(id.toString());
        await agencyPage.clickSubmitInviteButton();
        const inviteId = (await agencyPage.getSubmittedInvite()).id;

        await expect(await agencyPage.getSuccessInviteSubmitNotification()).toBeVisible();

        await clientPage.openInvite(inviteId);
        await clientPage.clickAcceptButton();
        await clientPage.clickCloseInviteButton();
    });
});
