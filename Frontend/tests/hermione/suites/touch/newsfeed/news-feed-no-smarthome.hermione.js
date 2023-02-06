const { YandexMini } = require('../../../speakers');

describe('Сторисы / есть колонка, но нет УД', () => {
    it('Есть сторя со ссылкой на Маркет', async function() {
        const { browser, PO } = this;

        await browser.yaLoginWritable();

        // На аккаунте нет устройств УД, но есть колонка
        await browser.yaAddSpeakers([new YandexMini()]);

        // Открыть список сторис
        await browser.yaOpenPage('', PO.IotHome());
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();

        // есть сторя Умный дом это просто
        // Тапнуть на сторю
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'Умный дом');
        await browser.waitForVisible(PO.StoryModal.story.storyItem.content());

        // Отображается сторя про УД с кнопкой [smarthome]
        await browser.yaAssertView('smarthome', 'body');

        // Тап на Перейти в магазин ведёт на Яндекс Маркет со списком девайсов УД
        const link = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link.startsWith('https://market.yandex.ru/brands--umnyi-dom-yandexa/17333002'), 'Ожидалась ссылка на Маркет');
    });
});
