({
    block: 'x-page',
    title: 'm-date',
    content: [
        {
            block: 'gemini',
            mods: {'date-buttons': true},
            content: {
                block: 'm-calendar-range',
                mods: {toggler: 'button', has: 'clear'},
                js: {
                    value: {
                        from: '2012-10-21',
                        to: '2012-10-24'
                    }
                },
                label: {from: 'с', to: 'по'},
                size: 'm',
                theme: 'grey'
            }
        },
        {
            block: 'gemini',
            mods: {'text-inputs': true},
            content: {
                block: 'm-calendar-range',
                mods: {toggler: 'input', has: 'clear'},
                js: {
                    value: {
                        from: '2012-10-21',
                        to: '2012-10-24'
                    }
                },
                label: {from: 'с', to: 'по'},
                size: 'm',
                theme: 'grey'
            }
        },
        {
            block: 'gemini',
            mods: {'text-buttons': true},
            content: {
                block: 'm-calendar-range',
                mods: {toggler: 'button', has: 'clear'},
                js: true,
                defaultText: {from: 'начало', to: 'конец'},
                size: 'm',
                theme: 'grey'
            }
        },
        {
            block: 'gemini',
            mods: {inputs: true},
            content: {
                block: 'm-calendar-range',
                mods: {toggler: 'input', has: 'clear'},
                js: true,
                defaultText: {from: 'начало', to: 'конец'},
                size: 'm',
                theme: 'grey'
            }
        }
    ]
});
