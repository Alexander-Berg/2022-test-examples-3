({
        block: 'b-page',
        title: 'm-editor',
        head: [{
            elem: 'css',
            url: '_gemini.css',
            ie: false
        }],
        content: [{
            block: 'gemini',
            cls: 'm-editor_type_test',
            js: true,
            content: [{
                block: 'm-editor-buttons',
                mix: [{
                    block: 'm-editor',
                    js: {
                        id: 'test'
                    }
                }],
                content: ''
            }, {
                block: 'm-editor',
                js: {
                    id: 'test',
                    uploadParams: {
                        reqParams: true
                    },
                    buttons: [
                        ['h2', 'h3'],
                        ['bold', 'italic', 'underline', 'strikethrough'],
                        ['ul', 'ol'],
                        ['indent', 'outdent'],
                        ['quote', 'separator', 'link', 'picture', 'code', 'table', 'color']
                    ]
                },
                value: 'Текст тут'
            }]
        }, {
            elem: 'cc',
            condition: 'IE 8',
            content: {
                elem: 'js',
                url: 'https://yastatic.net/es5-shims/0.0.1/es5-shims.min.js'
            }
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {
                block: 'i-jquery',
                mods: {
                    version: '1.8.3'
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
