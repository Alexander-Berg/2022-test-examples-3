import {
    INVALID_BS_META_VIDEO_BANNER_STORAGE,
    VALID_BS_META_VIDEO_AD,
    MINIMAL_VALID_BS_META_VIDEO_AD,
    INVALID_BS_META_VIDEO_AD_2,
    BANNER_STORAGE_VIDEO_OUTSTREAM_SECOND_VAST,
} from '../testMocks';
import { getOutstreamDataParams } from './getOutstreamDataParams';

describe('[videoAdapterOutstream] getOutstreamDataParams', () => {
    it('should return undefined for invalid ad', () => {
        expect(getOutstreamDataParams(INVALID_BS_META_VIDEO_BANNER_STORAGE)).toEqual(undefined);
        expect(getOutstreamDataParams(INVALID_BS_META_VIDEO_AD_2)).toEqual(undefined);
    });

    it('should return correct value', () => {
        expect(getOutstreamDataParams(VALID_BS_META_VIDEO_AD)).toEqual({
            text: {
                body: 'body',
                domain: 'domain',
                title: 'title',
                green_url_text_prefix: 'green_url_text_prefix',
                green_url_text_suffix: 'green_url_text_suffix',
                age: 'age',
                warning: 'warning',
                bannerFlags: 'bannerFlags',
            },
            assets: {
                button: {
                    key: 'assets.button.key',
                    caption: 'assets.button.caption',
                    href: 'resource_links.direct_data.assets.button.href',
                },
                logo: {
                    'assets.logo.format': {
                        someField: 'assets.logo.someField',
                    },
                },
                images: [['https:first_img', '100', '100']],
            },
            click_url: {
                text_name: 'resource_links.direct_data.targetUrl',
                action_button: 'actionButton',
            },
            punyDomain: 'domain',
            faviconWidth: '50',
            faviconHeight: '50',
            adId: 'adId',
            videoHeight: 720,
            videoWidth: 1080,
            abuse: 'https:abuseUrl',
            firstFrame: {
                images: [{
                    width: 1920,
                    height: 720,
                    url: 'firstFrameUrl',
                    type: 'firstFrameType',
                }],
            },
            newImage: {
                '1': {
                    h: 100,
                    val: 'first_img',
                    w: 100,
                },
            },
        });
    });

    it('should return correct value for minimum data', () => {
        expect(getOutstreamDataParams(MINIMAL_VALID_BS_META_VIDEO_AD)).toEqual({
            text: {
                body: undefined,
                domain: undefined,
                title: undefined,
                green_url_text_prefix: undefined,
                green_url_text_suffix: undefined,
                age: '',
                warning: '',
                bannerFlags: 'bannerFlags',
            },
            assets: {
                button: {
                    key: undefined,
                    caption: undefined,
                    href: undefined,
                },
                logo: undefined,
                images: undefined,
            },
            click_url: {
                text_name: undefined,
                action_button: undefined,
            },
            punyDomain: '',
            faviconWidth: undefined,
            faviconHeight: undefined,
            adId: 'adId',
            videoHeight: null,
            videoWidth: null,
            firstFrame: undefined,
        });
    });

    it('should return data for banner storage vast', () => {
        expect(getOutstreamDataParams({ vast: BANNER_STORAGE_VIDEO_OUTSTREAM_SECOND_VAST })?.firstFrame).toEqual({
            images: [
                {
                    type: 'image/jpeg',
                    width: 1920,
                    height: 1080,
                    url: 'https://avatars.mds.yandex.net/get-vh/6549758/2a0000018060af81c74b5d860397b1405f0a/orig',
                },
            ],
        });
    });
});
