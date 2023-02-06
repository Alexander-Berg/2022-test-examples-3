const resizer = require('../services/resizer');

it('resizer', () => {
    const data = resizer();

    expect(data['img-src'].length).toEqual(1);
    expect(data['img-src'].includes('resize.yandex.net')).toBeTruthy();
});

it('resizer production', () => {
    const data = resizer('production');

    expect(data['img-src'].length).toEqual(1);
    expect(data['img-src'].includes('resize.yandex.net')).toBeTruthy();
});

it('resizer testing', () => {
    const data = resizer('testing');

    expect(data['img-src'].length).toEqual(1);
    expect(data['img-src'].includes('resize.rs.yandex.net')).toBeTruthy();
});
