({
    block: 'x-page',
    title: 'm-speller',
    content: [
        {
            block: 'gemini',
            js: true,
            content: [
                {
                    block: 'textarea',
                    mods: {theme: 'normal', size: 's', role: 'text'},
                    attrs: {style: 'width: 370px; padding: 10px;'},
                    text: 'отприоритизировать'
                },
                {
                    block: 'button2',
                    mods: {theme: 'normal', size: 'm', role: 'check'},
                    attrs: {style: 'margin: 10px;'},
                    text: 'Проверить правописание'
                },
                {
                    block: 'm-speller',
                    js: true
                }
            ]
        }
    ]
});
