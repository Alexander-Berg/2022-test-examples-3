const patchTitle = require.requireActual('../patchTitle').default;
const CHAR_NBSP = require.requireActual('../../stringUtils').CHAR_NBSP;

describe('patchTitle', () => {
    it('should replace spaced long dash with NoBreak space', () => {
        expect(
            patchTitle('Москва, автостанция Тёплый Стан — Сухиничи'),
        ).toEqual(`Москва, автостанция Тёплый Стан${CHAR_NBSP}— Сухиничи`);
    });

    it('should replace spaced short dash with NoBreak space and long dash', () => {
        expect(patchTitle('Москва (Киевский вокзал) - Калуга-1')).toEqual(
            `Москва (Киевский вокзал)${CHAR_NBSP}— Калуга-1`,
        );
    });

    it('should not change the input string', () => {
        expect(patchTitle('Москва- Черновцы')).toEqual('Москва- Черновцы');
    });

    it('should replace short word on the end of string with NoBreak space', () => {
        expect(patchTitle('Москва (Казанский вокзал) — 47 км')).toEqual(
            `Москва (Казанский вокзал)${CHAR_NBSP}— 47${CHAR_NBSP}км`,
        );

        expect(
            patchTitle('Москва (Казанский вокзал) — поворот на пятый км'),
        ).toEqual(
            `Москва (Казанский вокзал)${CHAR_NBSP}— поворот на пятый${CHAR_NBSP}км`,
        );
    });
});
