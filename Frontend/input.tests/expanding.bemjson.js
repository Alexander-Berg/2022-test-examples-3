[{
    block: 'x-page',
    title: 'input',
    content: [
        {
            block: 'input-wrap',
            content: [
                {
                    block: 'input',
                    mix: {block: 'gemini-expanding'},
                    mods: {
                        theme: 'touchsearch',
                        size: 'm',
                        expanding: 'yes',
                        type: 'textarea',
                        lines: 'single',
                        'line-min': 1,
                        'line-max': 4
                    },
                    js: {
                        move: false
                    },
                    content: {elem: 'control'}
                }
            ]
        }
    ]
}];
