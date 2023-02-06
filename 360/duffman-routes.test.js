'use strict';

test('coverage', () => {
    const routes = require('./duffman-routes/lite.js');
    expect(routes).toHaveProperty('routes');
});
