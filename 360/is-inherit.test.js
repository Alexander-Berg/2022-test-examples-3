'use strict';

const isInherit = require('./is-inherit.js');

test('должен вернуть false, если b не наследуется от a', function() {
    const a = class {};
    const b = class {};

    expect(isInherit(b, a)).toEqual(false);
});

test('должен вернуть true, если b наследуется от a', function() {
    const a = class {};
    const b = class extends a {};

    expect(isInherit(b, a)).toEqual(true);
});

test('должен вернуть true, если c наследуется от a', function() {
    const a = class {};
    const b = class extends a {};
    const c = class extends b {};

    expect(isInherit(c, a)).toEqual(true);
});

test('должен вернуть true, если b === a', function() {
    const a = class {};
    const b = a;

    expect(isInherit(b, a)).toEqual(true);
});
