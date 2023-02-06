const appmetrika = require('../services/appmetrika');

it('appmetrika', () => {
    const data = appmetrika();

    expect(data['connect-src'].length).toEqual(4);
    expect(data['connect-src'].includes('127.0.0.1:29009')).toBeTruthy();
    expect(data['connect-src'].includes('127.0.0.1:30102')).toBeTruthy();
    expect(data['connect-src'].includes('yandexmetrika.com:29010')).toBeTruthy();
    expect(data['connect-src'].includes('yandexmetrika.com:30103')).toBeTruthy();
});

it('only by ip', () => {
    const data = appmetrika({ byIp: true, byDomain: false });

    expect(data['connect-src'].length).toEqual(2);
    expect(data['connect-src'].includes('127.0.0.1:29009')).toBeTruthy();
    expect(data['connect-src'].includes('127.0.0.1:30102')).toBeTruthy();
    expect(data['connect-src'].includes('yandexmetrika.com:29010')).toBeFalsy();
    expect(data['connect-src'].includes('yandexmetrika.com:30103')).toBeFalsy();
});

it('only by domain', () => {
    const data = appmetrika({ byIp: false, byDomain: true });

    expect(data['connect-src'].length).toEqual(2);
    expect(data['connect-src'].includes('127.0.0.1:29009')).toBeFalsy();
    expect(data['connect-src'].includes('127.0.0.1:30102')).toBeFalsy();
    expect(data['connect-src'].includes('yandexmetrika.com:29010')).toBeTruthy();
    expect(data['connect-src'].includes('yandexmetrika.com:30103')).toBeTruthy();
});
