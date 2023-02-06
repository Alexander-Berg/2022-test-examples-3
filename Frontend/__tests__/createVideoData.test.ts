import createVideoData from '../createVideoData';

describe('createVideoData', () => {
    it('Returns correct data for youtube.com', () => {
        expect(createVideoData('https://www.youtube.com/embed/6c2cErPF-IM?enablejsapi=1&amp;wmode=opaque')).toStrictEqual({
            html: '<iframe src=\"//www.youtube.com/embed/6c2cErPF-IM?enablejsapi=1&amp;amp;wmode=opaque\" frameborder=\"0\" scrolling=\"no\" allow=\"autoplay; fullscreen\"></iframe>',
        });
    });

    it('Returns correct data for youtu.be', () => {
        expect(createVideoData('https://www.youtube.com/embed/btLZwPtYAE4?enablejsapi=1&amp;wmode=opaque')).toStrictEqual({
            html: '<iframe src=\"//www.youtube.com/embed/btLZwPtYAE4?enablejsapi=1&amp;amp;wmode=opaque\" frameborder=\"0\" scrolling=\"no\" allow=\"autoplay; fullscreen\"></iframe>',
        });
    });

    it('Returns correct data for rutube.ru', () => {
        expect(createVideoData('https://rutube.ru/video/embed/44dcc97d2ec767c1597a2daabf393052/?autoStart=true&amp;wmode=opaque')).toStrictEqual({
            html: '<iframe src=\"//rutube.ru/video/embed/44dcc97d2ec767c1597a2daabf393052/?autoStart=true&amp;amp;wmode=opaque\" frameborder=\"0\" scrolling=\"no\" allow=\"autoplay; fullscreen\"></iframe>',
        });
    });

    it('Returns correct data for coub.com', () => {
        expect(createVideoData('https://coub.com/embed/1osbrv?autoplay=true')).toStrictEqual({
            html: '<iframe src="//coub.com/embed/1osbrv?autoplay=true" frameborder="0" scrolling="no" allow="autoplay; fullscreen"></iframe>',
        });
    });

    it('Returns url data for frontend.vh.yandex.ru/*', () => {
        expect(createVideoData('https://frontend.vh.yandex.ru/player/4f77fcfec9e86ad4b3d4559a917b686b')).toStrictEqual({
            url: '//frontend.vh.yandex.ru/player/4f77fcfec9e86ad4b3d4559a917b686b',
        });
    });
});
