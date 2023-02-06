module.exports = function() {
    return [
        {
            applicable: 1,
            context: [
                {
                    full_url: '//yandex.ru/search/?text=',
                    is_best: '0',
                    is_current: '1',
                    name: 'serp'
                },
                {
                    full_url: '//yandex.ru/images/search?text=',
                    is_best: '0',
                    is_current: '0',
                    name: 'images'
                },
                {
                    full_url: '//yandex.ru/video/search?text=',
                    is_best: '0',
                    is_current: '0',
                    name: 'video'
                },
                {
                    full_url: '//maps.yandex.ru/?source=serp_navig&text=',
                    is_best: '0',
                    is_current: '0',
                    name: 'maps'
                },
                {
                    full_url: '//market.yandex.ru/search.xml?clid=521&cvredirect=2&text=',
                    is_best: '0',
                    is_current: '0',
                    name: 'market'
                },
                {
                    info: {
                        is_default: '1',
                        redirect_info: {
                            type: '0'
                        }
                    }
                }
            ],
            counter_prefix: '/wiz/navigation_context/',
            type: 'navigation_context',
            types: {
                all: [
                    'wizard',
                    'navigation_context'
                ],
                extra: [],
                kind: 'wizard',
                main: 'navigation_context'
            },
            wizplace: 'navigation'
        }
    ];
};
