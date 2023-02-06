import { clone } from 'lodash';
import {
    videoAdapterOutstreamSecond,
    isValidBSMetaVideoOutstreamSecond,
} from './videoAdapterOutstreamSecond';
import {
    BS_META_VIDEO_OUTSTREAM_SECOND_INVALID,
    BS_META_VIDEO_OUTSTREAM_SECOND,
    BS_META_VIDEO_OUTSTREAM_SECOND_DOUBLE,
    BS_META_VIDEO_OUTSTREAM_SECOND_NEW,
} from '../testMocks';

describe('[videoAdapterOutstream] videoAdapterOutstreamSecond', () => {
    it('should return undefined if bsMeta is invalid', () => {
        const result = videoAdapterOutstreamSecond(BS_META_VIDEO_OUTSTREAM_SECOND_INVALID);

        expect(isValidBSMetaVideoOutstreamSecond(BS_META_VIDEO_OUTSTREAM_SECOND_INVALID)).toEqual(false);
        expect(result).toEqual(undefined);
    });

    it('should return correct value', () => {
        const result = videoAdapterOutstreamSecond(BS_META_VIDEO_OUTSTREAM_SECOND);

        expect(isValidBSMetaVideoOutstreamSecond(BS_META_VIDEO_OUTSTREAM_SECOND)).toEqual(true);
        expect(result?.direct.ads[0].video.startsWith('PD94')).toEqual(true);
    });

    it('should return correct value for BSMetaVideo', () => {
        const result = videoAdapterOutstreamSecond(BS_META_VIDEO_OUTSTREAM_SECOND_NEW);

        expect(isValidBSMetaVideoOutstreamSecond(BS_META_VIDEO_OUTSTREAM_SECOND_NEW)).toEqual(true);
        expect(result?.direct.ads[0].video.startsWith('PD94')).toEqual(true);
    });

    it('should return correct double value', () => {
        const result = videoAdapterOutstreamSecond(BS_META_VIDEO_OUTSTREAM_SECOND_DOUBLE);

        expect(isValidBSMetaVideoOutstreamSecond(BS_META_VIDEO_OUTSTREAM_SECOND_DOUBLE)).toEqual(true);
        expect(result?.direct.ads[0].video.startsWith('PD94')).toEqual(true);
        expect(result?.direct.ads[1].video.startsWith('PD94')).toEqual(true);
    });

    it('should return correct value for another vast holder prop', () => {
        const bsMeta = clone(BS_META_VIDEO_OUTSTREAM_SECOND);

        bsMeta.direct.ads[0].vast = bsMeta.direct.ads[0].video;
        delete bsMeta.direct.ads[0].video;

        const result = videoAdapterOutstreamSecond(bsMeta);

        expect(isValidBSMetaVideoOutstreamSecond(bsMeta)).toEqual(true);
        expect(result?.direct.ads[0].vast.startsWith('<?xml')).toEqual(true);
    });
});
