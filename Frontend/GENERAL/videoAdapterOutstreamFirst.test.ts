import {
    videoAdapterOutstreamFirst,
    isValidBSMetaVideoOutstreamFirst,
} from './videoAdapterOutstreamFirst';
import {
    BS_META_VIDEO_OUTSTREAM_FIRST,
    BS_META_VIDEO_OUTSTREAM_FIRST_ENCODED,
    BS_META_VIDEO_OUTSTREAM_FIRST_INVALID,
    BS_META_VIDEO_OUTSTREAM_FIRST_VASTBASE64_BASE64,
    BS_META_VIDEO_OUTSTREAM_FIRST_VIDEO_BASE64,
} from '../testMocks';

describe('[videoAdapterOutstream] videoAdapterOutstreamFirst', () => {
    it('should return undefined if bsMeta is invalid', () => {
        const result = videoAdapterOutstreamFirst(BS_META_VIDEO_OUTSTREAM_FIRST_INVALID);

        expect(isValidBSMetaVideoOutstreamFirst(BS_META_VIDEO_OUTSTREAM_FIRST_INVALID)).toEqual(false);
        expect(result).toEqual(undefined);
    });

    it('should return correct value', () => {
        const result = videoAdapterOutstreamFirst(BS_META_VIDEO_OUTSTREAM_FIRST);

        expect(isValidBSMetaVideoOutstreamFirst(BS_META_VIDEO_OUTSTREAM_FIRST)).toEqual(true);
        expect(result?.rtb.vast.startsWith('<?xml')).toEqual(true);
        expect(typeof result?.rtb.data_params).toEqual('object');
    });

    it('should return correct encoded value', () => {
        const result = videoAdapterOutstreamFirst(BS_META_VIDEO_OUTSTREAM_FIRST_ENCODED);

        expect(isValidBSMetaVideoOutstreamFirst(BS_META_VIDEO_OUTSTREAM_FIRST_ENCODED)).toEqual(true);
        expect(typeof result?.rtb.vastBase64).toEqual('string');
        expect(result?.rtb.vastBase64.startsWith('<?xml')).toEqual(false);
    });

    it('should return correct value for video base64', () => {
        const result = videoAdapterOutstreamFirst(BS_META_VIDEO_OUTSTREAM_FIRST_VIDEO_BASE64);

        expect(isValidBSMetaVideoOutstreamFirst(BS_META_VIDEO_OUTSTREAM_FIRST_VIDEO_BASE64)).toEqual(true);
        expect(typeof result?.rtb.video).toEqual('string');
        expect(result?.rtb.video.startsWith('<?xml')).toEqual(false);
    });

    it('should return correct value for vastBase64 base64', () => {
        const result = videoAdapterOutstreamFirst(BS_META_VIDEO_OUTSTREAM_FIRST_VASTBASE64_BASE64);

        expect(isValidBSMetaVideoOutstreamFirst(BS_META_VIDEO_OUTSTREAM_FIRST_VASTBASE64_BASE64)).toEqual(true);
        expect(typeof result?.rtb.vastBase64).toEqual('string');
        expect(result?.rtb.vastBase64.startsWith('<?xml')).toEqual(false);
    });
});
