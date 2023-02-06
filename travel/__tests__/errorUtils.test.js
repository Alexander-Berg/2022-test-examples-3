const {prepareAmbiguousErrors} = require.requireActual('../errorUtils');

describe('errorUtils', () => {
    describe('prepareAmbiguousErrors', () => {
        const errors = [
            {
                ambiguousTitle: {
                    additionalTitle: 'Хабаровский край',
                    title: 'Большие Стыды',
                },
                key: '1',
            },
            {
                ambiguousTitle: {
                    additionalTitle: 'Хабаровский край',
                    title: 'Малые Стыды',
                },
                key: '2',
            },
            {
                ambiguousTitle: {
                    additionalTitle: 'Краснодарский край',
                    title: 'Стыды',
                },
                key: '3',
            },
        ];

        it('should return converted errors array', () => {
            const result = prepareAmbiguousErrors(errors);

            expect(result).toEqual([
                {
                    ambiguousTitle: {
                        additionalTitle: 'Краснодарский край',
                        title: [
                            {
                                name: 'Стыды',
                                key: '3',
                            },
                        ],
                    },
                    key: '3',
                },
                {
                    ambiguousTitle: {
                        additionalTitle: 'Хабаровский край',
                        title: [
                            {
                                name: 'Большие Стыды',
                                key: '1',
                            },
                            {
                                name: 'Малые Стыды',
                                key: '2',
                            },
                        ],
                    },
                    key: '1',
                },
            ]);
        });

        it('should return empty array', () => {
            const result = prepareAmbiguousErrors([]);

            expect(result).toEqual([]);
        });
    });
});
