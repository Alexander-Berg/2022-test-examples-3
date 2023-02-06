const { assert } = require('chai');
const paths = require('./paths');
const getUrl = require('../../utils/getUrl');

module.exports = async function() {
    const { browser } = this;

    await browser.pause(1000);
    await browser.url(getUrl() + '/service/direct/search');
    await browser.pause(1000);
    const field = await browser.$(paths.pagePath + paths.campaignIdField);
    await field.click();
    const input = await browser.$(paths.pagePath + paths.campaignIdField + '/input');
    await input.setValue('42509534');

    //const tag = await browser.$(paths.pagePath + paths.campaignIdFirstTag);
    //const value = await tag.getText();

    const archiveCheckbox = await browser.$(paths.pagePath + paths.archiveCheckbox);
    await archiveCheckbox.click();

    await browser.pause(1000);

    const searchButton = await browser.$(paths.pagePath + paths.searchButton);
    await searchButton.click();

    // Ждем загрузки объектов
    await browser.$(paths.pagePath + paths.pathToFullLink).waitForDisplayed({ timeout: 50000 });

    // Проверка существования настройки вида
    const view = await browser.$(paths.pagePath + paths.view);
    const viewText = await view.getText();

    // Проверка существования настройки полей
    const fieldsSettings = await browser.$(paths.pagePath + paths.fieldsSettings);
    const fieldsSettingsText = await fieldsSettings.getText();

    // Проверка появления пагинации
    const pagination = await browser.$(paths.pagination);
    const paginationText = await pagination.getText();

    // Проверка текста ссылки первого баннера в выдаче
    const link = await browser.$(paths.pagePath + paths.pathToFullLink);
    const linkText = await link.getText();

    // Кликаем на первый баннер
    const firstObjectSelect = await browser.$(paths.pagePath + paths.firstObject);
    await firstObjectSelect.click();

    // Ждем открытие панели модерации
    await browser.$(paths.root + paths.moderationHeader).waitForDisplayed({ timeout: 5000 });

    // Проверка открытия панели модерации
    const moderationHeader = await browser.$(paths.root + paths.moderationHeader);
    const moderationHeaderText = await moderationHeader.getText();

    assert.equal(linkText, 'Аккуратный офисный переезд в Мск!\n– *Упаковка с нас!');
    assert.equal(viewText, 'Компактный');
    assert.equal(moderationHeaderText, 'Модерировать объекты (1)');
    assert.equal(fieldsSettingsText, 'Настроить поля');
    assert.equal(paginationText, '<\n1\n2\n3\n4\n>');
};
