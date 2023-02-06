const assert = require('assert');
const fs = require('fs');
const nock = require('nock');
const config = require('yandex-cfg');
const catchError = require('catch-error-async');

const Avatars = require('lib/avatars');
const streamToBuffer = require('lib/streamToBuffer');

const { nockUploadAvatar, nockDeleteAvatar, nockUploadAvatarByUrl } = require('tests/mocks');

describe('Avatars library', () => {
    afterEach(() => nock.cleanAll());

    describe('getImageSrc', () => {
        it('should return all available image sources', async() => {
            const actual = await Avatars.getImageSrc('obi-wan-kenobi');

            assert.deepEqual(actual, {
                '286x137x2': 'https://avatars.mdst.yandex.net/get-events/obi-wan-kenobi/286x137x2',
                '130x130_c': 'https://avatars.mdst.yandex.net/get-events/obi-wan-kenobi/130x130_c',
                '130x130_f': 'https://avatars.mdst.yandex.net/get-events/obi-wan-kenobi/130x130_f',
                '80x80_c': 'https://avatars.mdst.yandex.net/get-events/obi-wan-kenobi/80x80_c',
                '80x80_f': 'https://avatars.mdst.yandex.net/get-events/obi-wan-kenobi/80x80_f',
                orig: 'https://avatars.mdst.yandex.net/get-events/obi-wan-kenobi/orig',
                preview: 'https://avatars.mdst.yandex.net/get-events/obi-wan-kenobi/preview',
            });
        });
    });

    describe('uploadImage', () => {
        it('should upload image to avatars service', async() => {
            nockUploadAvatar();

            const imageStream = fs.createReadStream('tests/data/shot_1296215782.png');
            const buffer = await streamToBuffer(imageStream);

            const response = await Avatars.uploadImage(buffer);

            assert.deepEqual(response, {
                'group-id': 1313,
                imagename: 12,
                sizes: {
                    preview: { height: 32, width: 512 },
                    orig: { height: 640, width: 1024 },
                },
            });
        });

        it('should error on fail to put image', async() => {
            nock(config.avatars.uploadHost)
                .post(/^\/put-.+/)
                .times(config.avatars.maxUploadRetries + 1)
                .reply(401, {});

            const buffer = Buffer.from('');
            const error = await catchError(Avatars.uploadImage.bind(Avatars), buffer);

            assert.equal(error.message, 'Failed to upload image');
            assert.equal(error.status, 400);
            assert.deepEqual(error.options, {
                internalCode: '400_FUI',
                tryCount: config.avatars.maxUploadRetries,
            });
        });
    });

    describe('deleteImage', () => {
        it('should delete image from avatars service', async() => {
            const nockInstance = nockDeleteAvatar();

            const imageId = '123/123344';

            await Avatars.deleteImage(imageId);

            assert.ok(nockInstance.isDone());
        });
    });

    describe('uploadImageByUrl', () => {
        it('should upload image to avatars by url', async() => {
            const imageUrl = 'https://han-solo.com/preview.jpg';
            const nockInstance = nockUploadAvatarByUrl({ imageUrl });

            await Avatars.uploadImageByUrl(imageUrl);

            assert.ok(nockInstance.isDone());
        });
    });
});
