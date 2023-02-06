import diffIndexBy from './diffIndexBy';

describe('Get diff from list with duplicates', () => {
    test('Single item', () => {
        const initialVals = [
            {id: 1, name: 'bob'},
            {id: 1, name: 'jil'},
            {id: 2, name: 'chris'},
            {id: 3, name: 'pete'},
        ];
        const leftVals = [1, 2, 3];

        expect(
            diffIndexBy(initialVals, leftVals, (m, val) => m.id !== val),
        ).toEqual([{id: 1, name: 'jil'}]);
    });

    test('Single item with complex id', () => {
        const initialVals = [
            {id: '1-bob', name: 'bob'},
            {id: '1-jil', name: 'jil'},
            {id: '2-chris', name: 'chris'},
            {id: '3-pete', name: 'pete'},
        ];
        const leftVals = ['1-jil', '2-chris', '3-pete'];

        expect(
            diffIndexBy(initialVals, leftVals, (m, val) => m.id !== val),
        ).toEqual([{id: '1-bob', name: 'bob'}]);
    });

    test('Multiple items', () => {
        const initialVals = [
            {id: 1, name: 'bob'},
            {id: 1, name: 'jil'},
            {id: 2, name: 'chris'},
            {id: 3, name: 'pete'},
            {id: 3, name: 'mary'},
        ];
        const leftVals = [1, 3, 3];

        expect(
            diffIndexBy(initialVals, leftVals, (m, val) => m.id !== val),
        ).toEqual([
            {id: 1, name: 'jil'},
            {id: 2, name: 'chris'},
        ]);
    });
});
