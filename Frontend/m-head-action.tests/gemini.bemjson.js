({
    block: 'x-page',
    content: [
        {
            block: 'gemini',
            attrs: {id: 'basic'},
            content: [{
                block: 'm-head-action',
                action: 'Go ahead!'
            }]
        },
        {
            block: 'gemini',
            attrs: {id: 'with-popup'},
            content: [{
                block: 'm-head-action',
                mods: {'with': 'popup'},
                action: {
                    content: 'Free ware',
                    items: [
                        {name: 'Yes', url: 'http://ya.ru'}
                    ]
                }
            }]
        }
    ]
});

