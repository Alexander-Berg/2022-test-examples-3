module.exports = {
    regular: {
        items: [
            {
                text: 'Главная',
                url: '/',
                active: true,
            },
            {
                text: 'Не главная',
                url: 'http://yandex.ru',
            },
            {
                text: 'В новой вкладке',
                url: 'http://yandex.ru',
                target: '_blank',
            },
            {
                text: 'Кнопка',
                eventType: 'button-test',
                children: [
                    {
                        text: 'Пункт 1',
                    },
                    {
                        text: 'Пункт 2',
                    },
                ],
            },
        ],
    },
    empty: {
        items: [],
    },
    single: {
        items: [
            {
                text: 'Главная',
                url: '/',
            },
        ],
    },
    active: {
        items: [
            {
                text: 'Главная',
                url: '/',
                active: true,
            },
        ],
    },
    buttons: {
        items: [
            {
                text: 'Кнопка 1',
            },
            {
                text: 'Кнопка 2',
                active: true,
            },
            {
                text: 'Кнопка 3',
                eventType: 'button-test',
            },
            {
                text: 'Кнопка 4',
                children: [
                    {
                        text: 'Пункт 1',
                    },
                    {
                        text: 'Пункт 2',
                    },
                ],
            },
            {
                text: 'Кнопка 5',
                eventType: 'button-test',
                children: [
                    {
                        text: 'Пункт 1',
                    },
                    {
                        text: 'Пункт 2',
                    },
                ],
            },
        ],
    },
    links: {
        items: [
            {
                text: 'Ссылка 1',
                url: '/',
            },
            {
                text: 'Ссылка 2',
                url: '/',
                active: true,
            },
            {
                text: 'Ссылка 3',
                url: 'http://yandex.ru',
            },
            {
                text: 'Ссылка 4',
                url: 'http://yandex.ru',
                target: '_blank',
            },
            {
                text: 'Ссылка 5',
                url: '/',
                children: [
                    {
                        text: 'Пункт 1',
                    },
                    {
                        text: 'Пункт 2',
                    },
                ],
            },
        ],
    },
};
