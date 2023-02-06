import type { IFlags } from './flags';
import { getFlags, reduceWhitelisted } from './flags';

describe('reduceWhitelisted', () => {
    let flags: IFlags;

    beforeEach(() =>{
        flags = { a: 1, b: 2, c: 'val' };
    });

    it('должен отфильтровать согласно whitelist', () => {
        const actual = reduceWhitelisted(flags, new Set(['a', 'b']));
        expect(actual).toEqual({ a: 1, b: 2 });
    });

    it('должен вернуть пустой объект, если whitelist пустой', () => {
        const actual = reduceWhitelisted(flags, new Set());
        expect(actual).toEqual({});
    });
});

describe('getFlags', () => {
    const whitelist = new Set(['active_tab', 'enable_sorting', 'some_flag']);
    let rootFlags : {};
    let contextFlags = {};
    let flagsJson: {};

    beforeEach(() =>{
        rootFlags = { some_flag: 'value' };
        contextFlags = {
            active_tab: 1,
            enable_sorting: 1,
        };
        flagsJson = {
            ...rootFlags,
            waste: 'waste',
        };
    });

    it('должен вернуть только флаги с корневого уровня, если не указан CONTEXT', () => {
        expect(rootFlags).toEqual(getFlags(flagsJson, whitelist));
    });

    it('должен вернуть только флаги с корневого уровня, если CONTEXT пустой', () => {
        flagsJson = {
            ...flagsJson,
            CONTEXT: {},
        };

        const actual = getFlags(flagsJson, whitelist);
        expect(rootFlags).toEqual(actual);
    });

    it('должен вернуть и флаги с корневого уровня и из CONTEXT', () => {
        flagsJson = {
            ...flagsJson,
            CONTEXT: { MAIN: { PRODUCTS: { ...contextFlags, also_waste: 'also_waste' } } },
        };

        const expected = {
            ...rootFlags,
            ...contextFlags,
        };
        expect(expected).toEqual(getFlags(flagsJson, whitelist));
    });

    it('должен вернуть значение флага с корневого уровня, у него приоритет', () => {
        const flagsJson = {
            active_tab: 1,
            CONTEXT: { MAIN: { PRODUCTS: { active_tab: 2 } } },
        };

        expect(1).toEqual(getFlags(flagsJson, whitelist).active_tab);
    });
});
