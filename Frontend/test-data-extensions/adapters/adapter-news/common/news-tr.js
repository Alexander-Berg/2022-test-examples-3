var imageUrlStub = require('@yandex-int/gemini-serp-stubs').imageUrlStub;

module.exports = {
    type: 'snippet',
    request_text: 'новости сша',
    extensions: {
        reqdata: { tld: 'com.tr' },
        i18n: { language: 'tr' }
    },
    data_stub: {
        markers: {
            WizardPos: '0'
        },
        num: 0,
        snippets: {
            full: {
                type: 'news',
                template: 'news',
                counter_prefix: '/snippet/news/',
                types: {
                    kind: 'wizard',
                    all: ['snippets', 'news'],
                    main: 'news'
                },
                passages: [],
                story: {
                    docs: [{
                        touch_picture: {
                            width: 480,
                            height: 240,
                            id: 1,
                            src: imageUrlStub(480, 240)
                        },
                        title: 'Новости США: Obama\'s fail, special symbols: "<`&>\'',
                        pda_url: 'http://riafan.ru/562314-novosti-ssha-amerika-priznala-rossiyu-glavnoi-ugrozoi-tramp-izvinilsya-za-seksistskie-vyskazyvaniya',
                        url: 'http://riafan.ru/562314-novosti-ssha-amerika-priznala-rossiyu-glavnoi-ugrozoi-tramp-izvinilsya-za-seksistskie-vyskazyvaniya',
                        agency: 'riafan.ru',
                        snippet: 'На сайте WikiLeaks обнародована часть переписки главы предвыборной кампании Хиллари Клинтон Джона Подесты. Дональд Трамп опубликовал видео с извинениями в адрес оскорбленных им женщин.',
                        time: '2016-10-09T16:52:31.000Z',
                    }, {
                        title: 'Новости дня: США и Россия уже дошли до...',
                        pda_url: 'https://m.rusdialog.ru/news/82360_1476265734',
                        url: 'https://www.rusdialog.ru/news/82360_1476265734',
                        agency: 'rusdialog.ru',
                        snippet: 'Последние новости. 12:54 США и Россия уже дошли до открытого противостояния, новая мировая война не за горами – вице-премьер Турции.',
                        time: '2016-10-12T09:54:54.000Z'
                    }, {
                        title: 'Новости дня: К концу года дипломатические споры...',
                        pda_url: 'https://m.rusdialog.ru/news/82362_1476268048',
                        url: 'https://www.rusdialog.ru/news/82362_1476268048',
                        agency: 'rusdialog.ru',
                        snippet: 'Все новости ». Выбор редакции. 13:56 Российская подлодка \'Ясень\' впервые за 20 лет бросит вызов субмарине \'Вирджиния\' и всему флоту США: кто победит?',
                        time: '2016-10-12T10:36:28.000Z'
                    }, {
                        title: 'Новости США: Obama\'s fail, special symbols: "<`&>\'',
                        pda_url: 'https://m.rusdialog.ru/news/82360_1476265734',
                        url: 'https://www.rusdialog.ru/news/82360_1476265734',
                        agency: 'rusdialog.ru',
                        snippet: 'Последние новости. 12:54 США и Россия уже дошли до открытого противостояния, новая мировая война не за горами – вице-премьер Турции.',
                        time: '2016-10-12T09:54:54.000Z'
                    }]
                },
                wizard_mobile_title: 'Güncel haberler',
                touch_picture: {
                    width: 480,
                    height: 240,
                    id: 1,
                    src: imageUrlStub(480, 240)
                },
                part: 'full',
                host: 'news.yandex.ru',
                applicable: 1,
                slot_rank: 0,
                serp_info: {
                    format: 'json',
                    type: 'news',
                    flat: 1
                },
                headline: '',
                slot: 'full'
            }
        }
    }
};
