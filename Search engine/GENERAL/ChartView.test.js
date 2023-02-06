import moment from 'moment';

import {calcFormatters, filterLabels} from './ChartView';

function prepareData(data) {
    return data.map(v => moment(v));
}

describe('TicksFormatter', () => {
    function getActualForTicksFormatter(data) {
        return calcFormatters(prepareData(data));
    }

    test('ticksFormatter', () => {
        const data = [
            '2018-02-20 10:00',
            '2018-05-20 10:00',
            '2018-05-22 10:00',
            '2018-05-22 12:00',
            '2018-08-20 10:00',
            '2018-12-20 10:00',
            '2019-02-20 11:00',
        ];
        const actual = getActualForTicksFormatter(data);
        const expected = [
            ['10:00', '20 Feb 2018'],
            ['10:00', '20 May'],
            ['10:00', '22'],
            ['12:00'],
            ['10:00', '20 Aug'],
            ['10:00', '20 Dec'],
            ['11:00', '20 Feb 2019'],
        ];
        expect(actual).toEqual(expected);
    });

    test('ticksFormatter2', () => {
        const data = [
            '2018-02-20 10:00',
            '2018-03-20 10:00',
            '2018-03-22 10:00',
            '2018-05-25 10:00',
            '2018-06-20 10:00',
            '2018-10-20 10:00',
            '2018-12-20 10:00',
        ];
        const actual = getActualForTicksFormatter(data);
        const expected = [
            ['20', 'Feb 2018'],
            ['20', 'Mar'],
            ['22'],
            ['25', 'May'],
            ['20', 'Jun'],
            ['20', 'Oct'],
            ['20', 'Dec'],
        ];
        expect(actual).toEqual(expected);
    });

    test('ticksFormatter3', () => {
        const data = [
            '2018-02-02 10:00',
            '2018-02-04 10:00',
            '2018-02-05 10:00',
            '2018-02-05 12:00',
            '2018-02-16 10:00',
            '2018-02-19 10:00',
            '2018-02-20 11:00',
        ];
        const actual = getActualForTicksFormatter(data);
        const expected = [
            ['10:00', '2 Feb 2018'],
            ['10:00', '4'],
            ['10:00', '5'],
            ['12:00'],
            ['10:00', '16'],
            ['10:00', '19'],
            ['11:00', '20'],
        ];
        expect(actual).toEqual(expected);
    });

    test('ticksFormatter4', () => {
        const data = [
            '2018-02-02 10:12',
            '2018-02-02 11:00',
            '2018-02-02 12:45',
            '2018-02-02 13:33',
            '2018-02-02 14:56',
            '2018-02-02 15:00',
            '2018-02-02 16:01',
        ];
        const actual = getActualForTicksFormatter(data);
        const expected = [
            ['10:12', '2 Feb 2018'],
            ['11:00'],
            ['12:45'],
            ['13:33'],
            ['14:56'],
            ['15:00'],
            ['16:01'],
        ];
        expect(actual).toEqual(expected);
    });

    test('ticksFormatterWithRealVals EcoSystem', () => {
        const data = [
            '2018-12-12 00:00',
            '2018-12-19 00:00',
            '2019-01-14 00:00',
            '2019-01-15 00:00',
            '2019-01-31 00:00',
            '2019-02-01 00:00',
            '2019-02-02 00:00',
            '2019-02-27 00:00',
        ];
        const actual = getActualForTicksFormatter(data);
        const expected = [
            ['12', 'Dec 2018'],
            ['19'],
            ['14', 'Jan 2019'],
            ['15'],
            ['31'],
            ['1', 'Feb'],
            ['2'],
            ['27'],
        ];
        expect(actual).toEqual(expected);
    });

    test('ticksFormatterWithRealVals man.m', () => {
        const data = [
            '2019-02-24 07:00',
            '2019-02-24 07:10',
            '2019-02-24 07:20',
            '2019-02-24 07:30',
            '2019-02-24 07:40',
            '2019-02-25 07:50',
        ];
        const actual = getActualForTicksFormatter(data);
        const expected = [
            ['7:00', '24 Feb 2019'],
            ['7:10'],
            ['7:20'],
            ['7:30'],
            ['7:40'],
            ['7:50', '25'],
        ];
        expect(actual).toEqual(expected);
    });

    test('ticksFormatterWithRealVals Menu Accept', () => {
        const data = [
            '2017-03-06 09:05',
            '2017-03-07 09:05',
            '2017-07-24 09:05',
            '2017-07-25 09:05',
            '2017-12-11 09:05',
            '2018-04-20 09:05',
            '2018-04-30 09:05',
            '2018-09-17 09:05',
            '2019-02-04 09:05',
        ];
        const actual = getActualForTicksFormatter(data);
        const expected = [
            ['6', 'Mar 2017'],
            ['7'],
            ['24', 'Jul'],
            ['25'],
            ['11', 'Dec'],
            ['20', 'Apr 2018'],
            ['30'],
            ['17', 'Sep'],
            ['4', 'Feb 2019'],
        ];
        expect(actual).toEqual(expected);
    });
});

