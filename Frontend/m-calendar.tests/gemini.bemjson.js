({
    block: 'b-page',
    title: 'm-calendar',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'test',
            content: {
                block: 'm-calendar',
                mix: {block: 'test-input-month'},
                mods: {toggler: 'input', type: 'month'},
                js: {
                    defaultText: 'выбери дату',
                    date: '2012-09',
                    specialDays: {
                        '2012-10-02': {background: '#f00', title: 'вау, выходной!', color: '#fff'},
                        '2012-10-05': {background: '#060', title: 'фак!'},
                        '2012-11-10': {background: '#060', title: 'йоу'}
                    }
                },
                size: 's',
                theme: 'grey'
            }
        },
        {
            block: 'test',
            content: {
                block: 'm-calendar',
                mix: {block: 'test-button-month'},
                mods: {toggler: 'button', type: 'month'},
                js: {
                    defaultText: 'выбери дату',
                    date: '2012-09',
                    specialDays: {
                        '2012-10-02': {background: '#f00', title: 'вау, выходной!', color: '#fff'},
                        '2012-10-05': {background: '#060', title: 'фак!'},
                        '2012-11-10': {background: '#060', title: 'йоу'}
                    }
                },
                size: 's',
                theme: 'grey'
            }
        },
        {
            block: 'test',
            content: {
                block: 'm-calendar',
                mix: {block: 'test-input-scope'},
                mods: {toggler: 'input', type: 'scope'},
                js: {
                    scope: {
                        from: '2012-09-13',
                        to: '2013-02-07'
                    },
                    defaultText: 'выбери дату'
                },
                size: 's',
                theme: 'grey'

            }
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
