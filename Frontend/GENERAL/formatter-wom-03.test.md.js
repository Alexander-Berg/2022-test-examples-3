module.exports = [
    {
        group: 'womFormatter %%%%',
        tests: [
            {
                markup: '%%\nsome text\n%\nand new text %\n%%\n',
                title: '% внутри форматтеров',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 33,
                            column: 1,
                            line: 6,
                        },
                    },
                    children: [
                        {
                            type: 'womFormatter',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 32,
                                    column: 3,
                                    line: 5,
                                },
                            },
                            value: 'some text\n%\nand new text %\n',
                        },
                    ],
                },
            },
            {
                markup: '%%\n\\|\n%%\n',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 9,
                            column: 1,
                            line: 4,
                        },
                    },
                    children: [
                        {
                            type: 'womFormatter',
                            inline: false,
                            position: {
                                start: {
                                    offset: 0,
                                    column: 1,
                                    line: 1,
                                },
                                end: {
                                    offset: 8,
                                    column: 3,
                                    line: 3,
                                },
                            },
                            value: '\\|\n',
                        },
                    ],
                },
            },
        ],
    },
];
