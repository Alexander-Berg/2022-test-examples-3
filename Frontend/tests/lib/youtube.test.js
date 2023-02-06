const assert = require('assert');
const nock = require('nock');
const config = require('yandex-cfg');
const catchError = require('catch-error-async');

const Youtube = require('lib/youtube');

const { nockYoutubeApi } = require('tests/mocks');

describe('Youtube library', () => {
    afterEach(() => nock.cleanAll());

    describe('getIdByUrl', () => {
        it('should parse id from url in format "youtube.com/oembed/{id}"', () => {
            const actual = Youtube.getIdByUrl('https://www.youtube.com/embed/zB4I68XVPzQ');
            const expected = 'zB4I68XVPzQ';

            assert.equal(actual, expected);
        });

        it('should parse id from url in format "youtu.be/{id}"', () => {
            const actual = Youtube.getIdByUrl('https://youtu.be/zB4I68XVPzQ');
            const expected = 'zB4I68XVPzQ';

            assert.equal(actual, expected);
        });

        it('should parse id from url in format "youtube.com/watch?v={id}"', () => {
            const actual = Youtube.getIdByUrl('https://www.youtube.com/watch?v=zB4I68XVPzQ');
            const expected = 'zB4I68XVPzQ';

            assert.equal(actual, expected);
        });

        it('should return null if id is not parsed"', () => {
            const yandexVideoUrl = 'https://video.yandex.ru/iframe/ya-events/m-69601/';
            const actual = Youtube.getIdByUrl(yandexVideoUrl);

            assert.equal(actual, null);
        });
    });

    describe('_parseDuration', () => {
        it('should format iso 8601 to seconds', () => {
            assert.equal(Youtube._parseDuration('PT2M13S'), 133);
            assert.equal(Youtube._parseDuration('PT30M13S'), 1813);
            assert.equal(Youtube._parseDuration('PT1H5M13S'), 3913);
        });
    });

    describe('_getThumbnailWithBestQuality', () => {
        it('should return thumbnails with best quality', () => {
            const thumbnails = {
                default: {
                    url: 'https://i.ytimg.com/vi/zB4I68XVPzQ/default.jpg',
                    width: 120,
                    height: 90,
                },
                medium: {
                    url: 'https://i.ytimg.com/vi/zB4I68XVPzQ/mqdefault.jpg',
                    width: 320,
                    height: 180,
                },
            };
            const actual = Youtube._getThumbnailWithBestQuality(thumbnails);
            const expected = thumbnails.medium;

            assert.deepEqual(actual, expected);
        });
    });

    describe('getMetadata', () => {
        it('should get video metadata from url', async() => {
            nockYoutubeApi({ videoId: 'zB4I68XVPzQ', title: '123' });

            const videoUrl = 'https://youtu.be/zB4I68XVPzQ';
            const actual = await Youtube.getMetadata(videoUrl);
            const expected = {
                source: 'youtube',
                iframeUrl: 'https://www.youtube.com/embed/zB4I68XVPzQ',
                videoUrl,
                videoId: 'zB4I68XVPzQ',
                title: '123',
                duration: 92,
                definition: 'hd',
                thumbnail: 'https://i.ytimg.com/vi/zB4I68XVPzQ/hqdefault.jpg',
                thumbnailHeight: 360,
                thumbnailWidth: 480,
            };

            assert.deepEqual(actual, expected);
        });

        it('should throw error if url is invalid', async() => {
            nockYoutubeApi();

            const videoUrl = 'https://video.yandex.ru/iframe/ya-events/m-69601/';
            const error = await catchError(Youtube.getMetadata.bind(Youtube), videoUrl);

            assert.equal(error.message, 'Video url is not valid');
            assert.equal(error.status, 400);
            assert.deepEqual(error.options, {
                internalCode: '400_VNV',
                videoUrl,
            });
        });

        it('should throw error if video is not found', async() => {
            nock(config.youtube.api)
                .get(/.*/)
                .query(() => true)
                .reply(404, {});

            const videoUrl = 'https://youtu.be/zB4I68XVPzQ';
            const error = await catchError(Youtube.getMetadata.bind(Youtube), videoUrl);

            assert.equal(error.message, 'Video is not found');
            assert.equal(error.status, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_VNF',
                videoUrl,
            });
        });
    });
});
