import { ColorConverter } from '../colorConverter';

describe('ColorConverter', () => {
    describe('метод hexToHsl', () => {
        it('должен ожидаемо конвертировать цвет', () => {
            expect(ColorConverter.hexToHsl('#222425')).toMatchInlineSnapshot(`
                Object {
                  "h": 200,
                  "l": 13.9,
                  "s": 4.2,
                }
            `);
            expect(ColorConverter.hexToHsl('#F4E6DE')).toMatchInlineSnapshot(`
                Object {
                  "h": 22,
                  "l": 91.4,
                  "s": 50,
                }
            `);
            expect(ColorConverter.hexToHsl('#284944')).toMatchInlineSnapshot(`
                Object {
                  "h": 171,
                  "l": 22.2,
                  "s": 29.2,
                }
            `);
            expect(ColorConverter.hexToHsl('#E8EAEA')).toMatchInlineSnapshot(`
                Object {
                  "h": 180,
                  "l": 91.4,
                  "s": 4.5,
                }
            `);
            expect(ColorConverter.hexToHsl('#fff')).toMatchInlineSnapshot(`
                Object {
                  "h": 0,
                  "l": 100,
                  "s": 0,
                }
            `);
            expect(ColorConverter.hexToHsl('#000')).toMatchInlineSnapshot(`
                Object {
                  "h": 0,
                  "l": 0,
                  "s": 0,
                }
            `);
        });
    });
});
