module.exports = {
    type: 'snippet',
    request_text: 'Кот Шрёдингера',
    data_stub: {
        num: 0,
        url: 'https://ru.wikipedia.org/wiki/%D0%9A%D0%BE%D1%82_%D0%A8%D1%80%D1%91%D0%B4%D0%B8%D0%BD%D0%B3%D0%B5%D1%80%D0%B0',
        host: 'ru.wikipedia.org',
        title: '\u0007[Кот\u0007] \u0007[Шрёдингера\u0007] — Википедия',
        doctitle: '\u0007[Кот\u0007] \u0007[Шрёдингера\u0007] — Википедия',
        green_url: 'ru.wikipedia.org/…\u0007[Кот\u0007]_\u0007[Шрёдингера\u0007]',
        construct: [
            {
                baobab: {
                    path: '/snippet/snippet_with_extended_preview'
                },
                linkOverlayFlag: 'blue-link-full',
                snip_with_ext_preview: {
                    can_show_in_sideblock: true,
                    sideblock_cgi_url: 'sbpfe565c869a903dae37ae95deeac4f10df2b3dba5',
                    sideblock_fallback: 'https://ru.wikipedia.org/wiki/%D0%9A%D0%BE%D1%82_%D0%A8%D1%80%D1%91%D0%B4%D0%B8%D0%BD%D0%B3%D0%B5%D1%80%D0%B0',
                    sideblock_url: '/search/cache/touch?'
                },
                type: 'snippet_with_extended_preview'
            }
        ],
        snippets: {
            main: {
                counter_prefix: '/snippet/generic/',
                headline: '\u0007[Кот\u0007] \u0007[Шрёдингера\u0007] — мысленный эксперимент, предложенный австрийским физиком-теоретиком, одним из создателей квантовой механики, Эрвином \u0007[Шрёдингером\u0007], \u0007[которым\u0007] он хотел показать неполноту квантовой механики при переходе от субатомных систем...',
                is_generic: 1,
                template: 'generic',
                type: 'mediawiki_snip'
            },
            pre: [
                {
                    applicable: 1,
                    counter_prefix: '/snippet/mobile_beauty_url/',
                    data: {
                        full_url: 'https://ru.wikipedia.org/wiki/%D0%9A%D0%BE%D1%82_%D0%A8%D1%80%D1%91%D0%B4%D0%B8%D0%BD%D0%B3%D0%B5%D1%80%D0%B0',
                        hilited_url: 'ru.wikipedia.org/…\u0007[Кот\u0007]…',
                        type: 'mobile_beauty_url'
                    },
                    slot: 'pre',
                    slot_rank: 0,
                    template: 'mobile_beauty_url',
                    type: 'mobile_beauty_url'
                }
            ]
        }
    }
};
