const data = require('../../tools/data');

module.exports = data.createPage({
    content: [
        {
            block: 'container',
            mods: { center: true },
            content: {
                block: 'row',
                content: {
                    elem: 'col',
                    elemMods: { size: '18' },
                    content: {
                        block: 'example',
                        content: 'Header',
                    },
                },
            },
        },
        {
            block: 'container',
            mods: { center: true, main: true },
            content: {
                block: 'row',
                content: [
                    {
                        elem: 'col',
                        elemMods: { size: '12' },
                        content: [
                            {
                                content_type: 'title',
                                size: 'l',
                                content: 'Формы обратной связи и новые типы медиаконтента',
                            },
                        ],
                    },
                    {
                        elem: 'col',
                        elemMods: { size: '6' },
                        content: {
                            block: 'example',
                            content: [
                                {
                                    content_type: 'related',
                                    items: [
                                        {
                                            agency: 'vedomosti.ru',
                                            agency_logo: '//avatars.mds.yandex.net/get-turbo/372683/2a0000015e5bd92255e0f5c0d1a2760fe199/',
                                            sideblock_cgi_url: 'https://www.vedomosti.ru/business/news/2018/01/15/747831-gazprom-reshenie-suda-naftogazom',
                                            sideblock_url: '/search/cache/touch?',
                                            title: 'Регрессное требование, как можно доказать с помощью',
                                            url: 'https://www.vedomosti.ru/business/news/2018/01/15/747831-gazprom-reshenie-suda-naftogazom',
                                        },
                                        {
                                            agency: 'vedomosti.ru',
                                            agency_logo: '//avatars.mds.yandex.net/get-turbo/372683/2a0000015e5bd92255e0f5c0d1a2760fe199/',
                                            sideblock_cgi_url: 'https://www.vedomosti.ru/business/news/2018/01/05/747106-energeticheskoi-nezavisimosti',
                                            sideblock_url: '/search/cache/touch?',
                                            title: 'Судебное решение гарантирует штраф, делая этот вопрос чрезвычайно актуальным.',
                                            url: 'https://www.vedomosti.ru/business/news/2018/01/05/747106-energeticheskoi-nezavisimosti',
                                        },
                                        {
                                            agency: 'vedomosti.ru',
                                            agency_logo: '//avatars.mds.yandex.net/get-turbo/372683/2a0000015e5bd92255e0f5c0d1a2760fe199/',
                                            sideblock_cgi_url: 'https://www.vedomosti.ru/business/news/2018/01/17/748132-naftogaz-nazval',
                                            sideblock_url: '/search/cache/touch?',
                                            title: 'Закрепленная в данном пункте императивная норма указывает',
                                            url: 'https://www.vedomosti.ru/business/news/2018/01/17/748132-naftogaz-nazval',
                                        },
                                    ],
                                },
                            ],
                        },
                    },
                ],
            },
        },
        {
            content_type: 'footer',
            col: '24',
            url: 'https://pikabu.ru/story/statya_iz_svezhego_na_pikabu_upominaet_statyu_iz_goryachego_na_lenteru_v_kotoroy_upominayut_polzovatelya_pikabu_5045278',
        },
    ],
    platform: 'desktop',
});
