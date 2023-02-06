describe('utils/colors', function() {
    var { toStr, toInv, toObj } = require('./colors');

    it('toObj', function() {
        var expected = { r: 1, g: 2, b: 3, a: 1 };
        var actual = toObj('rgb(1, 2, 3)');

        expect(actual).toEqual(expected);
    });

    it('toStr', function() {
        var expected = 'rgba(1, 2, 3, 1)';
        var actual = toStr({ r: 1, g: 2, b: 3, a: 1 });

        expect(actual).toEqual(expected);
    });

    it('toInv', function() {
        var expected = { r: 0, g: 0, b: 0, a: 0.5 };
        var actual = toInv({ r: 255, g: 129, b: 255, a: 0.5 });

        expect(actual).toEqual(expected);
    });
});
