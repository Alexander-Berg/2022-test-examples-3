const counters = require('../services/tns');

it('counters', () => {
    const data = counters();

    expect(data['img-src'].length).toEqual(1);
    expect(data['img-src'].includes('www.tns-counter.ru')).toBeTruthy();
});
