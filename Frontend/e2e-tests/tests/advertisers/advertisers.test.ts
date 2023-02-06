import { test, expect } from '@playwright/test';

import { Browser, getBrowser } from '../../browser';
import { AdvertisersPage } from '../../pages/advertisers';

let browser: Browser;
let page: AdvertisersPage;

test.describe('Роутинг адвертайзеров', () => {
    let advertisersData: { advertisers: { id: number, details: { name: string, type: string }}[] };

    const defaultSection = 'clients';

    test.beforeAll(async() => {
        return getBrowser().then(async browserInstance => {
            browser = browserInstance;
            page = new AdvertisersPage(browser.page);

            await page.authenticate();

            advertisersData = await page.waitForAdvertisersResponseData();
        });
    });

    test('По умолчанию открывается страница с первым доступным адвертайзером', async() => {
        await browser.open('/advertiser');

        const firstAdvertiserName = advertisersData.advertisers[0].details.name;

        expect(await page.currentAdvertiserName()).toEqual(firstAdvertiserName);
    });

    test('Есть возможность перейти к другому адвертайзеру из меню', async() => {
        const firstAdvertiser = advertisersData.advertisers[0];
        const secondAdvertiser = advertisersData.advertisers[1];

        await browser.open(`/${firstAdvertiser.id}`);
        await page.advertiserCardMenuButton.click();

        const menuItem = await page.menuAdvertiserLinkSelector(secondAdvertiser.details.name, secondAdvertiser.id);
        await menuItem.click();

        expect(await page.currentAdvertiserName()).toEqual(secondAdvertiser.details.name);
        expect(await page.getLocationPathname()).toEqual(`/${secondAdvertiser.id}/${defaultSection}`);
    });

    test('При переходе на адвертайзера без раздела происходит редирект на раздел по умолчанию', async() => {
        const firstAdvertiser = advertisersData.advertisers[0];
        const url = `/${firstAdvertiser.id}`;

        await browser.open(url);

        expect(await page.currentAdvertiserName()).toEqual(firstAdvertiser.details.name);
        expect(await page.getLocationPathname()).toEqual(`${url}/${defaultSection}`);
    });

    test('Последний открытый адвертайзер сохраняется и выбирается при следующем открытии', async() => {
        const secondAdvertiser = advertisersData.advertisers[1];

        await browser.open(`/${secondAdvertiser.id}/${defaultSection}`);
        await page.currentAdvertiserNameVisible();

        await browser.open('/advertiser');

        expect(await page.currentAdvertiserName()).toEqual(secondAdvertiser.details.name);
    });

    test('При переходе на адвертайзера-клиента открывается раздел Рекламных аккаунтов', async() => {
        const firstAdvertiser = advertisersData.advertisers[0];
        const clientAdvertiser = advertisersData.advertisers.find(advertiser => advertiser.details.type === 'client');

        if (!clientAdvertiser) {
            test.fixme(!clientAdvertiser, 'no advertiser with type "client"');
            return;
        }

        await browser.open(`/${firstAdvertiser.id}`);
        await page.advertiserCardMenuButton.click();

        const menuItem = await page.menuAdvertiserLinkSelector(clientAdvertiser.details.name, clientAdvertiser.id);
        await menuItem.click();

        expect(await page.currentAdvertiserName()).toEqual(clientAdvertiser.details.name);
        expect(await page.getLocationPathname()).toEqual(`/${clientAdvertiser.id}/accounts`);
    });

    test('При открытии несуществующего адвертайзера отображается страница 404', async() => {
        await browser.open(`/999999/${defaultSection}`);

        await expect(page.selector('h1')).toContainText('Ошибка 404');
    });

    test('При открытии несуществующей страницы внутри доступного адвертайзера отображается страница 404', async() => {
        const firstAdvertiser = advertisersData.advertisers[0];

        await browser.open(`/${firstAdvertiser.id}/nonexistent`);

        await expect(page.selector('h1')).toContainText('Ошибка 404');
    });

    test('При отсутствии доступных адвертайзеров происходит редирект', async() => {
        const unroute = await page.routeEmptyAdvertisersList();

        await browser.open('/advertiser');

        await page.waitForRedirect();

        expect(await page.getLocationPathname()).toEqual('/no-advertiser');

        await unroute();
    });

    test('При открытии текущего адвертайзера, к которому был отозван доступ, происходит редирект', async() => {
        const firstAdvertiser = advertisersData.advertisers[0];

        await page.setLocalStorageAdvertiserID('999999');
        await browser.open('/');
        await page.waitForRedirects();

        expect(await page.getLocationPathname()).toEqual(`/${firstAdvertiser.id}/clients`);
    });
});
