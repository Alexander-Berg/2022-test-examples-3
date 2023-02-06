import {
    toBaseUnit,
    getUnitsListForHardwareRequest,
    getDefaultUnit,
    defaultUnits,
    providerDefaultUnits,
    toHumanUnit,
    resourceConvertersByUnit,
} from './units';

describe('toBaseUnit works properly', () => {
    it('for enumerable', () => {
        expect(toBaseUnit({ unit: 'PERMILLE', value: 42000 })).toEqual({ value: 42 * 1e3, unit: 'PERMILLE' });
        expect(toBaseUnit({ unit: 'PERCENT', value: 4200 })).toEqual({ value: 42 * 1e3, unit: 'PERMILLE' });
        expect(toBaseUnit({ unit: 'COUNT', value: 42 })).toEqual({ value: 42 * 1e3, unit: 'PERMILLE' });
        expect(toBaseUnit({ unit: 'KILO', value: 42 })).toEqual({ value: 42 * 1e6, unit: 'PERMILLE' });
        expect(toBaseUnit({ unit: 'MEGA', value: 42 })).toEqual({ value: 42 * 1e9, unit: 'PERMILLE' });
        expect(toBaseUnit({ unit: 'GIGA', value: 42 })).toEqual({ value: 42 * 1e12, unit: 'PERMILLE' });
    });
    it('for memory', () => {
        expect(toBaseUnit({ unit: 'BYTE', value: 42 })).toEqual({ value: 42, unit: 'BYTE' });
        expect(toBaseUnit({ unit: 'KIBIBYTE', value: 42 })).toEqual({ value: 43008, unit: 'BYTE' });
        expect(toBaseUnit({ unit: 'MEBIBYTE', value: 42 })).toEqual({ value: 44040192, unit: 'BYTE' });
        expect(toBaseUnit({ unit: 'GIBIBYTE', value: 42 })).toEqual({ value: 45097156608, unit: 'BYTE' });
        expect(toBaseUnit({ unit: 'TEBIBYTE', value: 42 })).toEqual({ value: 46179488366592, unit: 'BYTE' });
    });
    it('for money', () => {
        expect(toBaseUnit({ unit: 'CURRENCY', value: 42 })).toEqual({ value: 42, unit: 'CURRENCY' });
    });
    it('for traffic', () => {
        expect(toBaseUnit({ unit: 'BPS', value: 42 })).toEqual({ value: 42, unit: 'BPS' });
        expect(toBaseUnit({ unit: 'KBPS', value: 42 })).toEqual({ value: 42000, unit: 'BPS' });
        expect(toBaseUnit({ unit: 'MBPS', value: 42 })).toEqual({ value: 42000000, unit: 'BPS' });
        expect(toBaseUnit({ unit: 'GBPS', value: 42 })).toEqual({ value: 42000000000, unit: 'BPS' });
        expect(toBaseUnit({ unit: 'TBPS', value: 42 })).toEqual({ value: 42000000000000, unit: 'BPS' });
    });
    it('for binary traffic', () => {
        expect(toBaseUnit({ unit: 'BINARY_BPS', value: 42 })).toEqual({ value: 42, unit: 'BINARY_BPS' });
        expect(toBaseUnit({ unit: 'KIBPS', value: 42 })).toEqual({ value: 43008, unit: 'BINARY_BPS' });
        expect(toBaseUnit({ unit: 'MIBPS', value: 42 })).toEqual({ value: 44040192, unit: 'BINARY_BPS' });
        expect(toBaseUnit({ unit: 'GIBPS', value: 42 })).toEqual({ value: 45097156608, unit: 'BINARY_BPS' });
        expect(toBaseUnit({ unit: 'TIBPS', value: 42 })).toEqual({ value: 46179488366592, unit: 'BINARY_BPS' });
    });
    it('for processor', () => {
        expect(toBaseUnit({ unit: 'PERMILLE_CORES', value: 42000 })).toEqual({ value: 42 * 1e3, unit: 'PERMILLE_CORES' });
        expect(toBaseUnit({ unit: 'PERCENT_CORES', value: 4200 })).toEqual({ value: 42 * 1e3, unit: 'PERMILLE_CORES' });
        expect(toBaseUnit({ unit: 'CORES', value: 42 })).toEqual({ value: 42 * 1e3, unit: 'PERMILLE_CORES' });
        expect(toBaseUnit({ unit: 'KILO_CORES', value: 42 })).toEqual({ value: 42 * 1e6, unit: 'PERMILLE_CORES' });
        expect(toBaseUnit({ unit: 'MEGA_CORES', value: 42 })).toEqual({ value: 42 * 1e9, unit: 'PERMILLE_CORES' });
        expect(toBaseUnit({ unit: 'GIGA_CORES', value: 42 })).toEqual({ value: 42 * 1e12, unit: 'PERMILLE_CORES' });
    });
    it('for invalid unit', () => {
        expect(toBaseUnit({ unit: 'SOME_INVALID_UNIT_11', value: 42 })).toEqual({ value: null, unit: null });
    });
});

