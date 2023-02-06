const assert = require('assert');
const request = require('supertest');
const nock = require('nock');
const config = require('yandex-cfg');
const _ = require('lodash');

const db = require('db');
const app = require('app');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const {
    nockBlackbox,
    nockTvmtool,
    nockUploadAvatar,
    nockUploadAvatarByUrl,
    nockDeleteAvatar,
} = require('tests/mocks');

const imageUrl = 'https://han-solo.com/preview.jpg';

describe('Admin images routes', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('GET /admin/images', () => {
        it('should return images with default pagination', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            const imagesData = _.range(1, 26)
                .map(num => ({ imageId: `${num}`, name: `image-${num}.jpg` }));

            await factory.image.create(imagesData);

            await request(app.listen())
                .get('/v1/admin/images')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 25);
                    assert.equal(body.meta.pageNumber, config.pagination.pageNumber);
                    assert.equal(body.meta.pageSize, config.pagination.pageSize);
                    assert.equal(body.rows.length, 20);
                    assert.equal(body.rows[0].name, 'image-25.jpg');
                    assert.equal(body.rows[0].src,
                        'https://avatars.mdst.yandex.net/get-events/25/orig');
                    assert.equal(body.rows[0].previewSrc,
                        'https://avatars.mdst.yandex.net/get-events/25/preview');
                });
        });

        it('should return images by pageSize and pageNumber', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            const imagesData = _.range(1, 26)
                .map(num => ({ imageId: `${num}`, name: `image-${num}.jpg` }));

            await factory.image.create(imagesData);

            await request(app.listen())
                .get('/v1/admin/images')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 3, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 25);
                    assert.equal(body.meta.pageNumber, 3);
                    assert.equal(body.meta.pageSize, 3);
                    assert.equal(body.rows.length, 3);
                    assert.equal(body.rows[0].name, 'image-19.jpg');
                    assert.equal(body.rows[0].src,
                        'https://avatars.mdst.yandex.net/get-events/19/orig');
                    assert.equal(body.rows[0].previewSrc,
                        'https://avatars.mdst.yandex.net/get-events/19/preview');
                });
        });

        it('should throw error if pageNumber is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            await request(app.listen())
                .get('/v1/admin/images')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 'inv@lid' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_PII',
                    message: 'Page number is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('POST /admin/images', () => {
        it('should upload and save image', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            nockUploadAvatar();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            await request(app.listen())
                .post('/v1/admin/images')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .attach('image', 'tests/data/shot_1296215782.png')
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.name, 'shot_1296215782.png');
                    assert.equal(body.id, '1313/12');
                    assert.equal(body.src,
                        'https://avatars.mdst.yandex.net/get-events/1313/12/orig');
                    assert.equal(body.previewSrc,
                        'https://avatars.mdst.yandex.net/get-events/1313/12/preview');
                });
        });

        it('should throw error if file is not passed', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            await request(app.listen())
                .post('/v1/admin/images')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_FII',
                    message: 'File is invalid',
                });
        });
    });

    describe('GET /admin/images/by-url', () => {
        it('should upload and save image by url', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            nockUploadAvatarByUrl({ imageUrl });
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            await request(app.listen())
                .get('/v1/admin/images/by-url')
                .query({ url: imageUrl })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.name, '12');
                    assert.equal(body.id, '1313/12');
                    assert.equal(body.src,
                        'https://avatars.mdst.yandex.net/get-events/1313/12/orig');
                    assert.equal(body.previewSrc,
                        'https://avatars.mdst.yandex.net/get-events/1313/12/preview');
                });
        });

        it('should throw error if url is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            await request(app.listen())
                .get('/v1/admin/images/by-url')
                .query({ url: 'invalid-url' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Image url is invalid',
                });
        });
    });

    describe('DELETE /admin/images/:groupId/:imageId', () => {
        it('should delete image', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            nockDeleteAvatar();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });
            await factory.image.create({ imageId: '123/123' });

            await request(app.listen())
                .delete('/v1/admin/images/123/123')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204);

            const actual = await db.image.findAll();

            assert.equal(actual.length, 0);
        });

        it('should throw error if group id is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            await request(app.listen())
                .delete('/v1/admin/images/inv@lid/123')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.internalCode, '400_III');
                    assert.equal(body.message, 'Group id is invalid');
                });
        });

        it('should throw error if image id is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            await request(app.listen())
                .delete('/v1/admin/images/123/inv@lid;')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.internalCode, '400_III');
                    assert.equal(body.message, 'Image id is invalid');
                });
        });
    });
});
