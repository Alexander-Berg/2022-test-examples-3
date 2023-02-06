const space = require('../lib/space');

const expect = require('expect');

const defaultUnits = ['bytes', 'KB', 'MB', 'GB', 'TB'];
const defaultSeparator = ',';
const defaultSpaceObject = (bytes, props) => space.getSpaceObject(bytes, defaultUnits, defaultSeparator, props);
const defaultSpaceString = (bytes, props) => space.getSpaceString(bytes, defaultUnits, defaultSeparator, props);

const runCustomFormatTest = (bytes, props, expectedNumber, expectedUnit) => {
    expect(defaultSpaceObject(bytes, props)).toEqual({ number: expectedNumber, unit: expectedUnit });
    expect(defaultSpaceString(bytes, props)).toEqual(expectedNumber + ' ' + expectedUnit);
};

const runDefaultFormatTest = (
    bytes,
    expectedNumber,
    expectedUnit
) => runCustomFormatTest(bytes, undefined, expectedNumber, expectedUnit);

describe('space', () => {
    const _KB = 1024;
    const _MB = _KB * 1024;
    const _GB = _MB * 1024;
    const _TB = _GB * 1024;

    describe('Default format', () => {
        it('should return 0 bytes', () => {
            runDefaultFormatTest(0, '0', 'bytes');
        });
        it('should return 0 bytes [-13]', () => {
            runDefaultFormatTest(-13, '0', 'bytes');
        });
        it('should return 0 bytes ["two"]', () => {
            runDefaultFormatTest('two', '0', 'bytes');
        });
        it('should return 0 bytes [1/0]', () => {
            runDefaultFormatTest(1 / 0, '0', 'bytes');
        });
        it('should return 0 bytes [-1/0]', () => {
            runDefaultFormatTest(-1 / 0, '0', 'bytes');
        });
        it('should return 0 bytes [2^60]', () => {
            runDefaultFormatTest(Math.pow(2, 60), '0', 'bytes');
        });
        it('should return 2 bytes ["2"]', () => {
            runDefaultFormatTest('2', '2', 'bytes');
        });
        it('should return 3 bytes [PI]', () => {
            runDefaultFormatTest(Math.PI, '3', 'bytes');
        });
        it('should return 106 bytes', () => {
            runDefaultFormatTest(106, '106', 'bytes');
        });
        it('should return 1023 bytes', () => {
            runDefaultFormatTest(_KB - 1, '1023', 'bytes');
        });
        it('should return 1 KB', () => {
            runDefaultFormatTest(_KB, '1', 'KB');
        });
        it('should return 1 KB [2 KB - 1 byte]', () => {
            runDefaultFormatTest(2 * _KB - 1, '1', 'KB');
        });
        it('should return 2 KB', () => {
            runDefaultFormatTest(2 * _KB, '2', 'KB');
        });
        it('should return 99 KB', () => {
            runDefaultFormatTest(_KB * 100 - 1, '99', 'KB');
        });
        it('should return 1023 KB', () => {
            runDefaultFormatTest(_MB - 1, '1023', 'KB');
        });
        it('should return 1 MB', () => {
            runDefaultFormatTest(_MB, '1', 'MB');
        });
        it('should return 1 MB [1 MB + 102 KB]', () => {
            runDefaultFormatTest(_MB + 102 * _KB, '1', 'MB');
        });
        it('should return 1,1 MB [1 MB + 103 KB]', () => {
            runDefaultFormatTest(_MB + 103 * _KB, '1,1', 'MB');
        });
        it('should return 1023,9 MB', () => {
            runDefaultFormatTest(_GB - 1, '1023,9', 'MB');
        });
        it('should return 1 GB', () => {
            runDefaultFormatTest(_GB, '1', 'GB');
        });
        it('should return 1 GB [1 GB + 10 MB]', () => {
            runDefaultFormatTest(_GB + 10 * _MB, '1', 'GB');
        });
        it('should return 1,01 GB [1 GB + 11 MB]', () => {
            runDefaultFormatTest(_GB + 11 * _MB, '1,01', 'GB');
        });
        it('should return 33,33 GB', () => {
            runDefaultFormatTest(_GB * 33.333, '33,33', 'GB');
        });
        it('should return 1023,99 GB', () => {
            runDefaultFormatTest(_TB - 1, '1023,99', 'GB');
        });
        it('should return 1 TB', () => {
            runDefaultFormatTest(_TB, '1', 'TB');
        });
        it('should return 1 TB [1 TB + 10 GB]', () => {
            runDefaultFormatTest(_TB + 10 * _GB, '1', 'TB');
        });
        it('should return 1,01 TB [1 TB + 11 GB]', () => {
            runDefaultFormatTest(_TB + 11 * _GB, '1,01', 'TB');
        });
        it('should return 3,23 TB', () => {
            runDefaultFormatTest(_TB * 42 / 13, '3,23', 'TB');
        });
        it('should return 1234,56 TB', () => {
            runDefaultFormatTest(_TB * 1234.56789, '1234,56', 'TB');
        });
    });
    describe('Custom formats', () => {
        it('should return 512,28 KB', () => {
            runCustomFormatTest(524576, { precision: 2 }, '512,28', 'KB');
        });
        it('should return 1,5 MB', () => {
            runCustomFormatTest(1572864, { precision: 20 }, '1,5', 'MB');
        });
        it('should return 1,46 GB', () => {
            runCustomFormatTest(1573741824, undefined, '1,46', 'GB');
        });
        it('should return 1,46566 GB', () => {
            runCustomFormatTest(1573741824, { precision: 5 }, '1,46566', 'GB');
        });
        it('should return 1,001 GB [1 GB + 1 MB + 25 KB, precision: 3]', () => {
            runCustomFormatTest(_GB + _MB + 25 * _KB, { precision: 3 }, '1,001', 'GB');
        });
        it('should return 1 GB [1 GB + 1 MB + 25 KB, precision: 2]', () => {
            runCustomFormatTest(_GB + _MB + 25 * _KB, { precision: 2 }, '1', 'GB');
        });
        it('should return 1,101 GB [precision: 3]', () => {
            runCustomFormatTest(1182189750, { precision: 3 }, '1,101', 'GB');
        });
        it('should return 1,1 GB [precision: 2]', () => {
            runCustomFormatTest(1182189750, { precision: 2 }, '1,1', 'GB');
        });
        it('should return 230 MB', () => {
            runCustomFormatTest(241332224, { length: 3 }, '230', 'MB');
        });
        it('should return 16,1 GB', () => {
            runCustomFormatTest(17336107008, { length: 3 }, '16,1', 'GB');
        });
        it('should return 4,76 MB', () => {
            runCustomFormatTest(4996096, { length: 3 }, '4,76', 'MB');
        });
        it('should return 16,14 GB', () => {
            runCustomFormatTest(17336107008, { length: 4 }, '16,14', 'GB');
        });
    });
    describe('Custom units', () => {
        const customUnits = ['b', 'k', 'm', 'g', 't'];
        const customSeparator = '_';
        const customSpaceObject = (bytes) => space.getSpaceObject(bytes, customUnits, customSeparator);
        const customSpaceString = (bytes) => space.getSpaceString(bytes, customUnits, customSeparator);

        const runCustomUnitsTest = (bytes, expectedNumber, expectedUnit) => {
            expect(customSpaceObject(bytes)).toEqual({ number: expectedNumber, unit: expectedUnit });
            expect(customSpaceString(bytes)).toEqual(expectedNumber + ' ' + expectedUnit);
        };

        it('should return 1 b', () => {
            runCustomUnitsTest(1.234, '1', 'b');
        });
        it('should return 1 k', () => {
            runCustomUnitsTest(_KB * 1.234, '1', 'k');
        });
        it('should return 1_2 m', () => {
            runCustomUnitsTest(_MB * 1.234, '1_2', 'm');
        });
        it('should return 1_23 g', () => {
            runCustomUnitsTest(_GB * 1.234, '1_23', 'g');
        });
        it('should return 1_23 t', () => {
            runCustomUnitsTest(_TB * 1.234, '1_23', 't');
        });
    });
    describe('Errors', () => {
        it('should throw "units must be an array" with minimum length', () => {
            expect(() => space.getSpaceObject(1, [], ',')).toThrow();
        });
    });
});
