const avatars = require('../services/avatars');

it('avatars', () => {
    const data = avatars();

    expect(data['img-src'].length).toEqual(1);
    expect(data['img-src'].includes('avatars.mds.yandex.net')).toBeTruthy();
});

it('avatars production', () => {
    const data = avatars('production');

    expect(data['img-src'].length).toEqual(1);
    expect(data['img-src'].includes('avatars.mds.yandex.net')).toBeTruthy();
});

it('avatars iting', () => {
    const data = avatars('iting');

    expect(data['img-src'].length).toEqual(1);
    expect(data['img-src'].includes('avatars.mdst.yandex.net')).toBeTruthy();
});
