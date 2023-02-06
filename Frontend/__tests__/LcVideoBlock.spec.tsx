import * as React from 'react';
import { mount, shallow } from 'enzyme';
import * as serializer from 'jest-serializer-html';
import { LcSizes } from '@yandex-turbo/components/lcTypes/lcTypes';
import { LcVideoBlock } from '../LcVideoBlock';
import { ILcVideo, VideoRatio, VideoType } from '../LcVideoBlock.types';
import * as LazyIframe from '../LazyIframe/LazyIframe';
import * as LazyVideo from '../LazyVideo/LazyVideo';

const defaultVideo: ILcVideo = {
    type: VideoType.Uploaded,
    player: {
        host: '//yastatic.net/yandex-video-player-iframe-api-bundles',
        version: '1.0-2305',
    },
    ratio: VideoRatio['16x9'],
    autoplay: true,
    loop: true,
    muted: false,
    showControls: true,
    mp4Url: 'https://streaming.video.yandex.ru/get/yndx-video/m-69033-1690643b9c3-c491e9a08b6caf39/480p.mp4',
    webmUrl: 'https://streaming.video.yandex.ru/get/yndx-video/m-69033-1690643b9c3-c491e9a08b6caf39/480p.webm',
};

expect.addSnapshotSerializer(serializer);

describe('LcVideoBlock section', () => {
    beforeAll(() => {
        // @ts-ignore
        global.IntersectionObserver = class {
            constructor() { }
            observe() { }
            disconnect() { }
        };
    });

    describe('should render video for uploaded media', () => {
        const originalLazyVideo = LazyVideo.LazyVideo;

        beforeAll(() => {
            // исключаем withVisibilityProvider
            // @ts-ignore
            LazyVideo.LazyVideo = props => <LazyVideo.LazyVideoComponent {...props} isVisible />;
        });

        test('with sources when visible', () => {
            const video: ILcVideo = {
                ...defaultVideo,
            };
            const component = shallow(<LcVideoBlock width={LcSizes.S} video={video} />);

            expect(component.html()).toMatchSnapshot();
        });

        test('without sources when not visible', () => {
            // @ts-ignore
            LazyVideo.LazyVideo = originalLazyVideo;

            const video: ILcVideo = {
                ...defaultVideo,
            };
            const component = mount(<LcVideoBlock width={LcSizes.S} video={video} />);

            expect(component.html()).toMatchSnapshot();
        });
    });

    describe('should render iframe', () => {
        const originalLazyIframe = LazyIframe.LazyIframe;

        beforeAll(() => {
            // исключаем withVisibilityProvider
            // @ts-ignore
            LazyIframe.LazyIframe = props => <LazyIframe.LazyIframeComponent {...props} isVisible />;
        });

        test('type youtube when visible', () => {
            const video: ILcVideo = {
                ...defaultVideo,
                type: VideoType.Youtube,
                youtubeSrc: 'jiOe-Ar8KOk',
            };
            const component = mount(<LcVideoBlock width={LcSizes.S} video={video} />);

            expect(component.html()).toMatchSnapshot();
        });

        test('type vimeo when visible', () => {
            const video: ILcVideo = {
                ...defaultVideo,
                type: VideoType.Vimeo,
                vimeoSrc: '234717047',
            };
            const component = mount(<LcVideoBlock width={LcSizes.S} video={video} />);

            expect(component.html()).toMatchSnapshot();
        });

        test('type VH when visible', () => {
            const video: ILcVideo = {
                ...defaultVideo,
                type: VideoType.VH,
                vhSrc: '4def59fcc06effd9aeabedb7f1171e1a',
            };
            const component = mount(<LcVideoBlock width={LcSizes.S} video={video} />);

            expect(component.html()).toMatchSnapshot();
        });

        test('type stream when visible', () => {
            const video: ILcVideo = {
                ...defaultVideo,
                type: VideoType.Stream,
                streamSrc: 'https://streaming.video.yandex-team.ru/get/video-nda/m-114538-1636e7b903d-bd6ef4934dc7580e/480p.m3u8',
            };
            const component = mount(<LcVideoBlock width={LcSizes.S} video={video} />);

            expect(component.html()).toMatchSnapshot();
        });

        test('without src when not visible', () => {
            // @ts-ignore
            LazyIframe.LazyIframe = originalLazyIframe;

            const video: ILcVideo = {
                ...defaultVideo,
                type: VideoType.Youtube,
                youtubeSrc: 'jiOe-Ar8KOk',
            };
            const component = mount(<LcVideoBlock width={LcSizes.S} video={video} />);

            expect(component.html()).toMatchSnapshot();
        });
    });
});
