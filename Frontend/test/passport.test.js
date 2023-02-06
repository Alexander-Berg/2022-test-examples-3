const passport = require('../services/passport');

it('passport', () => {
    const data = passport();

    expect(data['script-src'].length).toEqual(2);
    expect(data['script-src'].includes('pass.yandex.%tld%')).toBeTruthy();
    expect(data['script-src'].includes('social.yandex.%tld%')).toBeTruthy();

    expect(data['img-src'].length).toEqual(2);
    expect(data['img-src'].includes('passport.yandex.ru')).toBeTruthy();
    expect(data['img-src'].includes('passport.yandex.%tld%')).toBeTruthy();

    expect(data['connect-src'].length).toEqual(2);
    expect(data['connect-src'].includes('passport.yandex.ru')).toBeTruthy();
    expect(data['connect-src'].includes('passport.yandex.%tld%')).toBeTruthy();
});

it('passport testing', () => {
    const data = passport('testing');

    expect(data['script-src'].length).toEqual(2);
    expect(data['script-src'].includes('pass-test.yandex.%tld%')).toBeTruthy();
    expect(data['script-src'].includes('social-test.yandex.%tld%')).toBeTruthy();

    expect(data['img-src'].length).toEqual(2);
    expect(data['img-src'].includes('passport.yandex.ru')).toBeTruthy();
    expect(data['img-src'].includes('passport.yandex.%tld%')).toBeTruthy();

    expect(data['connect-src'].length).toEqual(2);
    expect(data['connect-src'].includes('passport-test.yandex.ru')).toBeTruthy();
    expect(data['connect-src'].includes('passport-test.yandex.%tld%')).toBeTruthy();
});

it('passport production kz', () => {
    const data = passport('production', 'kz');

    expect(data['script-src'].length).toEqual(2);
    expect(data['script-src'].includes('pass.yandex.kz')).toBeTruthy();
    expect(data['script-src'].includes('social.yandex.kz')).toBeTruthy();

    expect(data['img-src'].length).toEqual(2);
    expect(data['img-src'].includes('passport.yandex.ru')).toBeTruthy();
    expect(data['img-src'].includes('passport.yandex.kz')).toBeTruthy();

    expect(data['connect-src'].length).toEqual(2);
    expect(data['connect-src'].includes('passport.yandex.ru')).toBeTruthy();
    expect(data['connect-src'].includes('passport.yandex.kz')).toBeTruthy();
});
