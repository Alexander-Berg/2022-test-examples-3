specs({
    feature: 'Скролл контейнера с сообщениями',
}, function () {
    const infiniteListContainer = '.ui-InfiniteList-Container';

    beforeEach(async function () {
        await this.browser.yaOpenMessenger({
            userAlias: 'user',
        });
    });

    hermione.skip.in(['firefox'], 'https://st.yandex-team.ru/MSSNGRFRONT-3377');
    it('Скролл контейнера с сообщениями мышью-трекпадом', async function () {
        const { browser } = this;

        await browser.waitForVisible(infiniteListContainer, 'Список сообщений не виден');
        await browser.assertView('no-scroll', '.yamb-conversation');
        for (let i = 0; i < 2; i += 1) {
            await this.yaScrollInfiniteList(-200);
        }
        await browser.assertView('scroll', '.yamb-conversation');
    });
});
