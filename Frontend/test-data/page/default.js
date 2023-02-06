const data = require('../../tools/data');

module.exports = data.createPage({
    content: [
        {
            content_type: 'cover',
            content: [
                { content_type: 'divider' },
                {
                    content_type: 'title',
                    content: 'В Москве ограничат движение в связи с репетициями Парада Победы',
                },
                {
                    content_type: 'subtitle',
                    content: 'В Москве ограничат движение в связи с репетициями Парада Победы',
                },
                {
                    content_type: 'description',
                    content: {
                        content_type: 'date',
                        content: 1493211240,
                    },
                },
            ],
        },
        {
            content_type: 'lead',
            content: 'В Москве ограничат движение в связи с репетициями Парада Победы',
        },
        {
            content_type: 'paragraph',
            text: [
                'ЖЕНЕВА, 29 марта. /Корр. ТАСС Константин Прибытков/. Европейский вещательный совет (ЕВС) рассмотрит вопрос о том, были ли нарушены правила при проведении майского конкурса \'Евровидение\' в Киеве и о принятии в этой связи мер в отношении украинской телекомпании после его завершения. Об этом заявили ТАСС в женевской штаб-квартире ЕВС, комментируя публикацию в швейцарской газете ',
                { content_type: 'link', text: 'Blick', url: 'http://www.blick.ch/' },
                'о возможном введении санкций в отношении Украины из-за отказа в допуске в страну российской участницы Юлии Самойловой.',
            ],
        },
        { content_type: 'title', text: 'Незнание закона не освобождает от ответственности', size: 'M' },
        { content_type: 'title', text: 'Незнание закона не освобождает от ответственности', size: 'S' },
        { content_type: 'title', text: 'Незнание закона не освобождает от ответственности', size: 'XS' },
        { content_type: 'title', text: 'Незнание закона не освобождает от ответственности', size: 'XXS' },
        { content_type: 'title', text: 'Незнание закона не освобождает от ответственности', size: 'XXXS' },
        { content_type: 'paragraph', text: 'Официальный представитель ЕВС Дейв Гудман заявил корреспонденту ТАСС:' },
        { content_type: 'paragraph', text: 'Отвечая на вопрос о том, может ли он подтвердить сообщение Blick о возможном введении санкций против Украины со стороны ЕВС, Гудман сказал: \'Согласно обычной процедуре, если будет установлено, что какая-либо телекомпания нарушила правила соревнования, то это будет рассмотрено руководящим комитетом и Референтной группой песенного конкурса \'Евровидение\' после его завершения в мае\'. Они решат, \'необходимо ли предпринять дальнейшие шаги\', пояснил официальный представитель, не уточнив, о каких мерах может идти речь.' },
        { content_type: 'title', text: 'Угроза санкций' },
        {
            content_type: 'paragraph',
            text: [
                'О том, что ЕВС может ввести санкции против Украины, временного отстранив ее от участия в \'Евровидении\' из-за ситуации с российской участницей Юлией Самойловой, ',
                { content_type: 'link', text: 'сообщила', url: 'http://tass.ru/mezhdunarodnaya-panorama/4136089' },
                ' швейцарская газета Blick со ссылкой на главу Европейского вещательного союза Ингрид Дельтенре.',
            ],
        },
        { content_type: 'title', text: 'Запрет СБУ' },
        { content_type: 'paragraph', text: 'Украина получила право принимать \'Евровидение-2017\' после победы певицы Джамалы с песней \'1944\' на конкурсе в Стокгольме в 2016 году. Полуфиналы состоятся в Киеве 9 и 11 мая, финал - 13-го. Главной ареной мероприятия станет Международный выставочный центр. В \'Евровидении-2017\' примут участие представители 43 стран.' },
        {
            content_type: 'blockquote',
            content: [
                {
                    content_type: 'paragraph',
                    text: [
                        '"Мы ',
                        {
                            content_type: 'link',
                            text: 'напряженно работаем',
                            url: 'http://yandex.ru/turbo',
                        },
                        ', чтобы найти решение, которое позволило бы всем 43 участникам принять участие в песенном конкурсе "Евровидение" этого года.',
                    ],
                },
            ],
        },
        {
            content_type: 'paragraph',
            text: [
                { content_type: 'link', content: 'link', url: 'http://example.com/abc' },
                ' ',
                { content_type: 'abbr', content: 'abbr', title: 'qwe' },
                ' ',
                { content_type: 'b', content: { content_type: 'i', content: 'bold and italic' } },
                ' ',
                { content_type: 'big', content: 'big' },
                ' ',
                { content_type: 'code', content: 'code' },
                ' ',
                { content_type: 'del', content: 'del' },
                ' ',
                { content_type: 'em', content: 'em' },
                ' ',
                { content_type: 'i', content: 'i' },
                ' ',
                { content_type: 'ins', content: 'ins' },
            ],
        },
        { content_type: 'pre', content: '  pre  ' },
        {
            content_type: 'paragraph',
            text: [
                { content_type: 'small', content: 'small' },
                ' ',
                { content_type: 'strong', content: 'strong' },
                ' ',
                { content_type: 'sub', content: 'sub' },
                ' ',
                { content_type: 'sup', content: 'sup' },
                ' ',
                { content_type: 'u', content: 'u' },
            ],
        },
        { content_type: 'meta', items: ['TASS', { content_type: 'link', text: 'Read more', url: 'https://tass.com' }] },
        {
            content_type: 'related',
            items: [
                {
                    time: 1492953780,
                    url: 'https://rg.ru/2017/04/23/bajkery-udarili-probegom-po-avtomobilistam.html',
                    sideblock_cgi_url: 'test_news_2',
                    agency: 'rg.ru',
                    title: 'Связанная статья с тайтлом в две строчечки',
                    sideblock_url: '/search/cache/touch?',
                },
            ],
        },
        {
            content_type: 'source',
            url: 'https://rg.ru',
        },
    ],
});
