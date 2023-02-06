import {
    videoAdapterOutstreamAdfoxSecond,
    isValidBSMetaVideoOutstreamAdfoxSecond,
} from './videoAdapterOutstreamAdfoxSecond';
import {
    BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND,
    BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_DOUBLE,
    BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_ENCODED,
    BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_INVALID,
    BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_VASTBASE64_BASE64,
    BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_VIDEO_BASE64,
} from '../testMocks';

describe('[videoAdapterOutstream] videoAdapterOutstreamAdfoxSecond', () => {
    it('should return undefined if bsMeta is invalid', () => {
        const result = videoAdapterOutstreamAdfoxSecond(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_INVALID);

        expect(isValidBSMetaVideoOutstreamAdfoxSecond(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_INVALID)).toEqual(false);
        expect(result).toEqual(undefined);
    });

    it('should return correct value', () => {
        const result = videoAdapterOutstreamAdfoxSecond(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND);

        expect(isValidBSMetaVideoOutstreamAdfoxSecond(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND)).toEqual(true);
        expect(result?.data[0].attributes.data.rtb.vast.startsWith('<?xml')).toEqual(true);
        expect(typeof result?.data[0].attributes.data.rtb.data_params).toEqual('object');
    });

    it('should return correct encoded value', () => {
        const result = videoAdapterOutstreamAdfoxSecond(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_ENCODED);

        expect(isValidBSMetaVideoOutstreamAdfoxSecond(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_ENCODED)).toEqual(true);
        expect(typeof result?.data[0].attributes.data.rtb.vastBase64).toEqual('string');
        expect(result?.data[0].attributes.data.rtb.vastBase64.startsWith('<?xml')).toEqual(false);
    });

    it('should return correct double value', () => {
        const result = videoAdapterOutstreamAdfoxSecond(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_DOUBLE);

        expect(isValidBSMetaVideoOutstreamAdfoxSecond(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_DOUBLE)).toEqual(true);
        expect(result?.data[0].attributes.data.rtb.vast.startsWith('<?xml')).toEqual(true);
        expect(result?.data[1].attributes.data.rtb.vast.startsWith('<?xml')).toEqual(true);
    });

    it('should return correct value for video base64', () => {
        const result = videoAdapterOutstreamAdfoxSecond(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_VIDEO_BASE64);

        expect(isValidBSMetaVideoOutstreamAdfoxSecond(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_VIDEO_BASE64)).toEqual(true);
        expect(typeof result?.data[0].attributes.data.rtb.video).toEqual('string');
        expect(result?.data[0].attributes.data.rtb.video.startsWith('<?xml')).toEqual(false);
    });

    it('should return correct value for vastBase64 base64', () => {
        const result = videoAdapterOutstreamAdfoxSecond(BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_VASTBASE64_BASE64);

        expect(isValidBSMetaVideoOutstreamAdfoxSecond(
            BS_META_VIDEO_OUTSTREAM_ADFOX_SECOND_VASTBASE64_BASE64,
        )).toEqual(true);
        expect(typeof result?.data[0].attributes.data.rtb.vastBase64).toEqual('string');
        expect(result?.data[0].attributes.data.rtb.vastBase64.startsWith('<?xml')).toEqual(false);
    });
});