describe('resourceConvertersByUnit self converts', () => {
    it('BPS -> BPS', () => {
        const actual = resourceConvertersByUnit.BPS.BPS(42);
        const expected = { unit: 'BPS', value: 42 };

        expect(actual).toEqual(expected);
    });

    it('PERMILLE -> PERMILLE', () => {
        const actual = resourceConvertersByUnit.PERMILLE.PERMILLE(42);
        const expected = { unit: 'PERMILLE', value: 42 };

        expect(actual).toEqual(expected);
    });

    it('PERCENT -> PERCENT', () => {
        const actual = resourceConvertersByUnit.PERCENT.PERCENT(42);
        const expected = { unit: 'PERCENT', value: 42 };

        expect(actual).toEqual(expected);
    });

    it('CURRENCY -> CURRENCY', () => {
        const actual = resourceConvertersByUnit.CURRENCY.CURRENCY(42);
        const expected = { unit: 'CURRENCY', value: 42 };

        expect(actual).toEqual(expected);
    });

    it('RUBLES -> RUBLES', () => {
        const actual = resourceConvertersByUnit.RUBLES.RUBLES(42);
        const expected = { unit: 'RUBLES', value: 42 };

        expect(actual).toEqual(expected);
    });

    it('BYTE -> BYTE', () => {
        const actual = resourceConvertersByUnit.BYTE.BYTE(42);
        const expected = { unit: 'BYTE', value: 42 };

        expect(actual).toEqual(expected);
    });

    it('PERMILLE_CORES -> PERMILLE_CORES', () => {
        const actual = resourceConvertersByUnit.PERMILLE_CORES.PERMILLE_CORES(42);
        const expected = { unit: 'PERMILLE_CORES', value: 42 };

        expect(actual).toEqual(expected);
    });

    it('BINARY_BPS -> BINARY_BPS', () => {
        const actual = resourceConvertersByUnit.BINARY_BPS.BINARY_BPS(42);
        const expected = { unit: 'BINARY_BPS', value: 42 };

        expect(actual).toEqual(expected);
    });
});

describe('getUnitsListForHardwareRequest works properly', () => {
    it('for enumerable', () => {
        const result = getUnitsListForHardwareRequest('ENUMERABLE');

        expect(result).toEqual([
            { key: 'COUNT', name: 'units' },
        ]);

        expect(getUnitsListForHardwareRequest('ENUMERABLE')).toBe(result);
    });

    it('for memory', () => {
        const result = getUnitsListForHardwareRequest('MEMORY');

        expect(result).toEqual([
            { key: 'MEBIBYTE', name: 'MiB' },
            { key: 'GIBIBYTE', name: 'GiB' },
            { key: 'TEBIBYTE', name: 'TiB' },
        ]);

        expect(getUnitsListForHardwareRequest('MEMORY')).toBe(result);
    });

    it('for money', () => {
        const result = getUnitsListForHardwareRequest('MONEY');

        expect(result).toEqual([
            { key: 'CURRENCY', name: 'Cur' },
        ]);

        expect(getUnitsListForHardwareRequest('MONEY')).toBe(result);
    });

    it('for power', () => {
        const result = getUnitsListForHardwareRequest('POWER');

        expect(result).toEqual([
            { key: 'PERMILLE', name: '‰' },
            { key: 'PERCENT', name: '%' },
            { key: 'COUNT', name: 'units' },
            { key: 'KILO', name: 'K units' },
            { key: 'MEGA', name: 'M units' },
            { key: 'GIGA', name: 'G units' },
        ]);

        expect(getUnitsListForHardwareRequest('POWER')).toBe(result);
    });

    it('for storage', () => {
        const result = getUnitsListForHardwareRequest('STORAGE');

        expect(result).toEqual([
            { key: 'MEBIBYTE', name: 'MiB' },
            { key: 'GIBIBYTE', name: 'GiB' },
            { key: 'TEBIBYTE', name: 'TiB' },
        ]);

        expect(getUnitsListForHardwareRequest('STORAGE')).toBe(result);
    });

    it('for traffic', () => {
        const result = getUnitsListForHardwareRequest('TRAFFIC');

        expect(result).toEqual([
            { key: 'BPS', name: 'B/s' },
            { key: 'KBPS', name: 'kB/s' },
            { key: 'MBPS', name: 'MB/s' },
            { key: 'GBPS', name: 'GB/s' },
            { key: 'TBPS', name: 'TB/s' },
        ]);

        expect(getUnitsListForHardwareRequest('TRAFFIC')).toBe(result);
    });

    it('for binary traffic', () => {
        const result = getUnitsListForHardwareRequest('BINARY_TRAFFIC');

        expect(result).toEqual([
            { key: 'BINARY_BPS', name: 'B/s' },
            { key: 'KIBPS', name: 'KiB/s' },
            { key: 'MIBPS', name: 'MiB/s' },
            { key: 'GIBPS', name: 'GiB/s' },
            { key: 'TIBPS', name: 'TiB/s' },
        ]);

        expect(getUnitsListForHardwareRequest('BINARY_TRAFFIC')).toBe(result);
    });

    it('for processor', () => {
        const result = getUnitsListForHardwareRequest('PROCESSOR');

        expect(result).toEqual([
            { key: 'CORES', name: 'cores' },
        ]);

        expect(getUnitsListForHardwareRequest('PROCESSOR')).toBe(result);
    });

    it('for invalid type', () => {
        const result = getUnitsListForHardwareRequest('SOME_INVALID_TYPE_22');

        expect(result).toEqual([]);

        expect(getUnitsListForHardwareRequest('SOME_INVALID_TYPE_22')).toBe(result);
    });

    it('for provider which has not special unit list', () => {
        const type = 'STORAGE';
        const providerKey = 'yt';

        expect(getUnitsListForHardwareRequest(type, providerKey)).toEqual(getUnitsListForHardwareRequest(type));
    });

    it('for specific provider which has other unit list', () => {
        const type = 'STORAGE';
        const providerKey = 'dbaas';

        const unitsWithoutProvider = getUnitsListForHardwareRequest(type);
        const unitsWithProvider = getUnitsListForHardwareRequest(type, providerKey);

        expect(unitsWithProvider).not.toEqual(unitsWithoutProvider);

        expect(unitsWithProvider).toEqual([{ key: 'GIBIBYTE', name: 'GiB' }]);
    });
});

