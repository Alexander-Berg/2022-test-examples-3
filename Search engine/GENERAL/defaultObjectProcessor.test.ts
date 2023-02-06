import {defaultObjectProcessor} from './defaultObjectProcessor';

describe('defaultObjectProcessor', () => {
    test('correctly process string value fields', () => {
        expect(defaultObjectProcessor({a: '1', b: 2, c: {a: 1}}))
            .toMatchInlineSnapshot(`
            Object {
              "a": 1,
              "b": 2,
              "c": Object {
                "a": 1,
              },
            }
        `);
    });
    test('correctly process string JSON value fields', () => {
        expect(
            defaultObjectProcessor({
                a: '1',
                c: JSON.stringify([2, '2']),
                b: JSON.stringify({b: 2}),
            }),
        ).toMatchInlineSnapshot(`
            Object {
              "a": 1,
              "b": Object {
                "b": 2,
              },
              "c": Array [
                2,
                "2",
              ],
            }
        `);
    });
});
