const snippet = require('../../../tools/data');

module.exports = snippet.createPage(
    {
        construct: [
            {
                description: 'Европейский вещательный совет может',
                host: 'tass.ru',
                sourcePassage: 'Читать на ТАСС',
                preview_content: [
                    {
                        content_type: 'paragraph',
                        text: [
                            'ЖЕНЕВА, 29 марта. /Корр. ТАСС Константин Прибытков/. ',
                            {
                                content_type: 'link',
                                text: 'Blick',
                                url: 'http://www.blick.ch/',
                            },
                            ' о возможном введении санкций в отношении.',
                        ],
                    },
                    {
                        content_type: 'paragraph',
                        text: '\'Мы напряженно работаем, чтобы найти решение, которое позволило бы всем 43 участникам.',
                    },
                    {
                        content_type: 'title',
                        text: 'Угроза санкций',
                    },
                    {
                        content_type: 'paragraph',
                        text: 'Украина получила право принимать \'Евровидение-2017\' после победы певицы Джамалы с песней \'1944\' на конкурсе в Стокгольме в 2016 году.',
                    },
                    {
                        content_type: 'table',
                        head: [{
                            cells: [{
                                text: [{
                                    content_type: 'link',
                                    text: 'Геохронологическая шкала',
                                    url: 'https://ru.wikipedia.org/wiki/Геохронологическая_шкала',
                                }],
                                colspan: 3,
                            }],
                        }],
                        rows: [
                            {
                                cells: [
                                    'Эон',
                                    'Эра',
                                    'Период',
                                ],
                            },
                            {
                                cells: [
                                    { text: 'Ф а н е р о з о й', rowspan: 3 },
                                    {
                                        text: [
                                            {
                                                content_type: 'link',
                                                text: 'Кайнозой',
                                                url: 'https://ru.wikipedia.org/wiki/Кайнозой',
                                            },
                                        ],
                                        rowspan: 3,
                                    },
                                    'Четвертичный',
                                ],
                            },
                            {
                                cells: [
                                    'Неоген',
                                ],
                            },
                            {
                                cells: [
                                    'Палеоген',
                                ],
                            },
                        ],
                    },
                ],
                pub_date: 1490799450,
                author: 'ALEX HEYGOM',
                related: [
                    {
                        agency: 'rg.ru',
                        sideblock_cgi_url: 'test_news_2',
                        sideblock_url: '/search/cache/touch?',
                        time: 1492953780,
                        title: 'Тестовая статья с очень большим заголовком. Строки на 2 наверное',
                        url: 'https://rg.ru/2017/04/23/bajkery-udarili-probegom-po-avtomobilistam.html',
                    },
                ],
                theme: 'light',
                title: 'Ложки нет',
                type: 'extended-preview',
                original_url: 'http://tass.ru/kultura/4136710',
                url: 'http://tass.ru/kultura/4136710',
            },
        ],
        doctitle: 'NEWS',
        url: 'http://tass.ru/kultura/4136710',
    }
);
