({
    block: 'b-page',
    title: 'mi-notifier',
    head: [{
        elem: 'css',
        url: '_gemini.css',
        ie: false
    }],
    content: [
        {
            block: 'instant-notifier',
            js: {
                messages: ['Сообщение, уехавшее вниз.', 'Последнее, верхнее сообщение.'],
                options: {
                            icons: true,
                            autoclosable: false,
                            delay: null,
                            position: 'left-top'
                        }
            }
        },
        {
            elem: 'cc',
            condition: 'gt IE 8',
            others: true,
            content: {
                block: 'i-jquery',
                mods: {
                    version: 'default'
                }
            }
        },
        {
            elem: 'js',
            url: '_gemini.js'
        }
    ]
});
