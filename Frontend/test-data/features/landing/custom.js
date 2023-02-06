const snippet = require('../../../tools/data');

module.exports = snippet.createPage({
    adv: true,
    construct: [
        {
            amp_meta: {
                analytics: [
                    {
                        id: '46417413',
                        mode: 'goals',
                        type: 'Yandex',
                    },
                ],
            },
            preview_content: [
                {
                    __meta: {
                        screen_lines: 67,
                    },
                    content: [
                        {
                            active: true,
                            content: [
                                {
                                    content: [
                                        {
                                            content: {
                                                content_type: 'image',
                                                src_set: [
                                                    {
                                                        src: 'https://avatars.mds.yandex.net/get-turbo/212562/32161851-9e01-4988-9afb-34af41d7c2fd/',
                                                    },
                                                ],
                                            },
                                            content_type: 'header',
                                        },
                                        {
                                            alt: 'Яндекс Лендинг 2',
                                            content_type: 'image',
                                            src_set: [
                                                {
                                                    color: '#B5A8A2',
                                                    src: 'https://avatars.mds.yandex.net/get-turbo/399060/ef62cec1-2ac0-4830-b5b6-c3e601034490/',
                                                },
                                            ],
                                        },
                                        {
                                            content_type: 'title',
                                            text: 'Найдётся всё!',
                                        },
                                        {
                                            content_type: 'description',
                                            text: '«Я́ндекс» — российская транснациональная компания, владеющая одноимённой системой поиска в Сети, интернет-порталами и службами в нескольких странах',
                                        },
                                    ],
                                    content_type: 'cover',
                                },
                                {
                                    content: [
                                        {
                                            content: 'Бренд',
                                            content_type: 'title',
                                        },
                                        {
                                            content: 'Поисковый продукт «Яндекс» появился в 1993 году. Название системы — Яндекс, Яndex, — придумали вместе Аркадий Волож и Илья Сегалович',
                                            content_type: 'paragraph',
                                        },
                                        {
                                            content: 'Рекламные кампании Яндекса',
                                            content_type: 'title',
                                        },
                                        {
                                            content: 'На протяжении многих лет «Яндекс» почти не обращался к услугам сторонних рекламных агентств, выступая как в роли заказчика, так и исполнителя. Ситуация переменилась во второй половине нулевых.',
                                            content_type: 'paragraph',
                                        },
                                        {
                                            content: 'Качество поиска',
                                            content_type: 'title',
                                        },
                                        {
                                            content: [
                                                'Копирующие или переписывающие информацию с других ресурсов и не создающие оригинального контента.',
                                                'Единственной целью которых является перенаправление пользователя на другой ресурс, автоматически (редирект) или добровольно.',
                                                'С автоматически сгенерированным (бессмысленным) текстом.',
                                            ],
                                            content_type: 'list',
                                        },
                                        {
                                            content: [
                                                'В том же 2001 году появляется система контекстной рекламы «',
                                                {
                                                    content: 'Яндекс.Директ',
                                                    content_type: 'link',
                                                    url: 'https://direct.yandex.ru',
                                                },
                                                '»',
                                            ],
                                            content_type: 'paragraph',
                                        },
                                    ],
                                    content_type: 'markup',
                                },
                                {
                                    content: {
                                        action: 'next',
                                        content_type: 'button',
                                        goal: 'g1',
                                        text: 'Оставить заявку',
                                    },
                                    content_type: 'sticky',
                                    position: 'bottom',
                                },
                                {
                                    content_type: 'footer',
                                    copyrights: {
                                        content_type: 'paragraph',
                                        text: [
                                            {
                                                content_type: 'link',
                                                text: 'Пользовательское соглашение',
                                                url: 'https://yandex.ru/legal/rules/',
                                            },
                                            ' компании Яндекс',
                                        ],
                                    },
                                },
                            ],
                            content_type: 'screen',
                            id: 'welcome',
                            next: 'form',
                        },
                        {
                            content: [
                                {
                                    content: [
                                        {
                                            content: {
                                                content_type: 'image',
                                                src_set: [
                                                    {
                                                        src: 'https://avatars.mds.yandex.net/get-turbo/212562/32161851-9e01-4988-9afb-34af41d7c2fd/',
                                                    },
                                                ],
                                            },
                                            content_type: 'header',
                                        },
                                        {
                                            content_type: 'divider',
                                        },
                                    ],
                                    content_type: 'cover',
                                },
                                {
                                    content_type: 'title',
                                    text: 'Заявка',
                                },
                                {
                                    content: [
                                        {
                                            content: [
                                                {
                                                    content_type: 'input',
                                                    label: 'Ваше имя',
                                                    name: 'name',
                                                    placeholder: 'Иван Петров',
                                                    type: 'text',
                                                },
                                            ],
                                            content_type: 'form-line',
                                        },
                                        {
                                            content: [
                                                {
                                                    content_type: 'input',
                                                    label: 'Телефон',
                                                    name: 'phone',
                                                    placeholder: '8-XXX-XXX-XX-XX',
                                                    type: 'tel',
                                                    validation: [
                                                        {
                                                            phone: true,
                                                            required: true,
                                                        },
                                                    ],
                                                },
                                            ],
                                            content_type: 'form-line',
                                        },
                                        {
                                            content: [
                                                {
                                                    content_type: 'input',
                                                    label: 'Email',
                                                    name: 'email',
                                                    placeholder: 'mail@example.com',
                                                    type: 'email',
                                                    validation: [
                                                        {
                                                            email: true,
                                                            required: true,
                                                        },
                                                    ],
                                                },
                                            ],
                                            content_type: 'form-line',
                                        },
                                        {
                                            content: [
                                                {
                                                    content_type: 'checkbox',
                                                    name: 'agreement',
                                                    text: 'Да, я согласен со всем, что вы скажете',
                                                },
                                            ],
                                            content_type: 'form-line',
                                        },
                                        {
                                            content: [
                                                'Нажимая "Отправить", даю согласие ',
                                                'ООО «ЯНДЕКС»',
                                                ' на обработку введенной информации и принимаю условия ',
                                                {
                                                    content_type: 'link',
                                                    text: 'Пользовательского соглашения',
                                                    url: 'https://yandex.ru/legal/rules/',
                                                },
                                                ' ООО «Яндекс»',
                                            ],
                                            content_type: 'agreement',
                                            full_text: {
                                                text: [
                                                    {
                                                        content_type: 'paragraph',
                                                        text: [
                                                            'Нажимая "Отправить", даю согласие ',
                                                            'ООО «ЯНДЕКС» , ИНН 7736207543',
                                                            ' на обработку введенной информации в целях, указанных в анкете, в порядке, предусмотренном политикой конфиденциальности ',
                                                            '',
                                                            ' компании. Анкета создана с использование ресурсов Яндекса, которые также предоставляют пользователя возможность автозаполнения анкет, в связи с чем также даю согласие ООО «Яндекс» на использование указанной в анкете информации в целях исполнения ',
                                                            {
                                                                content_type: 'link',
                                                                text: 'Пользовательского соглашения',
                                                                url: 'https://yandex.ru/legal/rules/',
                                                            },
                                                            '.',
                                                        ],
                                                    },
                                                ],
                                                title: 'Соглашение',
                                            },
                                            type: 'short',
                                        },
                                        {
                                            content: {
                                                content: [
                                                    {
                                                        content: {
                                                            action: 'prev',
                                                            content_type: 'button',
                                                            text: 'Назад',
                                                        },
                                                        content_type: 'column',
                                                    },
                                                    {
                                                        content: {
                                                            content_type: 'button',
                                                            goal: 'g3',
                                                            text: 'Отправить',
                                                            type: 'submit',
                                                        },
                                                        content_type: 'column',
                                                    },
                                                ],
                                                content_type: 'row',
                                            },
                                        },
                                    ],
                                    content_type: 'fieldset',
                                },
                                {
                                    content_type: 'footer',
                                    copyrights: {
                                        content_type: 'paragraph',
                                        text: [
                                            {
                                                content_type: 'link',
                                                text: 'Пользовательское соглашение',
                                                url: 'https://yandex.ru/legal/rules/',
                                            },
                                            ' компании Яндекс',
                                        ],
                                    },
                                },
                            ],
                            content_type: 'screen',
                            goal: {
                                '46417413': 'g2',
                            },
                            id: 'form',
                            next: 'thanks',
                        },
                        {
                            content: [
                                {
                                    content: [
                                        {
                                            content: {
                                                content_type: 'image',
                                                src_set: [
                                                    {
                                                        src: 'https://avatars.mds.yandex.net/get-turbo/212562/32161851-9e01-4988-9afb-34af41d7c2fd/',
                                                    },
                                                ],
                                            },
                                            content_type: 'header',
                                        },
                                        {
                                            content_type: 'divider',
                                        },
                                    ],
                                    content_type: 'cover',
                                },
                                {
                                    content_type: 'title',
                                    text: 'Спасибо за заявку!',
                                },
                                {
                                    content: [
                                        {
                                            content_type: 'paragraph',
                                            text: 'С вам свяжутся',
                                        },
                                    ],
                                    content_type: 'markup',
                                },
                                {
                                    content: {
                                        action: 'next',
                                        content_type: 'utm-button',
                                        goal: 'g7',
                                        text: 'Посетить сайт',
                                        url: 'https://yandex.ru',
                                    },
                                    content_type: 'actions',
                                },
                                {
                                    content_type: 'footer',
                                    copyrights: {
                                        content_type: 'paragraph',
                                        text: [
                                            {
                                                content_type: 'link',
                                                text: 'Пользовательское соглашение',
                                                url: 'https://yandex.ru/legal/rules/',
                                            },
                                            ' компании Яндекс',
                                        ],
                                    },
                                },
                            ],
                            content_type: 'screen',
                            form_result: true,
                            goal: 'g6',
                            id: 'thanks',
                        },
                    ],
                    content_type: 'form',
                    meta: 'HlsoNQVaLS0Nc0xmHCMRMEYPLhcqBTcyfVFtW2sBBTsvKwQjNlslW2hRP1Y2OWICJCoeLwEEZgpaThcgFhxdIg0KChQWWj9DaRqHhJPwn8CE3Z3un8xvCndZaSQDNDgYBFMOI1VCOiUAUGYRFllREC8VO1UhJj0tFzEYNQ8oEBY9Yzklbz5jEgFVBBo8GBgMGFtUeQ5kAlUGBC9FEhZJSjcwTXEYAgwoKF0DKE1CAWEdUFQPZ0lpDgA1O0FqZQYsHzoBa00UDgwLU1ZmDlUuQhsVbEA2NRMqHlEyDgo=',
                    name: 'test-landing-2',
                    next: 'thanks',
                    type: 'landing',
                },
            ],
        },
    ],
    pageId: 'test-landing-2',
    theme: {
        button: {
            action: {
                background: '#e61400',
                color: '#fff',
            },
            loading: {
                background: '#e61400',
                backgroundCover: '#ff0000',
                color: '#fff',
            },
        },
        cover: {
            background: '#e61400',
            description: {
                color: '#fff',
            },
            divider: {
                color: '#e61400',
            },
            header: {
                title: {
                    color: '#fff',
                },
            },
            title: {
                color: '#fff',
            },
        },
        preset: 'default',
    },
    title: 'Яндекс Лендинг 2',
});
