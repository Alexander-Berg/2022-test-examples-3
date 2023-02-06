const request = require('supertest');
const nock = require('nock');

require('chai').should();
const middlewareMock = require('./helper/middleware-mock');

const config = require('yandex-cfg');
const blogId = config.blogs.news.default;

describe('News page', () => {
    let app;

    function getCurrentDate() {
        const currentDate = new Date();

        return {
            currentDate,
            currentYear: currentDate.getFullYear(),
            currentMonth: currentDate.getMonth() + 1
        };
    }

    beforeEach(() => {
        middlewareMock.integrationBefore();
        app = require('../server/app');
    });

    it('should response 200 and return blogs, posts, tags', done => {
        const date = new Date(Date.UTC(2015, 10, 10, 0, 0, 0)).toString();

        nock(config.blogs.host)
            .get(`/tags/all/${blogId}`)
            .query({
                size: 20,
                lang: 'ru-RU'
            })
            .times(Infinity)
            .reply(200, [
                {
                    displayName: 'first tag',
                    slug: 'tag'
                },
                {
                    displayName: 'second tag',
                    slug: 'tag2'
                }
            ]);

        nock(config.blogs.host)
            .get(`/posts/${blogId}`)
            .query({
                size: 20,
                lang: 'ru-RU'
            })
            .times(Infinity)
            .reply(200, [
                {
                    slug: 'first-post',
                    someFields: 'someFields',
                    publishDate: date,
                    approvedTitle: 'approvedTitle'
                }, {
                    slug: 'second-post',
                    someFields: 'someFields',
                    publishDate: date,
                    approvedTitle: 'approvedTitle'
                }
            ]);

        nock(config.blogs.host)
            .get(`/${blogId}`)
            .query({
                size: 20,
                lang: 'ru-RU'
            })
            .times(Infinity)
            .reply(200, {
                postsCount: {
                    2016: {
                        Jan: 1,
                        Feb: 3
                    }
                },
                someField: 'field',
                slug: 'slug',
                localeTimezone: 'date',
                socialLinks: 'links'
            });

        request(app)
            .get('/adv/news')
            .set('host', 'adv.yandex.ru')
            .expect(200)
            .end((err, data) => {
                const { body } = data;
                const expectedBlog = {
                    slug: 'slug',
                    postsCount: {
                        2016: {
                            Jan: 1,
                            Feb: 3
                        }
                    },
                    localeTimezone: 'date',
                    socialLinks: 'links',
                    calendar: {
                        MONTH_DICTIONARY: [
                            'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
                        ],
                        start: {
                            year: 2016,
                            month: 1
                        },
                        current: {
                            year: getCurrentDate().currentYear,
                            month: getCurrentDate().currentMonth
                        },
                        checked: {
                            year: getCurrentDate().currentYear,
                            month: null
                        }
                    },
                    blogUrl: '/adv/news'
                };

                body.blog.should.deep.equal(expectedBlog);
                const expectedPosts = [
                    {
                        slug: 'first-post',
                        publishDate: date,
                        approvedTitle: 'approvedTitle',
                        url: '/adv/news/first-post',
                        date: '2015-11-10T03:00:00+03:00'
                    },
                    {
                        slug: 'second-post',
                        publishDate: date,
                        approvedTitle: 'approvedTitle',
                        url: '/adv/news/second-post',
                        date: '2015-11-10T03:00:00+03:00'
                    }
                ];

                body.posts.should.deep.equal(expectedPosts);

                const expectedTags = [
                    {
                        displayName: 'first tag',
                        slug: 'tag',
                        url: '?tag=tag'
                    },
                    {
                        displayName: 'second tag',
                        slug: 'tag2',
                        url: '?tag=tag2'
                    }
                ];

                body.tags.should.deep.equal(expectedTags);

                done(err);
            });
    });

    it('should return tag', done => {
        nock(config.blogs.host)
            .get(`/tags/all/${blogId}`)
            .query({
                size: 20,
                tag: 'tagSlug',
                lang: 'ru-RU'
            })
            .times(Infinity)
            .reply(200, []);

        nock(config.blogs.host)
            .get(`/tag/${blogId}/tagSlug`)
            .query({
                size: 20,
                tag: 'tagSlug',
                lang: 'ru-RU'
            })
            .times(Infinity)
            .reply(200, {
                displayName: 'first tag',
                slug: 'tagSlug'
            });

        nock(config.blogs.host)
            .get(`/posts/${blogId}`)
            .query({
                size: 20,
                tag: 'tagSlug',
                lang: 'ru-RU'
            })
            .times(Infinity)
            .reply(200, []);

        nock(config.blogs.host)
            .get(`/${blogId}`)
            .query({
                size: 20,
                tag: 'tagSlug',
                lang: 'ru-RU'
            })
            .times(Infinity)
            .reply(200, {
                postsCount: {
                    2016: {
                        Jan: 1,
                        Feb: 3
                    }
                },
                someField: 'field',
                slug: 'slug',
                localeTimezone: 'date',
                socialLinks: 'links'
            });

        request(app)
            .get('/adv/news')
            .set('host', 'adv.yandex.ru')
            .query({ tag: 'tagSlug' })
            .expect(200)
            .end((err, data) => {
                const expectedTag = {
                    displayName: 'first tag',
                    slug: 'tagSlug'
                };

                data.body.tag.should.deep.equal(expectedTag);

                done(err);
            });
    });

    it('should response 200 when api is unreachable', done => {
        nock(config.blogs.host)
            .get(`/tags/all/${blogId}`)
            .query({
                tag: 'tagSlug',
                size: 20,
                lang: 'ru-RU'
            })
            .times(Infinity)
            .reply(500, []);

        nock(config.blogs.host)
            .get(`/posts/${blogId}`)
            .query({
                tag: 'tagSlug',
                size: 20,
                lang: 'ru-RU'
            })
            .times(Infinity)
            .reply(500, []);

        nock(config.blogs.host)
            .get(`/${blogId}`)
            .query({
                tag: 'tagSlug',
                size: 20,
                lang: 'ru-RU'
            })
            .times(Infinity)
            .reply(500, []);

        nock(config.blogs.host)
            .get(`/tag/${blogId}/tagSlug`)
            .query({
                tag: 'tagSlug',
                size: 20,
                lang: 'ru-RU'
            })
            .times(Infinity)
            .reply(500, []);

        request(app)
            .get('/adv/news')
            .set('host', 'adv.yandex.ru')
            .query({ tag: 'tagSlug' })
            .expect(200, done);
    });

    afterEach(() => {
        middlewareMock.integrationAfter();
        nock.cleanAll();
    });
});
