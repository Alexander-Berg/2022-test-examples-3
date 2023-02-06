const snippet = require('../../../tools/data');

module.exports = snippet.createPage({
    title: 'Яндекс Лендинг 1',
    adv: true,
    theme: {
        preset: 'default',
        button: {
            action: {
                background: '#ffdb4d',
                color: '#000',
            },
            loading: {
                background: '#ffdb4d',
                color: '#000',
                backgroundCover: '#f7c600',
            },
        },
        logo: {
            backgroundImage: 'https://avatars.mds.yandex.net/get-turbo/399060/2e12b710-172c-4256-bc23-9901751842ea/svg',
        },
    },
    pageId: 'test-landing-1',
    construct: [
        {
            amp_meta: {
                analytics: [
                    {
                        id: '46417413',
                        type: 'Yandex',
                        mode: 'goals',
                    },
                    {
                        id: 'UA-122962992-1',
                        type: 'Google',
                    },
                ],
            },
            preview_content: [
                {
                    content_type: 'form',
                    type: 'landing',
                    name: 'test-landing-1',
                    next: 'thanks',
                    meta: 'HlsoNQVaLS0Nc0xmHCMRMEYPLhcqBTcyfVJtW2sBBTsvKwQjNlslW2hRP1Y2OWICJCoeLwEEZglaThcgFhxdIg0KChQWWj9DaRqHhJPwn8CE3Z3un8xvCXdZaSQDNDgYBFMOI1VCOiUAUGYRFllREC8VO1UhJj0tFzEYNQ8oEBY9Yzklbz5jEgFVBBo8GBgMGFtUeQ5kAlUGBC9FEhZJSjdvETsYHB1jfREWIQNaB29RXkkeLBwlGwlvdRhkKRcpFXZeawNTAh1FXVgnBFUiThlDdFhxLBc7BlEyDgo=',
                    content: [
                        {
                            content_type: 'screen',
                            active: true,
                            id: 'welcome',
                            next: 'form',
                            goal: 'g11',
                            goals: [{ type: 'google', category: 'screen', action: 'active' }],
                            content: [
                                {
                                    content_type: 'cover',
                                    content: [
                                        {
                                            content_type: 'header',
                                            content: {
                                                content_type: 'image',
                                                type: 'logo',
                                                cover: true,
                                                src_set: [
                                                    {
                                                        src: 'https://avatars.mds.yandex.net/get-turbo/399060/2e12b710-172c-4256-bc23-9901751842ea/svg',
                                                    },
                                                ],
                                            },
                                        },
                                        {
                                            content_type: 'image',
                                            type: 'default',
                                            cover: true,
                                            src_set: [
                                                {
                                                    src: 'https://avatars.mds.yandex.net/get-turbo/399060/ef62cec1-2ac0-4830-b5b6-c3e601034490/',
                                                    color: '#B5A8A2',
                                                },
                                            ],
                                            alt: 'Яндекс Лендинг 1',
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
                                },
                                {
                                    content_type: 'actions',
                                    content: {
                                        content_type: 'button',
                                        text: 'Оставить заявку',
                                        action: 'next',
                                        goal: 'g1',
                                    },
                                },
                                {
                                    content_type: 'footer',
                                    copyrights: {
                                        content_type: 'paragraph',
                                        text: '© 2017 ООО «Яндекс»',
                                    },
                                },
                            ],
                        },
                        {
                            content_type: 'screen',
                            id: 'form',
                            next: 'thanks',
                            goal: 'g2',
                            content: [
                                {
                                    content_type: 'cover',
                                    content: [
                                        {
                                            content_type: 'header',
                                            content: {
                                                content_type: 'image',
                                                type: 'logo',
                                                cover: true,
                                                src_set: [
                                                    {
                                                        src: 'https://avatars.mds.yandex.net/get-turbo/399060/2e12b710-172c-4256-bc23-9901751842ea/svg',
                                                    },
                                                ],
                                            },
                                        },
                                        {
                                            content_type: 'divider',
                                        },
                                    ],
                                },
                                {
                                    content_type: 'title',
                                    text: 'Заявка',
                                },
                                {
                                    content_type: 'fieldset',
                                    content: [
                                        {
                                            content_type: 'form-line',
                                            content: [
                                                {
                                                    content_type: 'input',
                                                    name: 'name',
                                                    placeholder: 'Иван Петров',
                                                    type: 'text',
                                                    label: 'Ваше имя',
                                                },
                                            ],
                                        },
                                        {
                                            content_type: 'form-line',
                                            content: [
                                                {
                                                    content_type: 'input',
                                                    name: 'phone',
                                                    placeholder: 'В любом формате',
                                                    type: 'tel',
                                                    label: 'Телефон',
                                                    validation: [
                                                        {
                                                            required: true,
                                                        },
                                                    ],
                                                },
                                            ],
                                        },
                                        {
                                            content_type: 'form-line',
                                            content: [
                                                {
                                                    content_type: 'select',
                                                    name: 'city',
                                                    label: 'Город',
                                                    options: [
                                                        {
                                                            text: 'Москва',
                                                            value: 'Москва',
                                                        },
                                                        {
                                                            text: 'Минск',
                                                            value: 'Минск',
                                                        },
                                                        {
                                                            text: 'Казань',
                                                            value: 'Казань',
                                                        },
                                                        {
                                                            text: 'Новосибирск',
                                                            value: 'Новосибирск',
                                                        },
                                                    ],
                                                },
                                            ],
                                        },
                                        {
                                            content_type: 'form-line',
                                            content: [
                                                {
                                                    content_type: 'textarea',
                                                    name: 'comment',
                                                    label: 'Вопрос/комментарий',
                                                },
                                            ],
                                        },
                                        {
                                            content_type: 'agreement',
                                            type: 'short',
                                            full_text: {
                                                title: 'Соглашение',
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
                                            },
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
                                        },
                                        {
                                            content: {
                                                content_type: 'row',
                                                content: [
                                                    {
                                                        content_type: 'column',
                                                        content: {
                                                            content_type: 'button',
                                                            text: 'Назад',
                                                            action: 'prev',
                                                        },
                                                    },
                                                    {
                                                        content_type: 'column',
                                                        content: {
                                                            content_type: 'button',
                                                            text: 'Отправить',
                                                            type: 'submit',
                                                            goal: 'g3',
                                                        },
                                                    },
                                                ],
                                            },
                                        },
                                    ],
                                },
                                {
                                    content_type: 'footer',
                                    copyrights: {
                                        content_type: 'paragraph',
                                        text: '© 2017 ООО «Яндекс»',
                                    },
                                },
                            ],
                        },
                        {
                            content_type: 'screen',
                            id: 'thanks',
                            form_result: true,
                            goal: 'g6',
                            content: [
                                {
                                    content_type: 'cover',
                                    content: [
                                        {
                                            content_type: 'header',
                                            content: {
                                                content_type: 'image',
                                                type: 'logo',
                                                cover: true,
                                                src_set: [
                                                    {
                                                        src: 'https://avatars.mds.yandex.net/get-turbo/399060/2e12b710-172c-4256-bc23-9901751842ea/svg',
                                                    },
                                                ],
                                            },
                                        },
                                        {
                                            content_type: 'divider',
                                        },
                                    ],
                                },
                                {
                                    content_type: 'title',
                                    text: 'Спасибо за заявку!',
                                },
                                {
                                    content_type: 'markup',
                                    content: [
                                        {
                                            content_type: 'paragraph',
                                            text: 'Специалист отдела продаж свяжется с Вами в ближайщее (рабочее) время и проконсультироует по любым вопросам.',
                                        },
                                        {
                                            content_type: 'paragraph',
                                            text: 'А пока, можете посетит наш официальный сайт.',
                                        },
                                    ],
                                },
                                {
                                    content_type: 'actions',
                                    content: {
                                        content_type: 'utm-button',
                                        text: 'Посетить сайт',
                                        action: 'next',
                                        url: 'https://yandex.ru',
                                        goal: 'g7',
                                    },
                                },
                                {
                                    content_type: 'footer',
                                    copyrights: {
                                        content_type: 'paragraph',
                                        text: '© 2017 ООО «Яндекс»',
                                    },
                                },
                            ],
                        },
                    ],
                },
            ],
        },
    ],
});
