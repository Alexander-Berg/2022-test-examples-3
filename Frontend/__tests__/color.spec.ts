import {
    hex2rgb,
    brightness,
    isLight,
    isWhite,
} from '../color';

describe('Хэлперы для работы с цветом', () => {
    describe('hex2rgb', () => {
        it('Возвращает rgb объект из #aa00F1', () => {
            expect(hex2rgb('#aa00F1'))
                .toEqual({
                    r: 170,
                    g: 0,
                    b: 241,
                });
        });
        it('Возвращает rgb объект из #a0F', () => {
            expect(hex2rgb('#a0F'))
                .toEqual({
                    r: 170,
                    g: 0,
                    b: 255,
                });
        });
        it('Возвращает rgb объект из aa00F1', () => {
            expect(hex2rgb('aa00F1'))
                .toEqual({
                    r: 170,
                    g: 0,
                    b: 241,
                });
        });
        it('Возвращает rgb объект из a0F', () => {
            expect(hex2rgb('a0F'))
                .toEqual({
                    r: 170,
                    g: 0,
                    b: 255,
                });
        });
        it('Возвращает null из невалидного цвета', () => {
            expect(hex2rgb('#aa0g'))
                .toEqual(null);
        });
    });
    describe('brightness', () => {
        it('Возвращает 255 для белого', () => {
            expect(brightness({
                r: 255,
                g: 255,
                b: 255,
            })).toEqual(255);
        });
        it('Возвращает 0 для черного', () => {
            expect(brightness({
                r: 0,
                g: 0,
                b: 0,
            })).toEqual(0);
        });
        it('Возвращает 76.24499999999999 для красного', () => {
            expect(brightness({
                r: 255,
                g: 0,
                b: 0,
            })).toEqual(76.24499999999999);
        });
    });
    describe('isLight', () => {
        it('Возвращает false для черного', () => {
            expect(isLight({
                r: 0,
                g: 0,
                b: 0,
            })).toEqual(false);
        });
        it('Возвращает true для белого', () => {
            expect(isLight({
                r: 255,
                g: 255,
                b: 255,
            })).toEqual(true);
        });
        it('Возвращает false для красного', () => {
            expect(isLight({
                r: 255,
                g: 0,
                b: 0,
            })).toEqual(false);
        });
    });
    describe('isWhite', () => {
        it('возвращает true для белого', () => {
            expect(isWhite({ r: 255, g: 255, b: 255 }))
                .toEqual(true);
        });

        it('возвращает false для небелых цветов', () => {
            expect(isWhite({ r: 255, g: 255, b: 0 }))
                .toEqual(false);
            expect(isWhite({ r: 255, g: 0, b: 255 }))
                .toEqual(false);
            expect(isWhite({ r: 0, g: 255, b: 255 }))
                .toEqual(false);
            expect(isWhite({ r: 254, g: 254, b: 254 }))
                .toEqual(false);
            expect(isWhite({ r: 0, g: 0, b: 0 }))
                .toEqual(false);
        });
    });
});
