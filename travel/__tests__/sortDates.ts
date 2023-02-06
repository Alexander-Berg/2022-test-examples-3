import sortDates from 'utilities/dateUtils/sortDates';

describe('sortDates()', () => {
    test('Если даты в нужном порядке, рендж должен остаться старым', () => {
        const range = [new Date('2021-02-02'), new Date('2021-03-03')];

        const sortedRange = sortDates(...range);

        expect(sortedRange).toEqual(range);
    });

    test('Если даты в обратном порядке, рендж должен отсортироваться', () => {
        const range = [new Date('2021-03-03'), new Date('2021-02-02')];

        const sortedRange = sortDates(...range);

        expect(sortedRange).toEqual(range.reverse());
    });
});
