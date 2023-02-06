const request = require('supertest');
const nock = require('nock');

const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const app = require('app');

const { nockBlackbox, nockTvmtool, nockYoutubeApi } = require('tests/mocks');

describe('Admin video routes', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('POST /admin/video/parse', () => {
        it('should upload and save image', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            nockYoutubeApi();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            await request(app.listen())
                .post('/v1/admin/video/parse')
                .send({ videoUrl: 'https://youtu.be/zB4I68XVPzQ' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({
                    source: 'youtube',
                    iframeUrl: 'https://www.youtube.com/embed/zB4I68XVPzQ',
                    videoUrl: 'https://youtu.be/zB4I68XVPzQ',
                    videoId: 'zB4I68XVPzQ',
                    title: 'Star Wars: The Last Jedi Official Teaser',
                    duration: 92,
                    definition: 'hd',
                    thumbnail: 'https://i.ytimg.com/vi/zB4I68XVPzQ/hqdefault.jpg',
                    thumbnailHeight: 360,
                    thumbnailWidth: 480,
                });
        });

        it('should throw error if videoUrl is invalid', async() => {
            nockBlackbox('yoda');
            nockTvmtool();
            nockYoutubeApi();
            await factory.userRole.create({ login: 'yoda', role: 'admin' });

            await request(app.listen())
                .post('/v1/admin/video/parse')
                .send({ videoUrl: '123' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_VNV',
                    message: 'Video url is not valid',
                    videoUrl: '123',
                });
        });
    });
});
