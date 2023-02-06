module.exports = [
    {
        group: 'Empty',
        tests: [
            {
                markup: '',
                title: 'Foo',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                    },
                    children: [
                    ],
                },
            },
            {
                markup: '',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                    },
                    children: [
                    ],
                },
            },
            {
                markup: '',
                title: 'Another one',
                expect: {
                    type: 'root',
                    position: {
                        start: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                        end: {
                            offset: 0,
                            column: 1,
                            line: 1,
                        },
                    },
                    children: [
                    ],
                },
            },
        ],
    },
];
