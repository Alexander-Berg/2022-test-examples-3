// jscs:disable
module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        construct: {
            type: 'test',
            template: 'composite',
            gap: 'm',
            items: [
                {
                    block: 'fact',
                    description: 'подо мной отступ "gap: m"'
                },
                {
                    block: 'fact',
                    description: 'подо мной тоже отступ "gap: m"'
                },
                {
                    template: 'composite',
                    logNode: { name: 'inside' },
                    gap: 'none',
                    items: [
                        {
                            block: 'fact',
                            description: 'я во вложенном шаблоне у которого отключены отступы'
                        }, {
                            block: 'fact',
                            description: 'подо мной тоже нет отступа'
                        },
                        {
                            template: 'composite',
                            logNode: { name: 'inside' },
                            gap: 's',
                            items: [
                                {
                                    block: 'fact',
                                    description: 'я в еще одном вложенном шаблоне, у которого отступ "gap: s"'
                                }, {
                                    block: 'fact',
                                    description: 'подо мной уже нет отступа "gap: s", я последний'
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    }
};
