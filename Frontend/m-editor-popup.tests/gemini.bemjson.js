({
    block: 'x-page',
    title: 'm-editor-popup',
    content: [
        {
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
                        ['link', 'picture', 'code', 'user', 'file', 'spell'],
                        ['ol', 'ul'],
                        ['bold', 'italic', 'underline', 'strikethrough'],
                        ['indent', 'outdent'],
                        ['h2', 'h3']
                    ]
                },
                value: 'Текст тут'
            }]
        },
        {
            block: 'gemini',
            cls: 'drafts',
            content: {
                block: 'm-editor-popup',
                mods: {type: 'drafts', editor: 'yes'},
                data: {
                    drafts: [
                        {val: 'Once upon a time...', date: 1477406609279, isSaved: false},
                        {val: '...in far far away kingdom', date: 1477406609279, isSaved: false}
                    ]
                }
            }
        }
    ]
});
