const share = require('../services/share');

it('Share to social', () => {
    const data = share();

    expect(data['style-src'].length).toEqual(1);
    expect(data['style-src'].includes('\'unsafe-inline\'')).toBeTruthy();

    expect(data['frame-src'].length).toEqual(1);
    expect(data['frame-src'].includes('yastatic.net')).toBeTruthy();

    expect(data['child-src'].length).toEqual(1);
    expect(data['child-src'].includes('yastatic.net')).toBeTruthy();
});
