const browserUpdater = require('../services/browserUpdater');

it('avatars', () => {
    const data = browserUpdater();

    expect(data['script-src'].length).toEqual(2);
    expect(data['script-src'].includes('yastatic.net')).toBeTruthy();
    expect(data['script-src'].includes('browser-updater.yandex.net')).toBeTruthy();

    expect(data['frame-src'].length).toEqual(1);
    expect(data['frame-src'].includes('yastatic.net')).toBeTruthy();

    expect(data['child-src'].length).toEqual(1);
    expect(data['child-src'].includes('yastatic.net')).toBeTruthy();
});
