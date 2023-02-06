import { mixToString } from '../../mix';

describe('mix', () => {
    it('Должен возвращать пустую строку на falsy значения', () => {
        expect(mixToString(null)).toBe('');
        expect(mixToString(undefined)).toBe('');
    });

    it('Должен возвращать валидный className для mix объекта', () => {
        const mix = {
            block: 'mix1',
            elem: 'elem1',
            mods: {
                mod1: 'val1',
            },
        };
        expect(mixToString(mix)).toBe('mix1__elem1 mix1__elem1_mod1_val1');
    });

    it('Должен возвращать валидный className для mix строки', () => {
        expect(mixToString('hello-world')).toBe('hello-world');
    });

    it('Должен возвращать валидный className для mix массива', () => {
        const mix = [
            {
                block: 'mix1',
                elem: 'elem1',
                mods: { mod1: 'val1' },
            },
            {
                block: 'mix2',
                elem: 'elem2',
                mods: { mod2: 'val2' },
            },
            'hello-world',
        ];
        expect(mixToString(mix)).toBe('mix1__elem1 mix1__elem1_mod1_val1 mix2__elem2 mix2__elem2_mod2_val2 hello-world');
    });

    it('Должен учитывать все типы данных объекта внутри mods', () => {
        const mix = {
            block: 'mix1',
            mods: {
                mod1: 'string',
                mod2: 12,
                mod3: true,
                mod4: false,
            },
        };
        expect(mixToString(mix)).toBe('mix1 mix1_mod1_string mix1_mod2_12 mix1_mod3');
    });

    it('Должен возвращать валидный className для объекта без elem', () => {
        const mix1 = {
            block: 'mix1',
            mods: { mod1: 'val1' },
        };
        const mix2 = [mix1];

        expect(mixToString(mix1)).toBe('mix1 mix1_mod1_val1');
        expect(mixToString(mix2)).toBe('mix1 mix1_mod1_val1');
    });

    it('Должен возвращать валидный className для объекта без mods', () => {
        const mix1 = {
            block: 'mix1',
            elem: 'elem1',
        };
        const mix2 = [mix1];

        expect(mixToString(mix1)).toBe('mix1__elem1');
        expect(mixToString(mix2)).toBe('mix1__elem1');
    });

    it('Должен возвращать валидный className для объекта без mods & elem', () => {
        const mix1 = {
            block: 'mix1',
        };
        const mix2 = [mix1];

        expect(mixToString(mix1)).toBe('mix1');
        expect(mixToString(mix2)).toBe('mix1');
    });
});