describe('filterLabels', () => {
    function getAcatualForFilterLabels(data) {
        return filterLabels(prepareData(data), 240).map(d =>
            d.format('YYYY-MM-DD HH:mm'),
        );
    }

    test('filterLabelsDays', () => {
        const data = [
            '2018-03-20 10:00',
            '2018-03-21 10:00',
            '2018-03-22 10:00',
            '2018-03-23 10:00',
            '2018-03-24 10:00',
            '2018-03-25 10:00',
            '2018-03-26 10:00',
        ];
        const actual = getAcatualForFilterLabels(data);
        const expected = [
            '2018-03-20 10:00',
            '2018-03-22 10:00',
            '2018-03-24 10:00',
            '2018-03-26 10:00',
        ];
        expect(actual).toEqual(expected);
    });

    test('filterLabelsMonths', () => {
        const data = [
            '2018-03-20 10:00',
            '2018-04-20 10:00',
            '2018-05-20 10:00',
            '2018-06-20 10:00',
            '2018-07-20 10:00',
            '2018-08-20 10:00',
            '2018-09-20 10:00',
        ];
        const actual = getAcatualForFilterLabels(data);
        const expected = [
            '2018-03-20 10:00',
            '2018-06-20 10:00',
            '2018-09-20 10:00',
        ];
        expect(actual).toEqual(expected);
    });

    test('filterLabelsWithFeb', () => {
        const data = [
            '2018-02-20 10:00',
            '2018-03-20 10:00',
            '2018-04-20 10:00',
            '2018-05-20 10:00',
            '2018-06-20 10:00',
            '2018-07-20 10:00',
            '2018-08-20 10:00',
        ];
        const actual = getAcatualForFilterLabels(data);
        const expected = [
            '2018-02-20 10:00',
            '2018-05-20 10:00',
            '2018-07-20 10:00',
        ];
        expect(actual).toEqual(expected);
    });

    test('filterLabels2', () => {
        const data = [
            '2018-02-20 10:00',
            '2018-03-20 10:00',
            '2018-03-22 10:00',
            '2018-05-25 10:00',
            '2018-06-20 10:00',
            '2018-10-20 10:00',
            '2018-12-20 10:00',
        ];
        const actual = getAcatualForFilterLabels(data);
        const expected = [
            '2018-02-20 10:00',
            '2018-06-20 10:00',
            '2018-10-20 10:00',
        ];
        expect(actual).toEqual(expected);
    });

    test('filterLabels3', () => {
        const data = [
            '2017-02-20 10:00',
            '2017-02-21 10:00',
            '2017-02-22 10:00',
            '2018-05-25 10:00',
            '2018-06-20 10:00',
            '2018-07-20 10:00',
            '2018-08-20 10:00',
        ];
        const actual = getAcatualForFilterLabels(data);
        const expected = ['2017-02-20 10:00', '2018-05-25 10:00'];
        expect(actual).toEqual(expected);
    });

    test('filterLabelsRealVals EcoSystem', () => {
        const data = [
            '2018-12-12 00:00',
            '2018-12-19 00:00',
            '2019-01-14 00:00',
            '2019-01-15 00:00',
            '2019-01-31 00:00',
            '2019-02-01 00:00',
            '2019-02-02 00:00',
            '2019-02-27 00:00',
        ];
        const actual = getAcatualForFilterLabels(data);
        const expected = [
            '2018-12-12 00:00',
            '2019-01-14 00:00',
            '2019-02-27 00:00',
        ];
        expect(actual).toEqual(expected);
    });

    test('filterLabelsRealVals man.m', () => {
        const data = [
            '2019-02-24 07:00',
            '2019-02-24 07:10',
            '2019-02-24 07:20',
            '2019-02-24 07:30',
            '2019-02-24 07:40',
            '2019-02-25 07:50',
        ];
        const actual = getAcatualForFilterLabels(data);
        const expected = ['2019-02-24 07:00', '2019-02-25 07:50'];
        expect(actual).toEqual(expected);
    });

    test('filterLabelsRealVals Menu Accept', () => {
        const data = [
            '2017-03-06 09:05',
            '2017-03-07 09:05',
            '2017-07-24 09:05',
            '2017-07-25 09:05',
            '2017-12-11 09:05',
            '2018-04-20 09:05',
            '2018-04-30 09:05',
            '2018-09-17 09:05',
            '2019-02-04 09:05',
        ];
        const actual = getAcatualForFilterLabels(data);
        const expected = [
            '2017-03-06 09:05',
            '2017-12-11 09:05',
            '2018-09-17 09:05',
        ];
        expect(actual).toEqual(expected);
    });
});
