import { MINIMAL_VALID_BS_META_VIDEO } from '../testMocks';
import { getBSMetaVideoFromVASTHolder } from './getBSMetaVideoFromVASTHolder';

describe('[videoAdapterOutstream] getBSMetaVideoFromVASTHolder', () => {
    it('should return undefined if no meta', () => {
        expect(getBSMetaVideoFromVASTHolder({})).toEqual(undefined);
    });

    it('should return "vast" value', () => {
        expect(getBSMetaVideoFromVASTHolder({
            vast: MINIMAL_VALID_BS_META_VIDEO,
        })).toEqual({
            bsMetaVideo: MINIMAL_VALID_BS_META_VIDEO,
            key: 'vast',
        });
    });

    it('should return "vastBase64" value', () => {
        expect(getBSMetaVideoFromVASTHolder({
            vastBase64: MINIMAL_VALID_BS_META_VIDEO,
        })).toEqual({
            bsMetaVideo: MINIMAL_VALID_BS_META_VIDEO,
            key: 'vastBase64',
        });
    });

    it('should return "video" value', () => {
        expect(getBSMetaVideoFromVASTHolder({
            video: MINIMAL_VALID_BS_META_VIDEO,
        })).toEqual({
            bsMetaVideo: MINIMAL_VALID_BS_META_VIDEO,
            key: 'video',
        });
    });
});
