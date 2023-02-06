({
    block: 'x-page',
    title: 'm-datepicker',
    content: [
        {
            block: 'gemini',
            js: true,
            attrs: {id: 'month'},
            content: {
                block: 'm-datepicker',
                mods: {type: 'month', has: 'clear'},
                js: {
                    value: '2012-10-21',
                    specialDays: {
                        '2012-10-02': {background: '#f00', title: 'красный фон', color: '#fff'},
                        '2012-10-05': {background: '#060', title: 'зеленый фон'}
                    }
                }
            }
        },
        {
            block: 'gemini',
            js: true,
            attrs: {id: 'month-limits'},
            content: {
                block: 'm-datepicker',
                mods: {type: 'month'},
                js: {
                    value: '2012-10-21',
                    specialDays: {
                        '2012-10-02': {background: '#f00', title: 'красный фон', color: '#fff'},
                        '2012-10-05': {background: '#060', title: 'зеленый фон'}
                    },
                    limits: {
                        min: '2012-10-04',
                        max: '2012-10-20'
                    }
                }
            }
        },
        {tag: 'br'},
        {
            block: 'gemini',
            js: true,
            attrs: {id: 'scope'},
            content: {
                block: 'm-datepicker',
                mods: {type: 'scope', has: 'clear'},
                js: {
                    scope: {
                        from: '2012-10-11',
                        to: '2013-02-07'
                    }
                }
            }
        },
        {
            block: 'gemini',
            js: true,
            attrs: {id: 'scope-disabled'},
            content: {
                block: 'm-datepicker',
                mods: {type: 'scope', disable: 'change', has: 'clear'},
                js: {
                    scope: {
                        from: '2012-10-11',
                        to: '2013-02-07'
                    }
                }
            }
        }
    ]
});
