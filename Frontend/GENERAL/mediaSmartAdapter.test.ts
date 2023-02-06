import {
    mediaSmartAdapter
} from './index';

import { newMetaFormat } from './mock';

describe('mediaSmartAdapter', () => {
    it('Тест для media-smart', () => {
        const adaptedData = mediaSmartAdapter(newMetaFormat, {
            isMobileSdk: false,
            isSspRtbCacheRequest: false
        });
        const preparedMetaToOldFormat = adaptedData.meta;
        const isAdaptedData = adaptedData.adapted;

        expect(isAdaptedData).toEqual(true);
        expect(preparedMetaToOldFormat).toMatchSnapshot();
    });
});
