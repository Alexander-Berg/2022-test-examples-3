jest.disableAutomock();

import addNoIndexPageToMeta, {noIndex} from '../addNoIndexPageToMeta';

const fooElement = {content: 'foo', name: 'bar'};

describe('addNoIndexPageToMeta', () => {
    it('В meta нет noIndex', () => {
        const meta = [fooElement];

        expect(addNoIndexPageToMeta(meta)).toEqual([fooElement, noIndex]);
    });

    it('В meta есть noIndex', () => {
        const meta = [fooElement, noIndex];

        expect(addNoIndexPageToMeta(meta)).toEqual(meta);
    });

    it('Данная функция мутирует исходный объект', () => {
        const meta = [fooElement];

        expect(addNoIndexPageToMeta(meta)).toBe(meta);
    });
});
