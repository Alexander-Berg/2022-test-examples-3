const fs = require('fs');

const Reader = require('../unistat/lib/fs-reader');

jest.mock('fs');

describe('fs-reader', () => {
    beforeEach(() => {
        jest.resetAllMocks();
    });

    test('Считывает размер файла при инициализации', async() => {
        fs.stat.mockImplementation((path, cb) => cb(null, { size: 1024 }));

        const reader = new Reader();

        await reader.init('/path/to/log');

        expect(fs.stat).toBeCalledWith('/path/to/log', expect.any(Function));
        expect(reader.position).toEqual(1024);

        return Promise.resolve();
    });

    test('Корректно считывает строки, если размер буфера больше размера файла', async() => {
        fs.stat.mockImplementation((path, cb) => cb(null, { size: 7 }));
        fs.open.mockImplementation((path, flag, cb) => cb(null, 3));
        // eslint-disable-next-line max-params
        fs.read.mockImplementation((fd, buf, bufOffset, length, position, cb) =>
            cb(null, { buffer: Buffer.from('string') }));

        const reader = new Reader();

        await reader.init('/path/to/log');
        const line = await reader.readLine();

        expect(line).toEqual('string');
        expect(fs.read).toBeCalledWith(3, expect.any(Buffer), 0, 7, 0, expect.any(Function));
    });

    test('Корректно считывает строки, если размер буфера меньше размера файла', async() => {
        fs.stat.mockImplementation((path, cb) => cb(null, { size: 2048 }));
        fs.open.mockImplementation((path, flag, cb) => cb(null, 3));
        // eslint-disable-next-line max-params
        fs.read.mockImplementation((fd, buf, bufOffset, length, position, cb) =>
            cb(null, { buffer: Buffer.from('string\n') }));

        const reader = new Reader({ bufferSize: 1024 });

        await reader.init('/path/to/log');

        expect(await reader.readLine()).toEqual('string');
        expect(fs.read).toBeCalledWith(3, expect.any(Buffer), 0, 1024, 1024, expect.any(Function));
        expect(await reader.readLine()).toEqual('string');
        expect(fs.read).toBeCalledWith(3, expect.any(Buffer), 0, 1024, 0, expect.any(Function));
    });

    test('Не обращается к файловой системе, если есть уже прочитаные строки', async() => {
        fs.stat.mockImplementation((path, cb) => cb(null, { size: 2048 }));
        fs.open.mockImplementation((path, flag, cb) => cb(null, 3));
        // eslint-disable-next-line max-params
        fs.read.mockImplementation((fd, buf, bufOffset, length, position, cb) =>
            cb(null, { buffer: Buffer.from('string3\nstring2\nstring1\n') }));

        const reader = new Reader();

        await reader.init('/path/to/log');

        expect(await reader.readLine()).toEqual('string1');
        expect(await reader.readLine()).toEqual('string2');
        expect(await reader.readLine()).toEqual('string3');
        expect(fs.read).toBeCalledTimes(1);
    });

    test('Закрывает файл при вызове метода done', async() => {
        fs.stat.mockImplementation((path, cb) => cb(null, { size: 2048 }));
        fs.open.mockImplementation((path, flag, cb) => cb(null, 3));
        // eslint-disable-next-line max-params
        fs.read.mockImplementation((fd, buf, bufOffset, length, position, cb) =>
            cb(null, { buffer: Buffer.from('string3\nstring2\nstring1') }));
        fs.close.mockImplementation((fd, cb) => cb(null));

        const reader = new Reader();

        await reader.init('/path/to/log');
        // файл еще не открывался, этого не происходит при инициализации
        await reader.done();
        await reader.readLine();
        await reader.done();
        expect(fs.close).toBeCalled();
    });

    test('Возврашает null, если больше нет контента', async() => {
        fs.stat.mockImplementation((path, cb) => cb(null, { size: 64 * 1024 }));
        fs.open.mockImplementation((path, flag, cb) => cb(null, 3));
        // eslint-disable-next-line max-params
        fs.read.mockImplementation((fd, buf, bufOffset, length, position, cb) =>
            cb(null, { buffer: Buffer.from('string\n') }));

        const reader = new Reader();

        await reader.init('/path/to/log');
        await reader.readLine();
        const line = await reader.readLine();

        expect(line).toBeNull();
    });

    test('Возврашает null, если файл пустой', async() => {
        fs.stat.mockImplementation((path, cb) => cb(null, { size: 0 }));
        fs.open.mockImplementation((path, flag, cb) => cb(null, 3));

        const reader = new Reader();

        await reader.init('/path/to/log');
        const line = await reader.readLine();

        expect(line).toBeNull();
    });

    test('Сохраняет перенос строки, когда перенос крайний слева символ', async() => {
        fs.stat.mockImplementation((path, cb) => cb(null, { size: 2 * 1024 }));
        fs.open.mockImplementation((path, flag, cb) => cb(null, 3));
        // eslint-disable-next-line max-params
        fs.read.mockImplementation((fd, buf, bufOffset, length, position, cb) =>
            cb(null, { buffer: Buffer.from('\nsome part\nanother part\n') }));

        const reader = new Reader({ bufferSize: 1024 });

        await reader.init('/path/to/log');

        expect(await reader.readLine()).toEqual('another part');
        expect(await reader.readLine()).toEqual('some part');
        expect(await reader.readLine()).toEqual('another part');
        expect(await reader.readLine()).toEqual('some part');
    });

    test('Корректно считывает строку, если размер буфера меньше размера строки', async() => {
        const stub = ['ng\n', 'tri', 's'];
        let i = 0;
        fs.stat.mockImplementation((path, cb) => cb(null, { size: 7 }));
        fs.open.mockImplementation((path, flag, cb) => cb(null, 3));
        // eslint-disable-next-line max-params
        fs.read.mockImplementation((fd, buf, bufOffset, length, position, cb) =>
            cb(null, { buffer: Buffer.from(stub[i++]) }));

        const reader = new Reader({ bufferSize: 3 });

        await reader.init('/path/to/log');

        expect(await reader.readLine()).toEqual('string');
        expect(fs.read).toBeCalledTimes(3);
    });
});
