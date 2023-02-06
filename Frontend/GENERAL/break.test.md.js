module.exports = [
    {
        group: 'break',
        tests: [
            {
                markup: 'text  \ntext',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 11,
                            column: 5,
                            line: 2,
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
                                    offset: 11,
                                    column: 5,
                                    line: 2,
                                },
                            },
                            children: [
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 0,
                                            column: 1,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 4,
                                            column: 5,
                                            line: 1,
                                        },
                                    },
                                    value: 'text',
                                },
                                {
                                    type: 'break',
                                    position: {
                                        start: {
                                            offset: 4,
                                            column: 5,
                                            line: 1,
                                        },
                                        end: {
                                            offset: 7,
                                            column: 1,
                                            line: 2,
                                        },
                                    },
                                },
                                {
                                    type: 'text',
                                    position: {
                                        start: {
                                            offset: 7,
                                            column: 1,
                                            line: 2,
                                        },
                                        end: {
                                            offset: 11,
                                            column: 5,
                                            line: 2,
                                        },
                                    },
                                    value: 'text',
                                },
                            ],
                        },
                    ],
                },
            },
        ],
    },
];
