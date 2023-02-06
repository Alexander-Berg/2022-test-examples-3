const { Light } = require('../../../devices');

describe('Сторисы / есть УД, но нет колонок', () => {
    it('Есть сторя со ссылкой на Станцию', async function() {
        const { browser, PO } = this;

        await browser.yaLoginWritable();

        // На аккаунте нет колонок, но есть устройства УД
        await browser.yaAddDevices([new Light()]);

        // Открыть список сторис
        await browser.yaOpenPage('', PO.IotHome());
        await browser.yaClickToSelectorByText(PO.IotNewsFeed.emptyItem(), 'Все новости');
        await browser.waitForVisible(PO.StoriesGrid());
        await browser.yaWaitBottomSheetAnimation();

        // есть сторя С колонкой удобнее
        // Тапнуть на сторю
        await browser.yaClickToSelectorByText(PO.StoriesGrid.item(), 'С колонкой удобнее');
        await browser.waitForVisible(PO.StoryModal.story.storyItem.content());

        // Отображается сторя про колонку с кнопкой [station]
        await browser.yaAssertView('station', 'body');

        // Тап на «Купить колонку» ведёт на https://yandex.ru/alice/station
        const link = (await browser.getAttribute(PO.StoryModal.story.storyItem.content.button(), 'href')).toString();
        assert(link.startsWith('https://yandex.ru/alice/station'), 'Ожидалась ссылка на лэндинг Станции');
    });
});
