import {
    videoAdapterOutstreamAdfoxFirst,
    isValidBSMetaVideoOutstreamAdfoxFirst,
} from './videoAdapterOutstreamAdfoxFirst';
import {
    BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST,
    BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST_DOUBLE,
    BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST_ENCODED,
    BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST_INVALID,
} from '../testMocks';

describe('[videoAdapterOutstream] videoAdapterOutstreamAdfoxFirst', () => {
    it('should return undefined if bsMeta is invalid', () => {
        const result = videoAdapterOutstreamAdfoxFirst(BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST_INVALID);

        expect(isValidBSMetaVideoOutstreamAdfoxFirst(BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST_INVALID)).toEqual(false);
        expect(result).toEqual(undefined);
    });

    it('should return correct value', () => {
        const result = videoAdapterOutstreamAdfoxFirst(BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST);

        expect(isValidBSMetaVideoOutstreamAdfoxFirst(BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST)).toEqual(true);
        expect(result?.data[0].attributes.vast.startsWith('<?xml')).toEqual(true);
    });

    it('should return correct encoded value', () => {
        const result = videoAdapterOutstreamAdfoxFirst(BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST_ENCODED);

        expect(isValidBSMetaVideoOutstreamAdfoxFirst(BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST_ENCODED)).toEqual(true);
        expect(typeof result?.data[0].attributes.vastBase64).toEqual('string');
        expect(result?.data[0].attributes.vastBase64.startsWith('<?xml')).toEqual(false);
    });

    it('should return correct double value', () => {
        const result = videoAdapterOutstreamAdfoxFirst(BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST_DOUBLE);

        expect(isValidBSMetaVideoOutstreamAdfoxFirst(BS_META_VIDEO_OUTSTREAM_ADFOX_FIRST_DOUBLE)).toEqual(true);
        expect(result?.data[0].attributes.vast.startsWith('<?xml')).toEqual(true);
        expect(result?.data[1].attributes.vast.startsWith('<?xml')).toEqual(true);
    });
});
