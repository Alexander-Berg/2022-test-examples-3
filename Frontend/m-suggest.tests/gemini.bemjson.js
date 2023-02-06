({
    block: 'x-page',
    mods: {nopad: 'yes'},
    title: 'm-suggest',
    content: [
        // В составе m-head
        {
            block: 'gemini',
            mods: {type: 'head'},
            content: {
                block: 'm-head',
                mods: {type: 'staff'},
                service: 'staff',
                title: 'Стаф',
                search: {
                    hint: 'поиск по Марсу',
                    action: '//link.to.search'
                }
            }
        },
        // b2b
        {
            block: 'gemini',
            mods: {type: 'b2b'},
            content: {
                    block: 'm-suggest',
                    mods: {type: 'b2b', size: 'm'},
                    inputMods: {clear: 'no'},
                    value: 'Lego',
                    found: '- найден 1 ответ'
            }
        },
        // multicomplete
        {
            block: 'gemini',
            mods: {type: 'multicomplete'},
            content: {
                block: 'm-suggest',
                mods: {type: 'multicomplete', size: 's'},
                value: 'Lego',
                found: '- найден 1 ответ'
            }
        },
        // intranet
        {
            block: 'gemini',
            mods: {type: 'intranet-suggest'},
            content: {
                block: 'm-suggest',
                mods: {type: 'intranet-suggest'},
                types: [{
                    name: 'staff',
                    maxCount: 2
                }]
            }
        },
        // simple
        {
            block: 'gemini',
            mods: {type: 'simple'},
            content: {
                block: 'm-suggest',
                mods: {type: 'simple'},
                types: [{
                    name: 'staff',
                    maxCount: 2
                }]
            }
        },
        // disabled
        {
            block: 'gemini',
            mods: {type: 'disabled'},
            content: {
                block: 'm-suggest',
                mods: {type: 'simple', disabled: 'yes'},
                types: [{
                    name: 'staff',
                    maxCount: 2
                }]
            }
        }
    ]
});
