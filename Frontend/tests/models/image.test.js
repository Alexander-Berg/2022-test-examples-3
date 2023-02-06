const assert = require('assert');
const _ = require('lodash');

const { DbType } = require('db/constants');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const Image = require('models/image');
const catchError = require('catch-error-async');

const testDbType = DbType.internal;

const {
    nockUploadAvatar,
    nockDeleteAvatar,
    nockUploadAvatarByUrl,
} = require('tests/mocks');

const imageUrl = 'https://han-solo.com/preview.jpg';

describe('Image model', () => {
    beforeEach(cleanDb);

    describe('findPage', () => {
        it('should find images with limit and offset', async() => {
            await factory.image.create({ imageId: '1234/1234', name: 'obi-wan-kenobi.jpg' });
            await factory.image.create({ imageId: '1234/1235', name: 'han-solo.jpg' });
            await factory.image.create({ imageId: '1234/1236', name: 'boba-fett.jpeg' });
            await factory.image.create({ imageId: '1234/1237', name: 'yoda.jpg' });

            const options = { pageSize: 3, pageNumber: 1, scope: 'page', dbType: testDbType };
            const actual = await Image.findPage(options);

            assert.equal(actual.rows.length, 3);
            assert.equal(actual.rows[0].name, 'yoda.jpg');
            assert.equal(actual.rows[1].name, 'boba-fett.jpeg');
            assert.equal(actual.rows[2].name, 'han-solo.jpg');

            assert.equal(actual.meta.totalSize, 4);
            assert.equal(actual.meta.pageNumber, 1);
            assert.equal(actual.meta.pageSize, 3);
        });
    });

    describe('saveImageFile', () => {
        it('should save image', async() => {
            nockUploadAvatar();

            await Image.saveImageFile({
                name: 'test.png',
                buffer: Buffer.alloc(256),
            }, testDbType);

            const actual = await db.image.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].name, 'test.png');
            assert(_.startsWith(actual[0].imageId, '1313/'));
        });

        it('should throw on bad group id from avatars', async() => {
            nockUploadAvatar({ groupId: 'inv@lid!' });

            const error = await catchError(Image.saveImageFile.bind(Image), {
                name: 'test.png',
                buffer: Buffer.alloc(256),
            }, testDbType);

            assert.equal(error.message, 'Avatars service group id is invalid');
            assert.equal(error.status, 400);
            assert.equal(error.options.internalCode, '400_III');
        });

        it('should throw on bad image id from avatars', async() => {
            nockUploadAvatar({ imageId: 'inv@lid!' });

            const error = await catchError(Image.saveImageFile.bind(Image), {
                name: 'test.png',
                buffer: Buffer.alloc(256),
            });

            assert.equal(error.message, 'Avatars service image id is invalid');
            assert.equal(error.status, 400);
            assert.equal(error.options.internalCode, '400_III');
        });
    });

    describe('saveImageByUrl', () => {
        it('should save image by url', async() => {
            nockUploadAvatarByUrl({ imageUrl });

            await Image.saveImageByUrl(imageUrl, testDbType);

            const actual = await db.image.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].name, '12');
            assert(_.startsWith(actual[0].imageId, '1313/'));
        });

        it('should throw on bad group id from avatars', async() => {
            nockUploadAvatarByUrl({ imageUrl, groupId: 'inv@lid!' });

            const error = await catchError(Image.saveImageByUrl.bind(Image), imageUrl, testDbType);

            assert.equal(error.message, 'Avatars service group id is invalid');
            assert.equal(error.status, 400);
            assert.equal(error.options.internalCode, '400_III');
        });

        it('should throw on bad image id from avatars', async() => {
            nockUploadAvatarByUrl({ imageUrl, imageId: 'inv@lid!' });

            const error = await catchError(Image.saveImageByUrl.bind(Image), imageUrl, testDbType);

            assert.equal(error.message, 'Avatars service image id is invalid');
            assert.equal(error.status, 400);
            assert.equal(error.options.internalCode, '400_III');
        });
    });

    describe('destroy', () => {
        it('should delete image', async() => {
            nockDeleteAvatar();

            await factory.image.create({ imageId: '123/12', name: 'test.png' });

            await Image.destroy('123/12', testDbType);

            const actual = await db.image.findAll();

            assert.equal(actual.length, 0);
        });

        it('should throw if image is not found', async() => {
            const error = await catchError(Image.destroy.bind(Image), '123/12', testDbType);

            assert.equal(error.message, 'Image not found');
            assert.equal(error.status, 404);
            assert.equal(error.options.internalCode, '404_ENF');
        });
    });
});
