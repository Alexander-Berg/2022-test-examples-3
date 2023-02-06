'use strict';

const {
    normalizeContact,
    normalizePopularContact
} = require('./aceventura.js');

test('normalizeContact', () => {
    expect(normalizeContact({ cid: 'cid', email: 'e@ma.il', name: 'name' })).toMatchSnapshot();
});

test('normalizePopularContact', () => {
    expect(normalizePopularContact({ cid: 'cid', email: 'e@ma.il', name: 'name' })).toMatchSnapshot();
});
