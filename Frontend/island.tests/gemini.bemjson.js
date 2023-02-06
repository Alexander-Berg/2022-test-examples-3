({
    block: 'b-page',
    title: 'island',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini',
            content: [
                {
                    block: 'gemini-island',
                    content: {
                        block: 'island',
                        content: 'Остров шириной 200px и высотой 100px'
                    }
                },
                {
                    block: 'gemini-island-light',
                    content: {
                        block: 'island',
                        mods: {type: 'light'},
                        content: 'Легкий остров шириной 200px и высотой 100px'
                    }
                },
                {
                    block: 'gemini-island-fly',
                    content: {
                        block: 'island',
                        mods: {type: 'fly'},
                        content: 'Парящий остров шириной 200px и высотой 100px'
                    }
                }
            ]
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {elem: 'js', url: 'https://yastatic.net/es5-shims/0.0.1/es5-shims.min.js'}
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {block: 'i-jquery', mods: {version: '1.8.3'}}
        },
        {
            elem: 'cc',
            condition: 'gt IE 8',
            others: true,
            content: {block: 'i-jquery', mods: {version: 'default'}}
        },
        {elem: 'js', url: '_gemini.js'}
    ]
});
