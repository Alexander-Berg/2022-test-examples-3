jest.setMock(
    '../tariffs/patchTariffs',
    jest.fn((segment, tariff) => tariff),
);

const addTariffsToSegments = require.requireActual(
    '../addTariffsToSegments',
).default;

describe('addTariffsToSegments', () => {
    it('should add tariffs to segments', () => {
        const segments = [
            {
                tariffsKeys: ['1', '2'],
            },
            {
                tariffsKeys: ['3', '4'],
            },
        ];

        const tariffs = [
            {
                key: '1',
                classes: {economy: 'foo'},
            },
            {
                key: '5',
                classes: {bus: 'bar'},
            },
        ];

        const meta = {};

        expect(addTariffsToSegments(segments, tariffs, meta)).toEqual([
            {
                tariffsKeys: ['1', '2'],
                tariffs: {
                    key: '1',
                    classes: {economy: 'foo'},
                },
            },
            {
                tariffsKeys: ['3', '4'],
            },
        ]);
    });
});
