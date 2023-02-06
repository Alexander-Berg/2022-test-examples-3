({
    block: 'x-page',
    title: 'tabbar',
    content: [
        {
            block: 'gemini-simple',
            attrs: {style: 'width: 600px; padding: 60px;'},
            content: {
                block: 'tabbar',
                menuMods: {size: 'm'},
                visibleTabsCount: 3,
                content: [
                    {
                        elem: 'tab',
                        content: {
                            block: 'link',
                            mods: {theme: 'normal'},
                            url: '//yandex.ru/search/',
                            text: 'Поиск'
                        }
                    },
                    {
                        elem: 'tab',
                        content: {
                            block: 'link',
                            mods: {theme: 'normal'},
                            url: '//yandex.ru/images/search/',
                            text: 'Картинки'
                        }
                    },
                    {
                        elem: 'tab',
                        content: {
                            block: 'link',
                            mods: {theme: 'normal'},
                            url: '//yandex.ru/video/search/',
                            text: 'Видео'
                        }
                    },
                    {
                        elem: 'tab',
                        content: {
                            block: 'link',
                            mods: {theme: 'normal'},
                            url: '//yandex.ru/maps/',
                            text: 'Карты'
                        }
                    },
                    {
                        elem: 'tab',
                        content: {
                            block: 'link',
                            mods: {theme: 'normal'},
                            url: '//music.yandex.ru/',
                            text: 'Музыка'
                        }
                    },
                    {
                        elem: 'tab',
                        content: {
                            block: 'link',
                            mods: {theme: 'normal'},
                            url: '//yandex.ru/market/',
                            text: 'Маркет'
                        }
                    }
                ]
            }
        }
    ]
});
