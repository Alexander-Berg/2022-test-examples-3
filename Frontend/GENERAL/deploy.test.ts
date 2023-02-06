import fs from 'fs';
import path from 'path';
import { mocked } from 'ts-jest/utils';
import s3API from '@yandex-int/si.ci.s3-client';
import deploy from './deploy';
import { IUploadConfig } from './config';

jest.mock('@yandex-int/si.ci.s3-client');
const mockedS3 = mocked(s3API, true);

describe('Deploy', () => {
    // игнорируем линтинг чтобы не перечислять все методы клиента
    // @ts-ignore
    let s3Client: jest.Mocked<s3API.Client> = {};
    let options: IUploadConfig;

    const fixturePath = './build/static';
    const filename = path.join(fixturePath, 'test.css');
    // количество файлов для загрузки
    // фактически 1, но добавляются еще сжатые br и gz файлы
    const COUNT_FILES: number = 3;

    beforeAll(() => {
        process.env.YENV = 'production';

        try {
            fs.mkdirSync('./build');
            fs.mkdirSync(fixturePath);
        } catch (e) {
            //
        }
        fs.writeFileSync(filename, '.test {}');
        fs.writeFileSync(`${filename}.gz`, `fake gzipped version of ${filename}`);
        fs.writeFileSync(`${filename}.br`, `fake br version of ${filename}`);
    });
    afterAll(() => {
        [filename, `${filename}.gz`, `${filename}.br`].forEach(file => fs.unlinkSync(file));
    });

    beforeEach(() => {
        // имитируем существование объекта в S3
        s3Client.headObject = jest.fn();
        s3Client.headObject.mockReturnValue(Promise.resolve({}));

        s3Client.upload = jest.fn();
        s3Client.deleteObject = jest.fn();

        s3Client.upload.mockReturnValue(
            Promise.resolve({
                Location: 's3-result-location',
                ETag: 'ETAG',
                Bucket: 'test-bucket',
                Key: 'Key',
            }),
        );

        options = {
            bucket: 'test-bucket',
            useYastaticCdn: true,
            concurrency: 1,
            static: {
                path: fixturePath,
                sources: ['**/*'],
                target: '/test-service/v1.0.0-test',
                overwrite: false,
                throwOnOverwrite: false,
                options: {
                    meta: {
                        foo: 'bar',
                    },
                    expires: new Date(Date.now()),
                },
            },
            // freeze в тесте не используется
            freeze: {
                path: path.join('_', fixturePath),
                sources: ['**/*.css'],
                target: '/test-service/_/v1.0.0-test',
                overwrite: false,
                throwOnOverwrite: false,
            },
            s3: {
                usePublicUrl: false,
                accessKeyId: 'accessKey',
                secretAccessKey: 'secretAccessKey',
                endpoint: 's3endpoint',
            },
        };

        mockedS3.getS3Client.mockImplementation(() => s3Client);
    });

    afterEach(done => {
        // проблема в проектировании static-uploader
        // по завершению очередь не очищается это порождает сайд эффекты при запуске тестов
        setTimeout(done, 100);
    });

    it('default (file already exists on s3)', async() => {
        await deploy(options);
        expect(s3Client.headObject).toHaveBeenCalledTimes(COUNT_FILES);
        // overwrite = false, объекты не закачиваются повторно
        expect(s3Client.upload).toHaveBeenCalledTimes(0);
    });

    it('default (file doesn\'t exists on s3)', async() => {
        // имитируем отсутсвие объекта в S3
        s3Client.headObject.mockReturnValue(Promise.reject());

        await deploy(options);

        expect(s3Client.headObject).toHaveBeenCalledTimes(COUNT_FILES);
        expect(s3Client.upload).toHaveBeenCalledTimes(COUNT_FILES);
        expect(s3Client.upload.mock.calls[0][3]).toHaveProperty('meta', { foo: 'bar' });
    });

    // локально проходит, но падает в CI,
    // TODO: разобраться и расскипать
    // eslint-disable-next-line mocha/no-skipped-tests
    xit('overwrite exists', async() => {
        options.static.overwrite = true;

        await deploy(options);

        expect(s3Client.headObject).toHaveBeenCalledTimes(COUNT_FILES);
        expect(s3Client.upload).toHaveBeenCalledTimes(COUNT_FILES);
    });

    it('throw on overwrite', async() => {
        options.static.throwOnOverwrite = true;

        await deploy(options);
        // deploy не кидает ошибку(только выводит сообщение в консоль) если объект существует
        // но прерывает выполнение при throwOnOverwrite = true, поэтому проверка происходит один раз
        expect(s3Client.headObject).toHaveBeenCalledTimes(1);
        expect(s3Client.upload).toHaveBeenCalledTimes(0);
    });

    // локально проходит, но падает в CI,
    // TODO: разобратсья и расскипать
    // eslint-disable-next-line mocha/no-skipped-tests
    xit('overwrite priority is higher than throwOnOverwrite', async() => {
        options.static.overwrite = true;
        options.static.throwOnOverwrite = true;

        await deploy(options);

        expect(s3Client.headObject).toHaveBeenCalledTimes(COUNT_FILES);
        expect(s3Client.upload).toHaveBeenCalledTimes(COUNT_FILES);
    });
});
