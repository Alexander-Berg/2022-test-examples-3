/**
 * Мок для провайдера, всегда возвращающий статические данные
 */

BEM.decl('suggest2-provider', {

    get: function(text, pos, callback) {
        var self = this,
            completions = [
                [
                    '',
                    'простой запрос'
                ], [
                    '',
                    'второй простой запрос'
                ], [
                    '',
                    'запрос по видеозаписям',
                    {
                        label: 'Видео',
                        target: '_self',
                        url: 'https://yandex.ru/video/search?text=запрос%20по%20видеозаписям&safety=1'
                    }
                ], [
                    '',
                    'второй запрос по видео',
                    {
                        label: 'Видео',
                        url: 'https://yandex.ru/video/search?text=второй%20запрос%20по%20видео&safety=1'
                    }
                ], [
                    '',
                    'ещё один запрос по видео',
                    {label: 'Видео'}
                ], [
                    '',
                    'запрос с отсутствием лейбла'
                ], [
                    '',
                    'второй запрос без лейбла'
                ], [
                    '',
                    'бременские музыканты',
                    {
                        label: 'Музыка',
                        url: '//music.yandex.ru/search?text=бременские%20музыканты'
                    }
                ], [
                    '',
                    'гимны',
                    {
                        label: 'Музыка',
                        url: 'music.yandex.ru/search?text=гимны'
                    }
                ], [
                    'html',
                    'html-тип',
                    {
                        label: 'Музыка',
                        body: '<span class=\"suggest2-item__text\">марш империи</span>'
                    }
                ], [
                    'bemjson',
                    'популярная классика',
                    {
                        label: 'Музыка',
                        bemjson: {elem: 'text', content: 'популярная классика'},
                        url: 'http://music.yandex.ru/users/ya.playlist/playlists/1099'
                    }
                ]
            ];

        this.afterCurrentEvent(function() {
            callback.call(self, text, pos, {
                orig: text,
                items: completions,
                meta: null
            });
        });
    }
});
