const selectors = require('../../../page-objects').index;

describe('Блоки', function() {
    describe('Главная страница', function() {
        describe('Окно отписки', function() {
            it.langs.only('ru');
            it('Отображается', async function() {
                await this.browser
                    .ywOpenPage('moscow', {
                        lang: this.lang,
                        query: {
                            showmethehamster: { spa_pushscribe_settings: 0 },
                            unpush: 'assist_weathertomorrow_push'
                        },
                    })
                    .ywWaitForVisible(selectors.PushScribe, 5000)
                    .ywHideCamerasAndNews()
                    .execute(selectors => {
                        document.querySelector(selectors.ModalParanja).style.opacity = '1';
                    }, selectors)
                    .assertView('FirstScreen', selectors.ModalContent);
            });
        });
    });
});
