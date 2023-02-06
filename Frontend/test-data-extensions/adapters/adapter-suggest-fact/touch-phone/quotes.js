module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        construct: {
            colors: {
                negative: 'negative',
                neutral: 'neutral',
                positive: 'positive'
            },
            baobab: {
                path: '/wiz/suggest_fact'
            },
            headline: 'Платина (NYMEX.PL) сегодня, 15:00 МСК',
            host: 'https://news.yandex.ru',
            path: {
                items: [
                    {
                        text: 'Яндекс.Новости',
                        url: 'https://news.yandex.ru/quotes/1505.html'
                    }
                ]
            },
            subtype: 'quotes',
            text: [
                [
                    {
                        currency: 'USD',
                        text: 918.8
                    },
                    {
                        style_type: 'positive',
                        currency: 'RUR',
                        text: 1.90
                    }
                ]
            ],
            type: 'suggest_fact',
            url: 'https://news.yandex.ru/quotes/1505.html',
            voiceInfo: {
                ru: [
                    {
                        lang: 'ru-RU',
                        text: "Цена' на Платину выросла на 0.21 процента, и составила 918 долларов 80 центов за тройскую унцию"
                    }
                ]
            },
            wizplace: 'important'
        }
    }
};
