jest.disableAutomock();

import {IOS, isOS, isIOS} from '../os';

const osData = {
    name: IOS,
    version: '11.1.2',
};

describe('isOS', () => {
    it('вернёт false если не совпадает имя ОС', () =>
        expect(isOS(osData, 'Android', 11)).toBe(false));

    it('вернёт false если не совпадают версии', () =>
        expect(isOS(osData, IOS, 10)).toBe(false));

    it('вернёт true если имя и версия совпадают', () =>
        expect(isOS(osData, IOS, 11)).toBe(true));

    it('вернёт true если имя совпадает и не указана версия', () =>
        expect(isOS(osData, IOS)).toBe(true));

    it('вернёт false если не указали имя ОС', () =>
        expect(isOS(osData)).toBe(false));
});

describe('isIOS', () => {
    it('вернёт false если это не iOS', () =>
        expect(
            isIOS({
                name: 'Android',
                version: '7.1',
            }),
        ).toBe(false));

    it('венёт false если версия не совпадает', () =>
        expect(
            isIOS(
                {
                    name: IOS,
                    version: '9.11',
                },
                11,
            ),
        ).toBe(false));

    it('вернёт true если имя и версия совпадают', () =>
        expect(isIOS(osData, 11)).toBe(true));
});
