import { tokenize } from 'lib/tokenize';

describe('tokenize', function () {
    it('empty string', function () {
        expect(tokenize('')).toMatchInlineSnapshot(`Array []`);
    });

    it('plain text', function () {
        expect(tokenize('Simple plain text here')).toMatchInlineSnapshot(`
Array [
  "Simple plain text here",
]
`);
    });

    it('text with single ref', function () {
        expect(tokenize('{only}')).toMatchInlineSnapshot(`
Array [
  Array [
    "only",
  ],
]
`);

        expect(tokenize('{start} long text here')).toMatchInlineSnapshot(`
Array [
  Array [
    "start",
  ],
  " long text here",
]
`);
        expect(tokenize('Text here {center} and long text here too')).toMatchInlineSnapshot(`
Array [
  "Text here ",
  Array [
    "center",
  ],
  " and long text here too",
]
`);
        expect(tokenize('Text {end}')).toMatchInlineSnapshot(`
Array [
  "Text ",
  Array [
    "end",
  ],
]
`);
    });

    it('text with multiple refs', function () {
        expect(tokenize('{start} text {center} and here long text too')).toMatchInlineSnapshot(`
Array [
  Array [
    "start",
  ],
  " text ",
  Array [
    "center",
  ],
  " and here long text too",
]
`);
        expect(tokenize('{start} text {center} and here long text with {end}')).toMatchInlineSnapshot(`
Array [
  Array [
    "start",
  ],
  " text ",
  Array [
    "center",
  ],
  " and here long text with ",
  Array [
    "end",
  ],
]
`);
        expect(tokenize('Refs {first}{second}{third} without gap')).toMatchInlineSnapshot(`
Array [
  "Refs ",
  Array [
    "first",
  ],
  Array [
    "second",
  ],
  Array [
    "third",
  ],
  " without gap",
]
`);
    });

    it('invalid text', function () {
        expect(tokenize('Empty ref {} and text here with second valid ref {end}')).toMatchInlineSnapshot(`
Array [
  "Empty ref {} and text here with second valid ref {end}",
]
`);
        expect(tokenize('Text with valid refs {foo} and invalid {open bracket {bar}')).toMatchInlineSnapshot(`
Array [
  "Text with valid refs {foo} and invalid {open bracket {bar}",
]
`);
        expect(tokenize('Text with valid refs {foo} and invalid close bracket} {bar}')).toMatchInlineSnapshot(`
Array [
  "Text with valid refs {foo} and invalid close bracket} {bar}",
]
`);
    });
});
