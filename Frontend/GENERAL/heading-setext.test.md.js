module.exports = [
    {
        group: 'Setext Heading\n-----',
        tests: [
            {
                markup: '{{iframe}}\n------',
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
                            column: 7,
                            line: 2,
                        },
                    },
                    children: [
                        {
                            type: 'womAction',
                            inline: false,
                            name: 'iframe',
                            params: {
                            },
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 10,
                                    column: 11,
                                    line: 1,
                                },
                            },
                        },
                        {
                            type: 'thematicBreak',
                            position: {
                                start: {
                                    offset: 11,
                                    column: 1,
                                    line: 2,
                                },
                                end: {
                                    offset: 17,
                                    column: 7,
                                    line: 2,
                                },
                            },
                        },
                    ],
                },
            },
        ],
    },
];
