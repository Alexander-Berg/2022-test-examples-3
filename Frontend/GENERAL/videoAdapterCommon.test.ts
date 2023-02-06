import { videoAdapterCommon } from './videoAdapterCommon';
import {
    BS_META_VIDEO_OUTSTREAM_FIRST,
    MINIMAL_VALID_BS_META_VIDEO,
    INVALID_BS_META_VIDEO,
    INVALID_BS_META_VIDEO_BANNER_STORAGE,
} from '../videoAdapterOutstream/testMocks';

// Используем новую схему работы с base64
jest.mock('../../../utils/ExperimentalFlags', () => {
    return {
        experimentalFlags: {
            getFlagValue: (name: string) => {
                return name === 'RR_USE_SIMPLE_BASE64_CODER' ? 'TRUE' : undefined;
            },
        },
    };
});

describe('videoAdapterCommon', () => {
    it('should return empty string for invalid outstream', () => {
        const result = videoAdapterCommon({
            rtb: {
                vast: INVALID_BS_META_VIDEO_BANNER_STORAGE,
            },
        });

        expect(result).toEqual('');
    });

    it('should return empty string for invalid instream', () => {
        const result = videoAdapterCommon(INVALID_BS_META_VIDEO);

        expect(result).toEqual('');
    });

    it('should return correct value for outstream', () => {
        const result = videoAdapterCommon(BS_META_VIDEO_OUTSTREAM_FIRST);

        // @ts-expect-error
        expect(result.rtb.vast.startsWith('<?xml')).toEqual(true);
        // @ts-expect-error
        expect(Boolean(result.rtb.data_params)).toEqual(true);
    });

    it('should return correct value for instream', () => {
        const result = videoAdapterCommon(MINIMAL_VALID_BS_META_VIDEO);

        // @ts-ignore
        expect(result.startsWith('<?xml')).toEqual(true);
    });
});
