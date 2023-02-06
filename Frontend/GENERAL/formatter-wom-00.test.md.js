module.exports = [
    {
        group: 'womFormatter %%',
        tests: [
            {
                markup: '1. %%(js)\n   js\n   %%',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 21,
                            column: 6,
                            line: 3,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 21,
                                    column: 6,
                                    line: 3,
                                },
                            },
                            styleType: 'decimal',
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 21,
                                            column: 6,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'womFormatter',
                                            attributes: {
                                            },
                                            format: 'js',
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 21,
                                                    column: 6,
                                                    line: 3,
                                                },
                                            },
                                            value: 'js\n',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '1. %%(js)\n   js\n   %%\n1. %%(js)\n   js\n   %%',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 43,
                            column: 6,
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 43,
                                    column: 6,
                                    line: 6,
                                },
                            },
                            styleType: 'decimal',
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 21,
                                            column: 6,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'womFormatter',
                                            attributes: {
                                            },
                                            format: 'js',
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 3,
                                                    column: 4,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 21,
                                                    column: 6,
                                                    line: 3,
                                                },
                                            },
                                            value: 'js\n',
                                        },
                                    ],
                                },
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 22,
                                            column: 1,
                                            line: 4,
                                        },
                                        end: {
                                            offset: 43,
                                            column: 6,
                                            line: 6,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'womFormatter',
                                            attributes: {
                                            },
                                            format: 'js',
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 25,
                                                    column: 4,
                                                    line: 4,
                                                },
                                                end: {
                                                    offset: 43,
                                                    column: 6,
                                                    line: 6,
                                                },
                                            },
                                            value: 'js\n',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '   1. %%(js)\n      js\n      %%',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 30,
                            column: 9,
                            line: 3,
                        },
                    },
                    children: [
                        {
                            type: 'list',
                            start: 1,
                            loose: false,
                            ordered: true,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 30,
                                    column: 9,
                                    line: 3,
                                },
                            },
                            styleType: 'decimal',
                            children: [
                                {
                                    type: 'listItem',
                                    checked: null,
                                    expandable: false,
                                    loose: false,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 30,
                                            column: 9,
                                            line: 3,
                                        },
                                    },
                                    restart: null,
                                    children: [
                                        {
                                            type: 'womFormatter',
                                            attributes: {
                                            },
                                            format: 'js',
                                            inline: false,
                                            position: {
                                                start: {
                                                    offset: 6,
                                                    column: 7,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 30,
                                                    column: 9,
                                                    line: 3,
                                                },
                                            },
                                            value: 'js\n',
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
];
