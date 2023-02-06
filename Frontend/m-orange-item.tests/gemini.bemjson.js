({
    block: 'x-page',
    title: 'm-orange-item',
    content: {
        block: 'gemini',
        content: [
            {
                block: 'm-oranges',
                content: [
                    {
                        block: 'm-orange-item',
                        mods: {old: 'no', status: 'unread'},
                        content: [
                            getTitle(),
                            {
                                elem: 'body'
                            }
                        ]
                    },
                    {
                        block: 'm-orange-item',
                        mods: {old: 'yes'},
                        content: [
                            getTitle(),
                            {
                                elem: 'body'
                            }
                        ]
                    },
                    {
                        block: 'm-orange-item',
                        mods: {old: 'no'},
                        content: [
                            getTitle(),
                            {
                                elem: 'body',
                                content: {
                                    block: 'm-orange-actions',
                                    mix: {block: 'm-orange-item', elem: 'actions'},
                                    answer: 1,
                                    comment: true,
                                    userComment: 'Что вы об этом думаете?'
                                }
                            }
                        ]
                    },
                    {
                        block: 'm-orange-item',
                        mods: {old: 'no'},
                        content: [
                            getTitle(),
                            {
                                elem: 'body',
                                content: {
                                    block: 'm-orange-actions',
                                    answer: 2
                                }
                            }
                        ]
                    },
                    {
                        block: 'm-orange-item',
                        mods: {old: 'no'},
                        content: [
                            getTitle(),
                            {
                                elem: 'body',
                                content: {
                                    block: 'm-orange-actions',
                                    answer: 3
                                }
                            }
                        ]
                    },
                    ['error', 'warning'].map(function(type) {
                        return {
                                block: 'm-orange-item',
                                mods: {old: 'yes'},
                                content: [{
                                            elem: 'error',
                                            elemMods: {type: type},
                                            content: 'Так должна выглядеть ошибка типа ' + type
                                        }]
                            };
                    })
                ]
            }
        ]
    }
});

function getTitle() {
    return {
            elem: 'title',
            content: [
                {
                    block: 'm-username',
                    content: 'Имя Фамилия'
                },
                {

                    block: 'link',
                    mods: {theme: 'normal'},
                    content: ' призывает вас в комментарии ISL-0000'
                },
                {
                    elem: 'mark',
                    content: '×'
                }
            ]
        };
}
