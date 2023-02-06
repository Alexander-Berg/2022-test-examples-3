import { vol } from 'memfs'
import { DefaultFileSystemImplementation } from '../../src/default-file-system-implementation'
import {
    CopyParameters,
    DeleteParameters,
    MakeDirectoryParameters,
    MoveParameters,
    ReadParameters,
    WriteParameters,
} from '../../src/xplat/common/code/api/file-system/file-system-implementation'
import { Encoding, HashType } from '../../src/xplat/common/code/api/file-system/file-system-types'
import { fromXPromise } from '../../src/xplat/common/ys/xpromise-support'
import { Int32 } from '../../src/xplat/common/ys/ys'

jest.mock('fs', () => require('memfs').fs)

// tslint:disable:no-inferred-empty-object-type

describe.skip(DefaultFileSystemImplementation, () => {
    const fileSystem = new DefaultFileSystemImplementation()

    beforeEach(() => {
        vol.reset()
    })

    describe('getItemInfo', () => {
        it('should get file and directory info', async () => {
            vol.fromJSON({ '/foo/bar.txt': 'bar' })
            await expect(fromXPromise(fileSystem.getItemInfo('/foo'))).resolves.toStrictEqual({
                isFile: false,
                path: '/foo',
                size: 0,
                mtime: expect.any(Number),
            })
            await expect(fromXPromise(fileSystem.getItemInfo('/foo/bar.txt'))).resolves.toStrictEqual({
                isFile: true,
                path: '/foo/bar.txt',
                size: 3,
                mtime: expect.any(Number),
            })
        })

        it('should reject for non-existing file and directory', async () => {
            await expect(fromXPromise(fileSystem.getItemInfo('/foo'))).rejects.toEqual(expect.any(String))
            await expect(fromXPromise(fileSystem.getItemInfo('/foo/bar.txt'))).rejects.toEqual(expect.any(String))
        })
    })

    describe('exists', () => {
        it('should check that file and directory exist', async () => {
            vol.fromJSON({ '/foo/bar.txt': 'bar' })
            await expect(fromXPromise(fileSystem.exists('/foo'))).resolves.toBe(true)
            await expect(fromXPromise(fileSystem.exists('/foo/bar.txt'))).resolves.toBe(true)
        })

        it('should check that file and directory do not exist', async () => {
            await expect(fromXPromise(fileSystem.exists('/foo'))).resolves.toBe(false)
            await expect(fromXPromise(fileSystem.exists('/foo/bar.txt'))).resolves.toBe(false)
        })
    })

    describe('listDirectory', () => {
        it('should list files in a directory', async () => {
            vol.fromJSON({ '/foo/bar.txt': 'bar', '/foo/baz.txt': 'baz' })
            const files = await fromXPromise(fileSystem.listDirectory('/foo'))
            expect(new Set(files)).toEqual(new Set(['/foo/bar.txt', '/foo/baz.txt']))
        })

        it('should list files in a directory with a trailing slash', async () => {
            vol.fromJSON({ '/foo/bar.txt': 'bar', '/foo/baz.txt': 'baz' })
            const files = await fromXPromise(fileSystem.listDirectory('/foo/'))
            expect(new Set(files)).toEqual(new Set(['/foo/bar.txt', '/foo/baz.txt']))
        })

        it('should reject for a file', async () => {
            vol.fromJSON({ '/foo.txt': 'foo.txt' })
            await expect(fromXPromise(fileSystem.exists('/foo.txt'))).resolves.toBe(true)
            await expect(fromXPromise(fileSystem.listDirectory('/foo.txt'))).rejects.toEqual(expect.any(String))
        })

        it('should reject for a non-existing directory', async () => {
            await expect(fromXPromise(fileSystem.exists('/foo'))).resolves.toBe(false)
            await expect(fromXPromise(fileSystem.listDirectory('/foo'))).rejects.toEqual(expect.any(String))
        })
    })

    describe('readAsStringWithParams', () => {
        beforeEach(() => {
            vol.fromJSON({ '/foo/bar.txt': 'foobar' })
        })

        function readString(encoding: Encoding, position?: Int32, length?: Int32) {
            return fromXPromise(fileSystem.readAsStringWithParams(
                '/foo/bar.txt',
                new ReadParameters(
                    encoding,
                    position !== undefined ? position : null,
                    length !== undefined ? length : null,
                ),
            ))
        }

        it('should read a string without offset and length', async () => {
            await expect(readString(Encoding.Utf8)).resolves.toBe('foobar')
            await expect(readString(Encoding.Base64)).resolves.toBe('Zm9vYmFy')
        })

        it('should read a string with offset and length', async () => {
            await expect(readString(Encoding.Utf8, 0, Number.POSITIVE_INFINITY)).resolves.toBe('foobar')
            await expect(readString(Encoding.Base64, 0, Number.POSITIVE_INFINITY)).resolves.toBe('Zm9vYmFy')
            await expect(readString(Encoding.Utf8, undefined, 5)).resolves.toBe('fooba')
            await expect(readString(Encoding.Base64, undefined, 5)).resolves.toBe('Zm9vYmE=')
            await expect(readString(Encoding.Utf8, 1, undefined)).resolves.toBe('oobar')
            await expect(readString(Encoding.Base64, 1, undefined)).resolves.toBe('b29iYXI=')
            await expect(readString(Encoding.Utf8, 1, 4)).resolves.toBe('ooba')
            await expect(readString(Encoding.Base64, 1, 4)).resolves.toBe('b29iYQ==')
        })

        it('should reject for a non-existing file', async () => {
            await expect(fromXPromise(fileSystem.readAsStringWithParams(
                '/foo/missing.txt',
                new ReadParameters(Encoding.Utf8, null, null),
            ))).rejects.toEqual(expect.any(String))
        })
    })

    describe('writeAsStringWithParams', () => {
        function readString(path: string, encoding: Encoding) {
            return fromXPromise(fileSystem.readAsStringWithParams(
                path,
                new ReadParameters(encoding, null, null),
            ))
        }

        function writeString(path: string, contents: string, encoding: Encoding, overwrite?: boolean) {
            return fromXPromise(fileSystem.writeAsStringWithParams(
                path,
                contents,
                new WriteParameters(encoding, overwrite || false),
            ))
        }

        it('should correctly write to file with different encodings', async () => {
            await writeString('/foo.txt', 'foobar', Encoding.Utf8)
            const foo = await readString('/foo.txt', Encoding.Utf8)
            expect(foo).toEqual('foobar')

            await writeString('/bar.txt', 'Zm9vYmFy', Encoding.Base64)
            const bar = await readString('/bar.txt', Encoding.Utf8)
            expect(bar).toEqual('foobar')
        })

        it('should respect "overwrite" param', async () => {
            await writeString('/foo.txt', 'foo', Encoding.Utf8)
            const foo = await readString('/foo.txt', Encoding.Utf8)
            expect(foo).toEqual('foo')

            await writeString('/foo.txt', 'foo_overwritten', Encoding.Utf8, true)
            const overwrittenFoo = await readString('/foo.txt', Encoding.Utf8)
            expect(overwrittenFoo).toEqual('foo_overwritten')

            await expect(writeString('/foo.txt', 'should fail', Encoding.Utf8)).rejects.toEqual(expect.any(String))
        })

        it('should fail with a directory path', async () => {
            vol.mkdirSync('/foo')
            await expect(writeString('/foo', 'should fail', Encoding.Utf8, true)).rejects.toEqual(expect.any(String))
        })
    })

    describe('deleteWithParams', () => {
        function remove(path: string, ignoreAbsence?: boolean) {
            return fromXPromise(fileSystem.deleteWithParams(
                path,
                new DeleteParameters(ignoreAbsence || false),
            ))
        }

        it('should delete files and directories', async () => {
            vol.fromJSON({ '/foo.txt': 'foo' })
            await expect(fromXPromise(fileSystem.exists('/foo.txt'))).resolves.toBe(true)
            await remove('/foo.txt')
            await expect(fromXPromise(fileSystem.exists('/foo.txt'))).resolves.toBe(false)

            vol.fromJSON({ '/foo/bar.txt': 'bar', '/foo/baz.txt': 'baz' })
            await expect(fromXPromise(fileSystem.exists('/foo'))).resolves.toBe(true)
            await remove('/foo')
            await expect(fromXPromise(fileSystem.exists('/foo'))).resolves.toBe(false)
        })

        it('should respect "ignoreAbsence" param', async () => {
            await expect(fromXPromise(fileSystem.exists('/foo.txt'))).resolves.toBe(false)
            await expect(remove('/foo.txt', true)).resolves
            await expect(remove('/foo.txt', false)).rejects.toEqual(expect.any(String))
        })
    })

    describe('moveWithParams', () => {
        function move(source: string, destination: string, createIntermediates?: boolean) {
            return fromXPromise(fileSystem.moveWithParams(
                source,
                destination,
                new MoveParameters(createIntermediates || false),
            ))
        }

        it('should move files', async () => {
            vol.fromJSON({ '/foo/bar.txt': 'bar' })
            await move('/foo/bar.txt', '/foo/bar_moved.txt')
            expect(vol.toJSON()).toEqual({ '/foo/bar_moved.txt': 'bar' })
        })

        it('should move directories', async () => {
            vol.fromJSON({ '/foo/bar.txt': 'bar' })
            await move('/foo', '/foo_moved')
            // unfortunately, "rename" is currently broken in "memfs" as it fails to properly link children
            // expect(vol.toJSON()).toEqual({'/foo_moved/bar.txt': 'bar'})
        })

        it('should respect "createIntermediates" param', async () => {
            vol.fromJSON({ '/foo/bar.txt': 'bar' })
            await move('/foo/bar.txt', '/foo/intermediate/bar.txt', true)
            expect(vol.toJSON()).toEqual({ '/foo/intermediate/bar.txt': 'bar' })
            await expect(move('/foo/intermediate/bar.txt', '/baz/intermediate/bar.txt', false)).rejects
                .toEqual(expect.any(String))
        })

        it('should fail if the destination already exists', async () => {
            vol.fromJSON({ '/foo/bar.txt': 'bar', '/foo/baz.txt': 'baz' })
            await expect(move('/foo/bar.txt', '/foo/baz.txt')).rejects.toEqual(expect.any(String))
        })
    })

    describe('copyWithParams', () => {
        function copy(source: string, destination: string, createIntermediates?: boolean) {
            return fromXPromise(fileSystem.copyWithParams(
                source,
                destination,
                new CopyParameters(createIntermediates || false),
            ))
        }

        it('should copy files', async () => {
            vol.fromJSON({ '/foo/bar.txt': 'bar' })
            await copy('/foo/bar.txt', '/foo/bar_copied.txt')
            expect(vol.toJSON()).toEqual({ '/foo/bar.txt': 'bar', '/foo/bar_copied.txt': 'bar' })
        })

        it('should copy directories', async () => {
            vol.fromJSON({ '/foo/bar.txt': 'bar' })
            await copy('/foo', '/foo_copied')
            expect(vol.toJSON()).toEqual({ '/foo/bar.txt': 'bar', '/foo_copied/bar.txt': 'bar' })
        })

        it('should respect "createIntermediates" param', async () => {
            vol.fromJSON({ '/foo/bar.txt': 'bar' })
            await copy('/foo/bar.txt', '/foo/intermediate/bar.txt', true)
            expect(vol.toJSON()).toEqual({ '/foo/bar.txt': 'bar', '/foo/intermediate/bar.txt': 'bar' })
            await expect(copy('/foo/intermediate/bar.txt', '/baz/intermediate/bar.txt', false)).rejects
                .toEqual(expect.any(String))
        })

        it('should fail if the destination already exists', async () => {
            vol.fromJSON({ '/foo/bar.txt': 'bar', '/foo/baz.txt': 'baz' })
            await expect(copy('/foo/bar.txt', '/foo/baz.txt')).rejects.toEqual(expect.any(String))
        })
    })

    describe('makeDirectoryWithParams', () => {
        function mkdir(path: string, createIntermediates?: boolean) {
            return fromXPromise(fileSystem.makeDirectoryWithParams(
                path,
                new MakeDirectoryParameters(createIntermediates || false),
            ))
        }

        it('should create directory', async () => {
            await mkdir('/foo')
            await expect(fromXPromise(fileSystem.exists('/foo'))).resolves.toBe(true)
        })

        it('should respect "createIntermediates" param', async () => {
            await mkdir('/foo/bar', true)
            await expect(fromXPromise(fileSystem.exists('/foo/bar'))).resolves.toBe(true)
            await expect(mkdir('/baz/bar')).rejects.toEqual(expect.any(String))
        })

        it('should fail if the destination already exists', async () => {
            await mkdir('/foo')
            await expect(fromXPromise(fileSystem.exists('/foo'))).resolves.toBe(true)
            await expect(mkdir('/foo')).rejects.toEqual(expect.any(String))
        })
    })

    describe('makeDirectoryWithParams', () => {
        it('should calculate hashes', async () => {
            vol.fromJSON({ '/foobar.txt': 'foobar' })
            await expect(fromXPromise(fileSystem.hash('/foobar.txt', HashType.Md5)))
                .resolves.toBe('3858f62230ac3c915f300c664312c63f')
            await expect(fromXPromise(fileSystem.hash('/foobar.txt', HashType.Sha256)))
                .resolves.toBe('c3ab8ff13720e8ad9047dd39466b3c8974e592c2fa383d4a3960714caef0c4f2')
            await expect(fromXPromise(fileSystem.hash('/foobar.txt', HashType.Sha512)))
                // tslint:disable-next-line:max-line-length
                .resolves.toBe('0a50261ebd1a390fed2bf326f2673c145582a6342d523204973d0219337f81616a8069b012587cf5635f6925f1b56c360230c19b273500ee013e030601bf2425')
        })

        it('should fail for a non-existing file', async () => {
            await expect(fromXPromise(fileSystem.hash('/foobar.txt', HashType.Md5))).rejects
                .toEqual(expect.any(String))
        })
    })
})
