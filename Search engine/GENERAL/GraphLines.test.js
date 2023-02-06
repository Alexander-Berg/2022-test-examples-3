import {GraphLine} from './GraphLines';

describe('getMissingPointVal:', () => {
    const graphLine = new GraphLine();
    let basicPoints;

    beforeEach(() => {
        basicPoints = [
            {
                date: '2019-01-11T06:00:00.000+03:00',
                value: 1,
            },
            {
                date: '2019-01-12T06:00:00.000+03:00',
                value: 2,
            },
            {
                date: '2019-01-13T06:00:00.000+03:00',
                value: 3,
            },
        ];
    });

    test('comment before min date', () => {
        const date = '2019-01-10T06:00:00.000+03:00';
        const val = 1;
        expect(graphLine.getMissingPointVal(date, basicPoints)).toEqual(val);
    });

    test('comment equal min date', () => {
        const date = '2019-01-11T06:00:00.000+03:00';
        const val = 1;
        expect(graphLine.getMissingPointVal(date, basicPoints)).toEqual(val);
    });

    test('comment equal date', () => {
        const date = '2019-01-12T06:00:00.000+03:00';
        const val = 2;
        expect(graphLine.getMissingPointVal(date, basicPoints)).toEqual(val);
    });

    test('comment between dates', () => {
        const date = '2019-01-11T14:00:00.000+03:00';
        const points = [
            {
                date: '2019-01-11T06:00:00.000+03:00',
                value: 0,
            },
            {
                date: '2019-01-12T06:00:00.000+03:00',
                value: 300,
            },
            {
                date: '2019-01-13T06:00:00.000+03:00',
                value: 150,
            },
        ];
        const val = 100;
        expect(graphLine.getMissingPointVal(date, points)).toEqual(val);
    });

    test('comment equal max date', () => {
        const date = '2019-01-13T06:00:00.000+03:00';
        const val = 3;
        expect(graphLine.getMissingPointVal(date, basicPoints)).toEqual(val);
    });

    test('getMissingPointVal - comment after max date', () => {
        const date = '2019-01-15T06:00:00.000+03:00';
        const val = 3;
        expect(graphLine.getMissingPointVal(date, basicPoints)).toEqual(val);
    });
});
