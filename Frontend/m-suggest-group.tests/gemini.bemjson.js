({
    block: 'x-page',
    title: 'm-suggest-group',
    content: [
        {
            block: 'gemini',
            attrs: {id: 'simple-group'},
            content: [
                {
                    block: 'm-suggest-group',
                    content: [
                        {
                            block: 'm-suggest-item',
                            mods: {
                                type: 'staff',
                                size: 's'
                            },
                            data: {
                                department: 'Служба общих компонентов',
                                phone: '0000',
                                login: 'kuznecov',
                                href: '//staff.yandex-team.ru/kuznecov',
                                title: 'Денис Кузнецов'
                            },
                            inputData: {text: 'request'}
                        },
                        {
                            block: 'm-suggest-item',
                            mods: {
                                type: 'staff',
                                link: 'yes',
                                size: 's'
                            },
                            data: {
                                department: 'Служба общих компонентов',
                                phone: '0001',
                                login: 'persidskiy',
                                href: '//staff.yandex-team.ru/persidskiy',
                                title: 'Иван Персидский'
                            },
                            inputData: {text: 'request'}
                        }
                    ]
                }
            ]
        },
        {
            block: 'gemini',
            attrs: {id: 'b2b-group'},
            content: [
                {
                    block: 'm-suggest-group',
                    mods: {'is-b2b': 'yes'},
                    groupLabel: 'group',
                    content: [
                        {
                            block: 'm-suggest-item',
                            mods: {type: 'b2b-departments', size: 'm', link: 'yes'},
                            data: {title: 'Группа разработки программ для ЭВМ'},
                            inputData: {text: 'request'}
                        },
                        {
                            block: 'm-suggest-item',
                            mods: {type: 'b2b-groups', size: 'm', link: 'yes'},
                            data: {
                                fields: [],
                                title: 'Какая-то группа лиц, возможно, дружелюбных, но как знать'
                            },
                            inputData: {text: 'request'}
                        }
                    ]
                }
            ]
        }
    ]
});