describe('getDefaultUnit works properly', () => {
    it('for enumerable', () => {
        expect(getDefaultUnit('ENUMERABLE')).toBe('COUNT');
    });

    it('for memory', () => {
        expect(getDefaultUnit('MEMORY')).toBe('GIBIBYTE');
    });

    it('for money', () => {
        expect(getDefaultUnit('MONEY')).toBe('CURRENCY');
    });

    it('for power', () => {
        expect(getDefaultUnit('POWER')).toBe('COUNT');
    });

    it('for storage', () => {
        expect(getDefaultUnit('STORAGE')).toBe('GIBIBYTE');
    });

    it('for traffic', () => {
        expect(getDefaultUnit('TRAFFIC')).toBe('GBPS');
    });

    it('for binary traffic', () => {
        expect(getDefaultUnit('BINARY_TRAFFIC')).toBe('GIBPS');
    });

    it('for processor', () => {
        expect(getDefaultUnit('PROCESSOR')).toBe('CORES');
    });

    it('for invalid type', () => {
        expect(getDefaultUnit('SOME_INVALID_TYPE_22')).toBe(undefined);
    });

    it('for specific provider, which have other default units', () => {
        const type = 'TRAFFIC';
        const expectedDefaultUnit = 'MBPS';

        // иначе тест проверяет просто взятие дефолтного значения, а не конкретного провайдера
        expect(defaultUnits[type] !== expectedDefaultUnit).toBeTruthy();
        expect(getDefaultUnit(type, 'logbroker')).toBe(expectedDefaultUnit);
    });

    it('for specific provider and resource, which have other default units', () => {
        const type = 'STORAGE';
        const expectedDefaultUnit = 'TEBIBYTE';

        // иначе тест проверяет просто взятие дефолтного значения, а не конкретного провайдера
        expect(defaultUnits[type] !== expectedDefaultUnit).toBeTruthy();
        expect(getDefaultUnit(type, 'yt', 'hdd')).toBe(expectedDefaultUnit);
    });

    it('for specific provider but default resource', () => {
        const provider = 'yt';
        const type = 'STORAGE';
        const expectedDefaultUnit = 'GIBIBYTE';

        // переопределение дефолта для провайдера есть, но не для этого ресурса
        expect(providerDefaultUnits[provider] !== undefined).toBeTruthy();
        expect(getDefaultUnit(type, provider, 'ssd')).toBe(expectedDefaultUnit);
    });
});

