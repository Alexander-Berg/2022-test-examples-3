import {makeDelta} from './makeDelta';

describe('makeDelta', () => {
    test('work as expected with default process function', () => {
        expect(
            makeDelta({
                origin: {a: 1},
                diff: {},
            }),
        ).toMatchInlineSnapshot(`
            Object {
              "delta": Object {
                "a": Array [
                  1,
                  0,
                  0,
                ],
              },
              "origin": Object {
                "a": 1,
              },
            }
        `);
        expect(
            makeDelta({
                origin: {a: JSON.stringify({a: 1})},
                diff: {},
            }),
        ).toMatchInlineSnapshot(`
            Object {
              "delta": Object {
                "a": Array [
                  Object {
                    "a": 1,
                  },
                  0,
                  0,
                ],
              },
              "origin": Object {
                "a": Object {
                  "a": 1,
                },
              },
            }
        `);
    });
    test('work as expected with overridden process function', () => {
        expect(
            makeDelta({
                origin: {a: 1},
                diff: {},
                processor: x => x,
            }),
        ).toMatchInlineSnapshot(`
            Object {
              "delta": Object {
                "a": Array [
                  1,
                  0,
                  0,
                ],
              },
              "origin": Object {
                "a": 1,
              },
            }
        `);
        expect(
            makeDelta({
                origin: {a: JSON.stringify({a: 1})},
                diff: {},
                processor: x => x,
            }),
        ).toMatchInlineSnapshot(`
            Object {
              "delta": Object {
                "a": Array [
                  "{\\"a\\":1}",
                  0,
                  0,
                ],
              },
              "origin": Object {
                "a": "{\\"a\\":1}",
              },
            }
        `);
    });
});
