jest.disableAutomock();

import getShortTariffClassName from '../getShortTariffClassName';

import {
    PLATZKARTE,
    COMPARTMENT,
    SUITE,
    COMMON,
    SITTING,
} from '../tariffClasses';

describe('getShortTariffClassName', () => {
    it('Вернет сокращенные названия классов вагонов', () => {
        expect(getShortTariffClassName(PLATZKARTE)).toBe('плац');
        expect(getShortTariffClassName(COMPARTMENT)).toBe('купе');
        expect(getShortTariffClassName(SUITE)).toBe('СВ');
        expect(getShortTariffClassName(COMMON)).toBe('общий');
        expect(getShortTariffClassName(SITTING)).toBe('сидяч');
    });

    it('Если класс вагона неизвестен - вернет пустую строку', () => {
        expect(getShortTariffClassName('platc')).toBe('');
        expect(getShortTariffClassName('virvir')).toBe('');
    });
});
