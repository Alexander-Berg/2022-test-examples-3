const consts = require('../config/index').folder;
const PageObjects = require('../page-objects/public');
const { listingItem } = require('../helpers/selectors');
const { assert } = require('chai');

const getListingItemText = async(bro, i) => {
    const element1 = await bro.$(listingItem(i));
    const elementText1 = await element1.getText();
    return elementText1.replace(/\n/g, '');
};

hermione.only.in('chrome-desktop');
describe('Доступность', () => {
    it('Навигация по листингу', async function() {
        const bro = this.browser;
        await bro.url(consts.PUBLIC_FOLDER_URL);
        await bro.yaWaitForVisible(PageObjects.listing());

        let activeText = await bro.execute(() => document.activeElement.textContent);
        const elementText1 = await getListingItemText(bro, 1);
        assert.equal(activeText, elementText1);

        // Заходим в папку
        await bro.keys('Enter');

        const item = await bro.$(listingItem(4));
        await bro.yaWaitForVisible(listingItem(4));
        assert.equal(
            (await item.getText(listingItem(4))),
            consts.PUBLIC_SUBFOLDER_ITEM_4
        );

        // Выходим из папки обратно в корень
        await bro.keys('Backspace');

        await bro.yaWaitForVisible(listingItem(1));
        activeText = await bro.execute(() => document.activeElement.textContent);
        assert.equal(activeText, elementText1);

        // Переходим стрелочкой ко второму файлу в списке
        await bro.keys('Down arrow');

        const item2 = await bro.$(listingItem(2));
        await item2.scrollIntoView();

        await bro.yaAssertView('accessibility-listing-1', listingItem(2));
    });

    it('Топбар', async function() {
        const bro = this.browser;
        await bro.url(consts.PUBLIC_FOLDER_URL);
        await bro.yaWaitForVisible(PageObjects.listing());
        // Открываем топбар
        await bro.keys('Space');
        await bro.pause(500);

        const infoElementText = await bro.execute(() => document.activeElement.getAttribute('aria-label'));
        assert.equal(infoElementText, 'Информация о папке');

        await bro.keys('Tab');
        const saveElementText = await bro.execute(() => document.activeElement.textContent);
        assert.equal(saveElementText, 'Сохранить на Яндекс.Диск');

        await bro.keys('Tab');
        const downloadElementText = await bro.execute(() => document.activeElement.textContent);
        assert.equal(downloadElementText, 'Скачать');

        await bro.keys('Tab');
        const closeElementText = await bro.execute(() => document.activeElement.getAttribute('aria-label'));
        assert.equal(closeElementText, 'Закрыть');

        // Проверяем что фокус заперт в топбаре
        await bro.keys('Tab');
        assert.equal(infoElementText, 'Информация о папке');
    });

    it('Слайдер', async function() {
        const bro = this.browser;
        await bro.url(consts.PUBLIC_FOLDER_URL);
        await bro.yaWaitForVisible(PageObjects.listing());
        // Переходим стрелочкой ко второму файлу в списке
        await bro.keys('Down arrow');
        await bro.keys('Enter');
        await bro.pause(500);
        // Проверяем навигацию по кнопкам
        const saveElementText = await bro.execute(() => document.activeElement.textContent);
        assert.equal(saveElementText, 'Сохранить на Яндекс.Диск');

        await bro.keys('Tab');
        const downloadElementText = await bro.execute(() => document.activeElement.textContent);
        assert.equal(downloadElementText, 'Скачать');

        await bro.keys('Tab');
        const moreElementText = await bro.execute(() => document.activeElement.getAttribute('aria-label'));
        assert.equal(moreElementText, 'Ещё');

        await bro.keys('Tab');
        const closeElementText = await bro.execute(() => document.activeElement.getAttribute('aria-label'));
        assert.equal(closeElementText, 'Закрыть');

        await bro.keys('Tab');
        const nextElementText = await bro.execute(() => document.activeElement.getAttribute('aria-label'));
        assert.equal(nextElementText, 'Следующий файл');
        // Проверяем подпись файла
        const a11yTitle = await bro.$(PageObjects.slider.a11y());
        assert.equal(await a11yTitle.getAttribute('aria-live'), 'assertive');
        assert.include(await a11yTitle.getHTML(), '111 планета.jpg');

        // Листаем вправо
        await bro.keys('Down arrow');
        assert.include(await a11yTitle.getHTML(), '11111111 (1).zip');
    });

    it('Уведомление о копировании ссылки', async function() {
        // Если работает одно уведомление, то и остальные доступны
        const bro = this.browser;
        await bro.url(consts.PUBLIC_FOLDER_URL);
        await bro.yaWaitForVisible(PageObjects.listing());
        await bro.keys(['Shift', 'Tab']);
        const moreElementText = await bro.execute(() => document.activeElement.getAttribute('aria-label'));
        assert.equal(moreElementText, 'Ещё');
        await bro.keys('Space');
        await bro.pause(500);
        // Проверяем навигацию по кнопкам
        const copyElementText = await bro.execute(() => document.activeElement.textContent);
        assert.equal(copyElementText, 'Скопировать ссылку');
        await bro.keys('Enter');

        await bro.yaWaitForVisible(PageObjects.notification.body.inner());
        const notificationContent = await bro.$(PageObjects.notification.body.inner());
        assert.equal(await notificationContent.getAttribute('aria-live'), 'assertive');
        assert.include(await notificationContent.getHTML(), 'Ссылка скопирована');
    });
});
