import { videoAdapterOutstream, isValidBSMetaVideoOutstream } from './videoAdapterOutstream';
import {
    BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST,
    BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND,
    BS_META_VIDEO_OUTSTREAM_EMPTY,
    BS_META_VIDEO_OUTSTREAM_EMPTY_2,
    BS_META_VIDEO_OUTSTREAM_FIRST,
    BS_META_VIDEO_OUTSTREAM_FIRST_2,
    BS_META_VIDEO_OUTSTREAM_FIRST_INVALID,
    BS_META_VIDEO_OUTSTREAM_SECOND_NEW,
} from './testMocks';

describe('videoAdapterOutstream', () => {
    it('should return undefined if bsMeta is invalid', () => {
        const result = videoAdapterOutstream(BS_META_VIDEO_OUTSTREAM_FIRST_INVALID);

        expect(isValidBSMetaVideoOutstream(BS_META_VIDEO_OUTSTREAM_FIRST_INVALID)).toEqual(false);
        expect(result).toEqual(undefined);
    });

    it('should return bsMeta as is if no banners', () => {
        expect(isValidBSMetaVideoOutstream(BS_META_VIDEO_OUTSTREAM_EMPTY)).toEqual(true);
        expect(videoAdapterOutstream(BS_META_VIDEO_OUTSTREAM_EMPTY)).toEqual(BS_META_VIDEO_OUTSTREAM_EMPTY);

        expect(isValidBSMetaVideoOutstream(BS_META_VIDEO_OUTSTREAM_EMPTY_2)).toEqual(true);
        expect(videoAdapterOutstream(BS_META_VIDEO_OUTSTREAM_EMPTY_2)).toEqual(BS_META_VIDEO_OUTSTREAM_EMPTY_2);
    });

    it('should return correct value for outstream first', () => {
        const result = videoAdapterOutstream(BS_META_VIDEO_OUTSTREAM_FIRST);

        expect(isValidBSMetaVideoOutstream(BS_META_VIDEO_OUTSTREAM_FIRST)).toEqual(true);
        expect(result?.rtb.vast.startsWith('<?xml')).toEqual(true);
        expect(result?.rtb?.data_params.adId).toEqual('adId');
    });

    it('should return correct value for outstream second', () => {
        const result = videoAdapterOutstream(BS_META_VIDEO_OUTSTREAM_SECOND_NEW);

        expect(isValidBSMetaVideoOutstream(BS_META_VIDEO_OUTSTREAM_SECOND_NEW)).toEqual(true);
        expect(result?.direct.ads[0].video.startsWith('PD94')).toEqual(true);
    });

    it('should return correct value for outstream adfox first', () => {
        const result = videoAdapterOutstream(BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST);

        expect(isValidBSMetaVideoOutstream(BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST)).toEqual(true);
        expect(result?.data[0].attributes.vast.startsWith('<?xml')).toEqual(true);
    });

    it('should return correct value for outstream adfox second', () => {
        const result = videoAdapterOutstream(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND);

        expect(isValidBSMetaVideoOutstream(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND)).toEqual(true);
        expect(result?.data[0].attributes.data.rtb.vast.startsWith('<?xml')).toEqual(true);
    });

    it('should return correct data_params', () => {
        const result = videoAdapterOutstream(BS_META_VIDEO_OUTSTREAM_FIRST_2);

        expect(result?.rtb?.data_params).toEqual({
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
                images: [
                    {
                        height: 720,
                        type: 'firstFrameType',
                        url: 'firstFrameUrl',
                        width: 1920,
                    },
                ],
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

    it('should return the same json for empty outstream', () => {
        const bsMetaFirst = {
            rtbAuctionInfo: {
                dspId: 5,
            },
        };
        const bsMetaSecond = {
            rtbAuctionInfo: {
                dspId: 10,
            },
        };

        expect(isValidBSMetaVideoOutstream(bsMetaFirst)).toEqual(true);
        expect(videoAdapterOutstream(bsMetaFirst)).toEqual(bsMetaFirst);

        expect(isValidBSMetaVideoOutstream(bsMetaSecond)).toEqual(true);
        expect(videoAdapterOutstream(bsMetaSecond)).toEqual(bsMetaSecond);
    });

    it('should return the same json for external dsp', () => {
        const bsMeta = {
            data: [{
                attributes: {
                    data: {
                        common: {
                            productType: '',
                        },
                    },
                },
            }],
        };

        expect(isValidBSMetaVideoOutstream(bsMeta)).toEqual(true);
        expect(videoAdapterOutstream(bsMeta)).toEqual(bsMeta);
    });
});
