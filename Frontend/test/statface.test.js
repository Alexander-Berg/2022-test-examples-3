const statface = require('../services/statface');

it('statface', () => {
    const data = statface();

    expect(data['script-src'].includes('yandex.%tld%')).toBeTruthy();
    expect(data['connect-src'].includes('yandex.%tld%')).toBeTruthy();
});
