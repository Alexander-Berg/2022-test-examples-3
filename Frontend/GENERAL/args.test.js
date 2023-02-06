describe('utils/args', function() {
    var args = require('./args');

    it('Should return args as array', function() {
        var expected = ['foo'];
        var actual = args({
            foo: 'foo'
        }, 'foo');

        expect(actual).toEqual(expected);
    });

    it('Should return filtered args', function() {
        var expected = ['f', 'o', 'o'];
        var actual = args({
            foo: ['f', 'o', '', 'o']
        }, 'foo');

        expect(actual).toEqual(expected);
    });

    it('Should return last arg', function() {
        var expected = 'o';
        var actual = args.arg({
            foo: ['f', 'o']
        }, 'foo');

        expect(actual).toEqual(expected);
    });
});
