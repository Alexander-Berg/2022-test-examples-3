import spaceHelper from 'helpers/space';

describe('spaceHelper', () => {
    const format = spaceHelper.format;

    const _KB = 1024;
    const _MB = _KB * 1024;
    const _GB = _MB * 1024;
    const _TB = _GB * 1024;

    describe('Default format', () => {
        it('должнен вернуть 0 байт', () => {
            expect(format(0)).toBe('0 байт');
        });
        it('должнен вернуть 0 байт [-13]', () => {
            expect(format(-13)).toBe('0 байт');
        });
        it('должнен вернуть 0 байт ["два"]', () => {
            expect(format('два')).toBe('0 байт');
        });
        it('должнен вернуть 0 байт [1/0]', () => {
            expect(format(1 / 0)).toBe('0 байт');
        });
        it('должнен вернуть 0 байт [-1/0]', () => {
            expect(format(-1 / 0)).toBe('0 байт');
        });
        it('должнен вернуть 0 байт [2^60]', () => {
            expect(format(Math.pow(2, 60))).toBe('0 байт');
        });
        it('должнен вернуть 2 байт ["2"]', () => {
            expect(format('2')).toBe('2 байт');
        });
        it('должнен вернуть 3 байт [PI]', () => {
            expect(format(Math.PI)).toBe('3 байт');
        });
        it('должнен вернуть 106 байт', () => {
            expect(format(106)).toBe('106 байт');
        });
        it('должнен вернуть 1023 байт', () => {
            expect(format(_KB - 1)).toBe('1023 байт');
        });
        it('должнен вернуть 1 КБ', () => {
            expect(format(_KB)).toBe('1 КБ');
        });
        it('должнен вернуть 99 КБ', () => {
            expect(format(_KB * 100 - 1)).toBe('99 КБ');
        });
        it('должнен вернуть 1023 КБ', () => {
            expect(format(_MB - 1)).toBe('1023 КБ');
        });
        it('должнен вернуть 1 МБ', () => {
            expect(format(_MB)).toBe('1 МБ');
        });
        it('должнен вернуть 1023,9 МБ', () => {
            expect(format(_GB - 1)).toBe('1023,9 МБ');
        });
        it('должнен вернуть 1 ГБ', () => {
            expect(format(_GB)).toBe('1 ГБ');
        });
        it('должнен вернуть 33,33 ГБ', () => {
            expect(format(_GB * 33.333)).toBe('33,33 ГБ');
        });
        it('должнен вернуть 1023,99 ГБ', () => {
            expect(format(_TB - 1)).toBe('1023,99 ГБ');
        });
        it('должнен вернуть 1 ТБ', () => {
            expect(format(_TB)).toBe('1 ТБ');
        });
        it('должнен вернуть 3,23 ТБ', () => {
            expect(format(_TB * 42 / 13)).toBe('3,23 ТБ');
        });
        it('должнен вернуть 1234,56 ТБ', () => {
            expect(format(_TB * 1234.56789)).toBe('1234,56 ТБ');
        });
    });
    describe('Custom formats', () => {
        it('должнен вернуть 1 КБ', () => {
            expect(format(1024, 'KB')).toBe('1 КБ');
        });
        it('должнен вернуть 1512 КБ', () => {
            expect(format(1548576, 'KB')).toBe('1512 КБ');
        });
        it('должнен вернуть 1512,28 КБ', () => {
            expect(format(1548576, 'KB', 'precision', 2)).toBe('1512,28 КБ');
        });
        it('должнен вернуть 1,4 МБ', () => {
            expect(format(1548576)).toBe('1,4 МБ');
        });
        it('должнен вернуть 1,5 МБ', () => {
            expect(format(1572864, 'precision', 20)).toBe('1,5 МБ');
        });
        it('должнен вернуть 1,50000 МБ', () => {
            expect(format('1572864', 'MB', 'precision', 5, 'fixed')).toBe('1,50000 МБ');
        });
        it('должнен вернуть 0,09 ГБ', () => {
            expect(format('107374182', 'GB')).toBe('0,09 ГБ');
        });
        it('должнен вернуть 1,4 ГБ', () => {
            expect(format('1573741824', 'best unit', 'rough')).toBe('1,4 ГБ');
        });
        it('должнен вернуть 1,46 ГБ', () => {
            expect(format('1573741824')).toBe('1,46 ГБ');
        });
        it('должнен вернуть 1,46566 ГБ', () => {
            expect(format('1573741824', 'precision', 5)).toBe('1,46566 ГБ');
        });
        it('должнен вернуть 1 ГБ', () => {
            expect(format('1573741824', 'int')).toBe('1 ГБ');
        });
        it('должнен вернуть 153', () => {
            expect(format('157374', 'number')).toBe('153');
        });
        it('должнен вернуть КБ', () => {
            expect(format('157374', 'unit')).toBe('КБ');
        });
        it('должнен вернуть 1 МБ', () => {
            expect(format('1048577', 'rough')).toBe('1 МБ');
        });
        it('должнен вернуть 1,5 ГБ', () => {
            expect(format('1610612736', 'rough')).toBe('1,5 ГБ');
        });
        it('должнен вернуть 230 МБ', () => {
            expect(format('241332224', 'length', 3)).toBe('230 МБ');
        });
        it('должнен вернуть 16,1 ГБ', () => {
            expect(format('17336107008', 'length', 3)).toBe('16,1 ГБ');
        });
        it('должнен вернуть 4,76 МБ', () => {
            expect(format('4996096', 'length', 3)).toBe('4,76 МБ');
        });
        it('должнен вернуть 16,14 ГБ', () => {
            expect(format('17336107008', 'length', 4)).toBe('16,14 ГБ');
        });
    });
    describe('Custom units', () => {
        const format = spaceHelper.create(['b', 'k', 'm', 'g', 't'], '_').format;

        it('должнен вернуть 1 b', () => {
            expect(format(1.234)).toBe('1 b');
        });
        it('должнен вернуть 1 k', () => {
            expect(format(_KB * 1.234)).toBe('1 k');
        });
        it('должнен вернуть 1_2 m', () => {
            expect(format(_MB * 1.234)).toBe('1_2 m');
        });
        it('должнен вернуть 1_23 g', () => {
            expect(format(_GB * 1.234)).toBe('1_23 g');
        });
        it('должнен вернуть 1_23 t', () => {
            expect(format(_TB * 1.234)).toBe('1_23 t');
        });
    });
});
