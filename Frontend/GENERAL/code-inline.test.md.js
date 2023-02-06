module.exports = [
    {
        group: 'inlineCode (``)',
        tests: [
            {
                markup: '``',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 2,
                            column: 3,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 2,
                                    column: 3,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'inlineCode',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 2,
                                            column: 3,
                                            line: 1,
                                        },
                                    },
                                    value: '',
                                },
                            ],
                        },
                    ],
                },
            },
            {
                markup: '<[``]>',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 6,
                            column: 7,
                            line: 1,
                        },
                    },
                    children: [
                        {
                            type: 'paragraph',
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 6,
                                    column: 7,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'womBlockquote',
                                    inline: true,
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                    },
                                    children: [
                                        {
                                            type: 'paragraph',
                                            position: {
                                                start: {
                                                    offset: 2,
                                                    column: 3,
                                                    line: 1,
                                                },
                                                end: {
                                                    offset: 4,
                                                    column: 5,
                                                    line: 1,
                                                },
                                            },
                                            children: [
                                                {
                                                    type: 'inlineCode',
                                                    position: {
                                                        start: {
                                                            offset: 2,
                                                            column: 3,
                                                            line: 1,
                                                        },
                                                        end: {
                                                            offset: 4,
                                                            column: 5,
                                                            line: 1,
                                                        },
                                                    },
                                                    value: '',
                                                },
                                            ],
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
