import {parseGidFromPath, stringIsGid} from '../parseGidFromPath';

declare const it: jest.It;

const validGids = [
    'root@1',
    'root@0000011111111',
    'x@0000011111111',
    'x@0',
    'asdasdasdasdhasdjhaskjdhjashdsakjhdasjhdjashdkasjhdaksjhdkjashdakjshdkasjhdakjshdajkshdkajhsdkjasdjkasdhaksjdhakjsdh@1123123123',
    'aASDASDJANSDNASJKDNASKJDNASKDJNSAJKDNASJKDNAJKSNDAJKSNDASJD@1123123123',
];

const invalidGids = [
    'foo',
    '002130131203012930123',
    '1',
    'a',
    'Foo@123213',
    'Foo@',
    '@123',
    'FFFFFFFF@123',
    '_=1-3121:@123',
    '!askdas@123',
    '123@metaClass',
];

const pathsWithGid = [
    ['root@1', '/root@1'],
    ['root@1', '/entity/root@1'],
    ['root@1', '/entity/root@1/edit'],
    ['root@1', '/entity/root@1/preview'],
    ['root@3', '/root@1/root@2/root@3'],
    ['root@2220202', '/zxczxc1/root@2220202/zxczcsad/00@00'],
];

const pathsWithoutGid = ['/entity/edit', '/entity/view', '/', 'entity@/oper/123'];

describe('stringIsGid', () => {
    it.each(validGids)('"%s" is a valid gid', str => {
        expect(stringIsGid(str)).toBeTruthy();
    });

    it.each(invalidGids)("'%s' isn't a valid gid", str => {
        expect(stringIsGid(str)).toBeFalsy();
    });
});

describe('parseGidFromPath', () => {
    it.each(pathsWithGid)('Gid "%s" has been exctracted from path "%s"', (expected, path) => {
        expect(parseGidFromPath(path)).toBe(expected);
    });

    it.each(pathsWithoutGid)("Path '%s' doesn't contain some gid", path => {
        expect(parseGidFromPath(path)).toBe(null);
    });
});
