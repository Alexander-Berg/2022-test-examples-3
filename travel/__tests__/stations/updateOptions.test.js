jest.dontMock('../../baseFilterManager');

const stationFrom = require.requireActual('../../stationFrom').default;
const stationTo = require.requireActual('../../stationTo').default;

const segment = {
    stationFrom: {
        id: 1,
        title: 'Станция 9 и три четверти',
    },
    stationTo: {
        id: 2,
        title: 'Хогвартс',
    },
};

describe('stationFrom', () => {
    describe('updateOptions', () => {
        it('should return updated options', () => {
            const options = [];
            const result = stationFrom.updateOptions(options, segment);

            expect(result).toEqual([
                {
                    value: '1',
                    text: 'Станция 9 и три четверти',
                },
            ]);
        });

        it('should return options without changes', () => {
            const options = [
                {
                    value: '1',
                    text: 'Станция 9 и три четверти',
                },
            ];
            const result = stationFrom.updateOptions(options, segment);

            expect(result).toEqual(options);
        });
    });
});

describe('stationTo', () => {
    describe('updateOptions', () => {
        it('should return updated options', () => {
            const options = [];
            const result = stationTo.updateOptions(options, segment);

            expect(result).toEqual([
                {
                    value: '2',
                    text: 'Хогвартс',
                },
            ]);
        });

        it('should return options without changes', () => {
            const options = [
                {
                    value: '2',
                    text: 'Хогвартс',
                },
            ];
            const result = stationTo.updateOptions(options, segment);

            expect(result).toEqual(options);
        });
    });
});
