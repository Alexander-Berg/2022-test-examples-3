[{
    mustDeps: [
        {block: 'i-bem', elem: ['dom']},
        {
            block: 'button2',
            mods: {
                size: ['xs', 's', 'm', 'l', 'n'],
                theme: ['normal', 'clear', 'action'],
                view: ['classic', 'default'],
                tone: ['default', 'red', 'grey', 'dark']
            }
        },
        {
            block: 'tooltip',
            mods: {
                size: ['xs', 's', 'm', 'l', 'n'],
                theme: ['success', 'normal', 'error', 'promo'],
                view: 'default',
                tone: ['default', 'red', 'grey', 'dark']
            }
        },
        {
            block: 'tooltip',
            elem: 'corner',
            mods: {
                star: 'yes'
            }
        },
        {
            block: 'tooltip',
            elem: ['description', 'buttons']
        }
    ]
}];
