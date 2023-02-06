({
    block: 'x-page',
    title: 'm-suggest',
    content: [
        {
            block: 'gemini',
            content: [
                {
                    block: 'm-suggest-item',
                    mods: {
                        type: 'b2b-departments',
                        size: 'm',
                        link: 'yes'
                    },
                    data: {title: 'Группа разработки программ для ЭВМ'},
                    inputData: {text: 'request'}
                },
                {
                    block: 'm-suggest-item',
                    mods: {
                        type: 'b2b-people',
                        size: 'm',
                        link: 'yes'
                    },
                    data: {
                        title: 'Группа разработки программ для ЭВМ',
                        fields: [
                            {type: 'login', value: 'login'},
                            {type: 'department_name', value: 'Все сотрудники'},
                            {type: 'is_dismissed', value: false}
                    ]},
                    inputData: {text: 'request'}
                },
                {
                    block: 'm-suggest-item',
                    mods: {
                        type: 'b2b-groups',
                        size: 'm',
                        link: 'yes'
                    },
                    data: {title: 'Еноты'},
                    inputData: {text: 'request'}
                },
                {
                    block: 'm-suggest-item',
                    mods: {
                        type: 'room',
                        size: 'm',
                        link: 'yes'
                    },
                    data: {
                        office_name: 'Москва, БЦ Морозов',
                        name: '1.Кофе',
                        type_display: 'переговорка'
                    },
                    inputData: {text: 'request'}
                },
                {
                    block: 'm-suggest-item',
                    mods: {
                        type: 'maillist',
                        size: 'm',
                        link: 'yes'
                    },
                    data: {
                        info: '',
                        is_open: true,
                        name: 'learn',
                        email: 'learn@yandex-team.ru'
                    },
                    inputData: {text: 'request'}
                },
                {
                    block: 'm-suggest-item',
                    mods: {
                        type: 'equipment',
                        size: 'm',
                        link: 'yes'
                    },
                    data: {
                        name: 'red1-21w3',
                        office_name: 'Москва, БЦ Морозов',
                        floor_display: '2 floor',
                        type_display: 'WiFi'
                    },
                    inputData: {text: 'request'}
                },
                {
                    block: 'm-suggest-item',
                    mods: {
                        type: 'simple',
                        size: 'm',
                        link: 'yes'
                    },
                    data: {text: 'Текст запроса', url: '#url'},
                    inputData: {text: 'request'}
                },
                {
                    block: 'm-suggest-item',
                    mods: {
                        type: 'staff',
                        size: 'm',
                        link: 'yes'
                    },
                    data: {
                        department: 'Служба общих компонентов интерфейсов',
                        phone: 6660,
                        login: 'kuznecov',
                        href: '//staff.yandex-team.ru/kuznecov',
                        title: 'Денис Кузнецов'
                    },
                    inputData: {text: 'request'}
                },
                {
                    block: 'm-suggest-item',
                    mods: {
                        type: 'table',
                        size: 'm',
                        link: 'yes'
                    },
                    data: {
                        office_name: 'Москва, БЦ Морозов',
                        status_display: 'Михаил Фадеев',
                        name: 'table 1111',
                        floor_display: '5 floor',
                        id: 1469
                    },
                    inputData: {text: 'request'}
                },
                {
                    block: 'm-suggest-item',
                    mods: {
                        type: 'nav',
                        size: 'm',
                        link: 'yes'
                    },
                    data: {
                        service_name: 'СЕРП',
                        title: '',
                        href: 'https://yandex.ru'
                    },
                    inputData: {text: 'Ы'}
                },
                {
                    block: 'm-suggest-item',
                    mods: {
                        type: 'nav',
                        size: 'm',
                        link: 'yes'
                    },
                    data: {
                        service_name: 'СЕРП',
                        title: 'SERP',
                        href: 'https://yandex.ru',
                        abc: true
                    },
                    inputData: {text: 'Ы'}
                }
            ]
        }
    ]
});
