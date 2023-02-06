const getMinDurationSegment = require.requireActual(
    '../getMinDurationSegment',
).default;

const segments = [{duration: 3000}, {duration: 2000}];

describe('getMinDurationSegment', () => {
    it('Вернет сегмент с наименьшим временем в пути', () => {
        expect(getMinDurationSegment(segments)).toEqual({
            ...segments[1],
        });
    });

    it('Для пустого массива вернет undefined', () => {
        expect(getMinDurationSegment([])).toBeUndefined();
    });

    it('Для сегмента с неизвестным временем в пути вернет undefined', () => {
        expect(getMinDurationSegment([{duration: undefined}])).toBeUndefined();
    });
});
