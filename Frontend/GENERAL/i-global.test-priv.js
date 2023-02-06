const Module = require('module');

describeBlock('i-global', function() {
    it('clear Module._pathCache in renderer.js', function() {
        assert.isObject(Module._pathCache);
        const cacheSize = Object.keys(Module._pathCache).length;
        assert.isTrue(cacheSize > 100, `Module._pathCache should have at least 100 entries, but has ${cacheSize}`);
    });
});
