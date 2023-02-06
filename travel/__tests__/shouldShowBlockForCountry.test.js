jest.disableAutomock();

import shouldShowBlockForCountry from '../shouldShowBlockForCountry';
import {
    TRAIN_TICKETS,
    PUBLIC_TRANSPORT,
    PROMOTIONS,
    ABOUT_TRAINS,
} from '../constants/homepageBlocks';
import {TLD_RU} from '../tlds';

const TLD_UNKNOWN = 'unknown';

describe('shouldShowBlockForCountry', () => {
    it('Вернет true для блока билетов на поезда для России', () => {
        expect(shouldShowBlockForCountry(TRAIN_TICKETS, TLD_RU)).toBe(true);
    });

    it('Вернет true для блока публичного транспорта для России', () => {
        expect(shouldShowBlockForCountry(PUBLIC_TRANSPORT, TLD_RU)).toBe(true);
    });

    it('Вернет true для блока промо для России', () => {
        expect(shouldShowBlockForCountry(PROMOTIONS, TLD_RU)).toBe(true);
    });

    it('Вернет true для блока про поезда для России', () => {
        expect(shouldShowBlockForCountry(ABOUT_TRAINS, TLD_RU)).toBe(true);
    });

    it('Вернет false для блока про поезда для неизвестного домена', () => {
        expect(shouldShowBlockForCountry(PROMOTIONS, TLD_UNKNOWN)).toBe(false);
    });
});
