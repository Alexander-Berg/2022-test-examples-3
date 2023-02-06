const iddqdSentry = require('../services/iddqdSentry');

it('sentry test', () => {
    const data = iddqdSentry('testuing');

    expect(data['script-src'].length).toEqual(1);
    expect(data['script-src'].includes('yastatic.net')).toBeTruthy();

    expect(data['connect-src'].length).toEqual(1);
    expect(data['connect-src'].includes('sentry-test-proxy.t.yandex.net')).toBeTruthy();
});

it('sentry prod', () => {
    const data = iddqdSentry('production');

    expect(data['script-src'].length).toEqual(1);
    expect(data['script-src'].includes('yastatic.net')).toBeTruthy();

    expect(data['connect-src'].length).toEqual(1);
    expect(data['connect-src'].includes('sentry.iddqd.yandex.net')).toBeTruthy();
});
