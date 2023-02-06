jest.disableAutomock();

import {replaceNewLinesForMobiles} from '../../stringUtils';

describe('replaceNewLinesForMobiles', () => {
    const stringOne = 'Один два\n Три';
    const stringTwo = 'Один\n\n два\n Три';
    const stringThree = 'Один два Три';

    it('Функция должна удалять новые строки на мобильных устройствах', () => {
        expect(replaceNewLinesForMobiles(stringOne, true)).toBe('Один два Три');
        expect(replaceNewLinesForMobiles(stringTwo, true)).toBe('Один два Три');
        expect(replaceNewLinesForMobiles(stringThree, true)).toBe(
            'Один два Три',
        );
    });

    it('Функция должна оставлять строку исходной на декстопах', () => {
        expect(replaceNewLinesForMobiles(stringOne)).toBe(stringOne);
        expect(replaceNewLinesForMobiles(stringTwo)).toBe(stringTwo);
        expect(replaceNewLinesForMobiles(stringThree)).toBe(stringThree);
    });
});
