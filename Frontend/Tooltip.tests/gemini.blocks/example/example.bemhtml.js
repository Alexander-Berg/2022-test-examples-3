block('example').match(function() {
    return this.mods.size;
})(
    js()(true),

    content()(function() {
        return [
            {
                block: 'button2',
                mods: {
                    size: this.mods.size,
                    theme: 'normal',
                    view: this.mods.view,
                    tone: this.mods.tone
                },
                text: 'owner'
            },
            {
                block: 'tooltip',
                mods: {
                    size: this.mods.size,
                    theme: this.mods.theme,
                    testcase: this.mods.testcase,
                    view: this.mods.view,
                    tone: this.mods.tone
                },
                content: 'Tooltip'
            }
        ];
    })
);

block('example').match(function() {
    return this.mods.multiline !== undefined;
})(
    js()(true),

    content()(function() {
        return [
            {
                block: 'button2',
                mods: {
                    size: this.mods.size,
                    theme: 'normal'
                },
                text: 'owner'
            },
            {
                block: 'tooltip',
                mods: {
                    size: this.mods.size,
                    theme: this.mods.theme,
                    testcase: this.mods.testcase
                },
                content: 'This is the tooltip<br>multiline example'
            }
        ];
    })
);

block('example').match(function() {
    return this.mods.tail !== undefined;
})(
    js()(true),

    content()(function() {
        return [
            {
                block: 'button2',
                mods: {
                    size: 'm',
                    theme: 'normal'
                },
                text: 'owner'
            },
            {
                block: 'tooltip',
                mods: {
                    size: 'm',
                    theme: 'normal'
                },
                tail: false,
                js: {offset: 5},
                content: 'Tooltip'
            }
        ];
    })
);

block('example').match(function() {
    return this.mods.theme === 'promo' && this.mods.size === 'm';
})(
    js()(true),

    content()(function() {
        return [
            {
                block: 'button2',
                mods: {
                    size: this.mods.size,
                    theme: 'normal',
                    view: this.mods.view,
                    tone: this.mods.tone
                },
                text: 'owner'
            },
            {
                block: 'tooltip',
                mods: {
                    size: this.mods.size,
                    theme: this.mods.theme,
                    testcase: this.mods.testcase,
                    view: this.mods.view,
                    tone: this.mods.tone
                },
                js: {
                    to: ['right-bottom']
                },
                content: [
                    {elem: 'description', content: 'Добавляйте понравившиеся ролики в закладки'},
                    {
                        elem: 'buttons',
                        content: [
                            {
                                block: 'button2',
                                mods: {view: 'classic', tone: 'default', size: 'm', theme: 'clear'},
                                text: 'Не хочу'
                            },
                            {
                                block: 'button2',
                                mods: {view: 'classic', tone: 'default', size: 'm', theme: 'action'},
                                text: 'Добавить'
                            }
                        ]
                    }
                ]
            }
        ];
    })
);

block('example').match(function() {
    return this.mods.theme === 'promo' && this.mods.size === 's';
})(
    js()(true),

    content()(function() {
        return [
            {
                block: 'button2',
                mods: {
                    size: this.mods.size,
                    theme: 'normal',
                    view: this.mods.view,
                    tone: this.mods.tone
                },
                text: 'owner'
            },
            {
                block: 'tooltip',
                mods: {
                    size: this.mods.size,
                    theme: this.mods.theme,
                    testcase: this.mods.testcase,
                    view: this.mods.view,
                    tone: this.mods.tone
                },
                js: {
                    to: ['right-bottom']
                },
                content: [
                    {elem: 'description', content: 'Добавьте СМИ в избранное'},
                    {elem: 'corner', elemMods: {star: 'yes'}}
                ]
            }
        ];
    })
);
