module.exports = [
    {
        group: 'html',
        tests: [
            {
                markup: '<span>test</span>',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 17,
                            column: 18,
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
                                    offset: 17,
                                    column: 18,
                                    line: 1,
                                },
                            },
                            children: [
                                {
                                    type: 'html',
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
                                    value: '<span>',
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 6,
                                            column: 7,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                    },
                                    value: 'test',
                                },
                                {
                                    type: 'html',
                                    position: {
                                        start: {
                                            offset: 10,
                                            column: 11,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 17,
                                            column: 18,
                                            line: 1,
                                        },
                                    },
                                    value: '</span>',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
];
