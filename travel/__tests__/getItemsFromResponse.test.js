const getItemsFromResponse = require.requireActual(
    '../getItemsFromResponse',
).default;

describe('getItemsFromResponse', () => {
    it('should cast suggest response to sane format', () => {
        const response = [
            '',
            [
                ['c54', 'Екатеринбург', 'г. Екатеринбург'],
                ['c213', 'Москва', 'г. Москва'],
            ],
        ];

        const expectedResult = [
            {
                value: {
                    key: 'c54',
                    title: 'Екатеринбург',
                },
                text: 'г. Екатеринбург',
            },
            {
                value: {
                    key: 'c213',
                    title: 'Москва',
                },
                text: 'г. Москва',
            },
        ];

        expect(getItemsFromResponse(response)).toEqual(expectedResult);
    });
});
