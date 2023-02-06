'use strinct';

const { checkNodeExists } = require('../../../UniSearch.test/helpers');
const PO = require('./UniSearchGames.page-object/index@common');

specs({
    feature: 'Универсальный колдунщик поиска игр',
}, function() {
    hermione.only.notIn(['searchapp-phone'], 'в firefox в гриде не поддержан кодек mp4');
    it('Внешний вид', async function() {
        await this.browser.yaOpenSerp({
            text: 'игры онлайн',
            // Пока ТБ.Игры не перешли на свой бэкенд, тип у документа такой
            data_filter: 'games',
        }, PO.UniSearchGames());

        await this.browser.assertView('plain', PO.UniSearchGames());
    });

    it('Баобаб-разметка айтемов', async function() {
        const assertNode = checkNodeExists.bind(this);

        await this.browser.yaOpenSerp({
            text: 'игры онлайн',
            // Пока ТБ.Игры не перешли на свой бэкенд, тип у документа такой
            data_filter: 'games',
        }, PO.UniSearchGames());

        await assertNode({
            path: '/$page/$main/$result/unisearch_games/view/scroller/item',
            attrs: {
                title: 'Аркады',
                url: 'https://yandex.ru/games/category/arcade',
            },
        });

        await assertNode({
            path: '/$page/$main/$result/unisearch_games/partnership-link',
            attrs: {
                url: 'https://yandex.ru/support/webmaster/search-appearance/games.html',
            },
        });
    });
});
