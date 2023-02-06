const s3 = require('../services/s3');

it('S3 production without bucket', () => {
    const data = s3(undefined, undefined, ['test']);

    expect(data.test.length).toEqual(2);
    expect(data.test.includes('s3.yandex.net')).toBeTruthy();
    expect(data.test.includes('s3.mds.yandex.net')).toBeTruthy();

    const data2 = s3('production', undefined, ['test']);

    expect(data2.test.length).toEqual(2);
    expect(data2.test.includes('s3.yandex.net')).toBeTruthy();
    expect(data2.test.includes('s3.mds.yandex.net')).toBeTruthy();
});

it('S3 production with bucket', () => {
    const data = s3(undefined, 'b', ['test']);

    expect(data.test.length).toEqual(4);
    expect(data.test.includes('s3.yandex.net')).toBeTruthy();
    expect(data.test.includes('s3.mds.yandex.net')).toBeTruthy();
    expect(data.test.includes('b.s3.yandex.net')).toBeTruthy();
    expect(data.test.includes('b.s3.mds.yandex.net')).toBeTruthy();
});

it('S3 testing without bucket', () => {
    const data = s3('testing', undefined, ['test']);

    expect(data.test.length).toEqual(1);
    expect(data.test.includes('s3.mdst.yandex.net')).toBeTruthy();
});

test('S3 testing with bucket', () => {
    const data = s3('testing', 'b', ['test']);

    expect(data.test.length).toEqual(2);
    expect(data.test.includes('s3.mdst.yandex.net')).toBeTruthy();
    expect(data.test.includes('b.s3.mdst.yandex.net')).toBeTruthy();
});