describe('toHumanUnit works properly', () => {
    it('for BPS', () => {
        const step = 1000;
        const base = 2;
        const units = ['KBPS', 'MBPS', 'GBPS', 'TBPS'];

        for (let i = 0; i < 4; i++) {
            const desiredUnit = units[i];
            const currentDenominator = Math.pow(step, i + 1);
            const result = toHumanUnit({
                base: { unit: 'BPS', value: base * currentDenominator },
            });

            expect(result).toEqual({ unit: desiredUnit, value: base });
        }
    });

    it('for PERMILLE', () => {
        const step = 10;
        const base = 3;
        const units = ['PERCENT', 'COUNT', 'KILO', 'MEGA', 'GIGA'];

        for (let i = 0; i < 5; i++) {
            const desiredUnit = units[i];
            const currentDenominator = Math.pow(step, i === 0 ? 1 : i * 3);
            const result = toHumanUnit({
                base: { unit: 'PERMILLE', value: base * currentDenominator },
            });

            expect(result).toEqual({ unit: desiredUnit, value: base });
        }
    });

    it('for PERCENT', () => {
        const base = 4;
        const units = ['COUNT', 'KILO', 'MEGA', 'GIGA'];
        const denominators = [1e2, 1e5, 1e8, 1e11];

        for (let i = 0; i < 4; i++) {
            const desiredUnit = units[i];
            const currentDenominator = denominators[i];
            const result = toHumanUnit({
                base: { unit: 'PERCENT', value: base * currentDenominator },
            });

            expect(result).toEqual({ unit: desiredUnit, value: base });
        }
    });

    it('for CURRENCY', () => {
        const result = toHumanUnit({
            base: { unit: 'CURRENCY', value: 1250 },
        });

        expect(result).toEqual({ unit: 'CURRENCY', value: 1250 });
    });

    // it('for RUBLES', () => {
    //     const step = 1000;
    //     const base = 50;
    //     const units = ['THOUSAND_RUBLES', 'MILLION_RUBLES'];

    //     for (let i = 0; i < 2; i++) {
    //         const desiredUnit = units[i];
    //         const currentDenominator = Math.pow(step, i + 1);
    //         const result = toHumanUnit({
    //             base: { unit: 'RUBLES', value: base * currentDenominator },
    //         });

    //         expect(result).toEqual({ unit: desiredUnit, value: base });
    //     }
    // });

    it('for BYTE', () => {
        const step = 1024;
        const base = 123;
        const units = ['KIBIBYTE', 'MEBIBYTE', 'GIBIBYTE', 'TEBIBYTE'];

        for (let i = 0; i < 4; i++) {
            const desiredUnit = units[i];
            const currentDenominator = Math.pow(step, i + 1);
            const result = toHumanUnit({
                base: { unit: 'BYTE', value: base * currentDenominator },
            });

            expect(result).toEqual({ unit: desiredUnit, value: base });
        }
    });

    it('for PERMILLE_CORES', () => {
        const base = 234;
        const units = ['PERCENT_CORES', 'CORES', 'KILO_CORES', 'MEGA_CORES', 'GIGA_CORES'];
        const denominators = [1e1, 1e3, 1e6, 1e9, 1e12];

        for (let i = 0; i < 5; i++) {
            const desiredUnit = units[i];
            const currentDenominator = denominators[i];
            const result = toHumanUnit({
                base: { unit: 'PERMILLE_CORES', value: base * currentDenominator },
            });

            expect(result).toEqual({ unit: desiredUnit, value: base });
        }
    });

    it('for BINARY_BPS', () => {
        const step = 1024;
        const base = 123;
        const units = ['KIBPS', 'MIBPS', 'GIBPS', 'TIBPS'];

        for (let i = 0; i < 4; i++) {
            const desiredUnit = units[i];
            const currentDenominator = Math.pow(step, i + 1);
            const result = toHumanUnit({
                base: { unit: 'BINARY_BPS', value: base * currentDenominator },
            });

            expect(result).toEqual({ unit: desiredUnit, value: base });
        }
    });

    it('for 100%', () => {
        const actual = toHumanUnit({
            base: { unit: 'PERMILLE', value: 1000 },
        });
        const expected = { unit: 'COUNT', value: 1 };

        expect(actual).toEqual(expected);
    });

    it('approximated', () => {
        const actual = toHumanUnit({
            base: { unit: 'PERMILLE', value: 1000 },
            approximate: true,
        });
        const expected = { unit: 'COUNT', value: 1 };

        expect(actual).toEqual(expected);
    });

    it('approximated non-ceil', () => {
        const actual = toHumanUnit({
            base: { unit: 'PERMILLE', value: 999 },
            approximate: true,
        });
        const expected = { unit: 'PERCENT', value: 99.9 };

        expect(actual).toEqual(expected);
    });

    it('with resourceType', () => {
        const actual = toHumanUnit({
            base: { unit: 'PERMILLE', value: 1000000 },
            resourceType: 'ENUMERABLE',
        });
        const expected = { unit: 'COUNT', value: 1000 };

        expect(actual).toEqual(expected);
    });

    it('for zero value', () => {
        const actual = toHumanUnit({
            base: { unit: 'PERMILLE', value: 0 },
            resourceType: 'ENUMERABLE',
        });
        const expected = { unit: 'COUNT', value: 0 };

        expect(actual).toEqual(expected);
    });
});
