({
        block: 'b-page',
        title: 'm-editor-button',
        head: [{
            elem: 'css',
            url: '_gemini.css',
            ie: false
        }],
        content: [{
            block: 'gemini',
            content: [(function() {
            var types = ['h2', 'h3', 'bold', 'italic',
                         'underline', 'strikethrough', 'ul', 'ol',
                         'indent', 'outdent', 'quote', 'readmore', 'separator',
                         'link', 'picture', 'code', 'table', 'color', 'math', 'spell',
                         'other', 'help', 'user', 'file', 'drafts'];

            return types.map(function(type) {
                return {
                    block: 'gemini-button',
                    content: [{
                        block: 'm-editor-button',
                        data: {drafts: {length: 1}},
                        mods: {type: type}
                    }]
                };
            });
        })(),
        {
            block: 'gemini-button',
            content: {
                block: 'm-editor-button',
                data: {drafts: {length: 1}},
                mods: {virtual: 'yes'}
            }
        }]
        },
        {
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
