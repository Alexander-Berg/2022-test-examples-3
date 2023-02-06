import { getTablePaths } from '../queryHelpers';
import { YTTable } from '../../types';

let startDate: Date;
let endDate: Date;
let tables: YTTable[];
let expectedTableNames: [string, string];
let oneTableArr: YTTable[];

beforeEach(() => {
    startDate = new Date('2021-07-10');
    endDate = new Date('2021-07-20');
    tables = [
        { name: '2021-07-07', hasSchema: true, type: 'TABLE' },
        { name: '2021-07-10', hasSchema: true, type: 'TABLE' },
        { name: '2021-07-12', hasSchema: true, type: 'TABLE' },
        { name: '2021-07-15', hasSchema: true, type: 'TABLE' },
        { name: '2021-07-18', hasSchema: true, type: 'TABLE' },
        { name: '2021-07-20', hasSchema: true, type: 'TABLE' },
    ];
    expectedTableNames = ['2021-07-10', '2021-07-18'];

    oneTableArr = [
        { name: '2021-07-10', hasSchema: true, type: 'TABLE' },
    ];
});

describe('queryHelpers', () => {
    describe('getTablePaths', () => {
        it('should return paths to tables in YT, which are closest to start and end dates', () => {
            expect(getTablePaths(tables, startDate, endDate)).toEqual(expectedTableNames);
        });

        it('should return same date for start and end, if passed array with one table, which satisfy start and end dates', () => {
            const expectedTableNamesWithOneTable = [oneTableArr[0].name, oneTableArr[0].name];
            expect(getTablePaths(oneTableArr, startDate, endDate)).toEqual(expectedTableNamesWithOneTable);
        });

        it('should return null if there are not tables, which satisfy startDate and endDate', () => {
            expect(getTablePaths([], startDate, endDate)).toBe(null);
        });

        it('should return null if endDate is "earlier", that startDate', () => {
            expect(getTablePaths(tables, startDate, new Date(startDate.getTime() - 1e6))).toBe(null);
        });
    });
});
