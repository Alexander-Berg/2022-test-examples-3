'use strict';

const config = require('yandex-cfg');
const nock = require('nock');
const request = require('supertest');

const app = require('../server/app');
const blogIdentity = config.blogs.news.default;
const blogsHost = config.blogs.host;
const captchaHost = config.captcha.apiHost;
const postIdentity = 'test-post';
const testComments = [
    {
        _id: '1',
        author: { imageSrc: 'https://yapic.yandex.ru/get//islands-middle' },
        body: { source: 'test', html: '<p>test</p>' }
    }
];

describe('Comments', () => {
    it('should get comments', done => {
        nock(blogsHost)
            .get(`/comments/all/${blogIdentity}/${postIdentity}`)
            .reply(200, testComments);

        request(app)
            .get(`/vda/news/${blogIdentity}/${postIdentity}/comments`)
            .set('host', 'yandex.ru')
            .expect(200, testComments, done);
    });

    it('should add comment correctly', done => {
        nock(blogsHost)
            .post('/comment')
            .reply(200, testComments)
            .get('/user/activity')
            .reply(200, { activity: false });

        request(app)
            .post('/vda/news/comment/add')
            .set('host', 'yandex.ru')
            .send(testComments[0])
            .expect(200, [testComments], done);
    });

    it('should complain', done => {
        nock(blogsHost)
            .post('/complaint')
            .reply(200, testComments[0]);

        request(app)
            .post('/vda/news/comment/complain')
            .set('host', 'yandex.ru')
            .expect(200, testComments[0], done);
    });

    it('should create comment with captcha', done => {
        const captchaParams = { key: 'key', rep: 'rep' };

        nock(blogsHost)
            .post('/comment')
            .reply(200, testComments)
            .get('/user/activity')
            .reply(200, { activity: false })
            .delete('/user/activity')
            .reply(200);

        nock(`http://${captchaHost}`)
            .get('/check')
            .query(captchaParams)
            .reply(200, { body: '<image_check>ok</image_check>' });

        request(app)
            .post('/vda/news/comment/captcha')
            .set('host', 'yandex.ru')
            .send(captchaParams)
            .expect(200, [testComments], done);
    });

    it('should show captcha', done => {
        const expectedCaptcha = { src: 'src', key: 'key' };

        nock(`http://${captchaHost}`)
            .get('/generate')
            .query({ type: 'rus', https: 'any' })
            .reply(200, { body: `/url='src'>key</` });

        request(app)
            .get('/vda/captcha/new')
            .query({ language: 'ru' })
            .set('host', 'yandex.ru')
            .expect(200, expectedCaptcha, done);
    });

    afterEach(() => {
        nock.cleanAll();
    });
});
