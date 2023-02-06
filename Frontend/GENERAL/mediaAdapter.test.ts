import {
    mediaAdapter,
} from './index';

import {
    newMetaFormat
} from './mock';
import { Experiments } from '../../../utils/experiments/Experiments';

describe('mediaAdapter', () => {
    it('Простой тест для media', () => {
        const experiments = new Experiments({ flagsMap: {}, flags: {}, testIds: '' });
        const adaptedData = mediaAdapter(newMetaFormat, 'MediaCreative', experiments, {
            isMobileSdk: false,
            isSspFromIframeRequest: false,
            isSspRtbCacheRequest: false,
            enableSsrMediaImageInMobileSdk: false,
            enableSsrMediaCreativeInMobileSdk: false,
            enableSsrMediaCreativeReachInMobileSdk: false,
        });
        const preparedMetaToOldFormat = adaptedData.meta;
        const isAdaptedData = adaptedData.adapted;

        expect(isAdaptedData).toEqual(true);
        expect(preparedMetaToOldFormat).toMatchSnapshot();
    });
});
