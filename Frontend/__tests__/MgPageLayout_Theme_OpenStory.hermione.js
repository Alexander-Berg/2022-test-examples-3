hermione.only.in(['chrome-desktop', 'firefox']);

specs({
    feature: 'Открытые истории',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид сетки с колонками', function() {
        const selector = '.row__col.layout__main.place__main';

        return this.browser
            .url('/news/story/Rassmatrivavshij_delo_bryanskogo_chinovnika_sudya_otdelil_Rossiyu_ot_SSSR--1ffe2cd18a4a19042dd98fd97b74ad0b?flags=yxnerpa_news_desktop_story_open-stories%3D1')
            .yaWaitForVisible(selector, 'Блок не появился')
            .windowHandleSize({ width: 1300, height: 1972 })
            .assertView('news-story-layout-1300', selector)
            .setViewportSize({ width: 1920, height: 1972 })
            .assertView('news-story-layout-1920', selector);
    });
});
