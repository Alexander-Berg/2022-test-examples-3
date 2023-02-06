import { cloneDeep } from 'lodash';
import { tgoAdapter } from './tgoAdapter';
import { newMetaFormat } from './mock';
import { BSMeta } from '../../ssr/parseMeta';

describe('tgoAdapter', () => {
    it('should use videoAdapter for vast/vastBase64 field if it is object', () => {
        const metaForVast: BSMeta = cloneDeep(newMetaFormat);
        const metaForVastBase64: BSMeta = cloneDeep(newMetaFormat);

        metaForVast.direct.ads[0].bs_data.vast = { campaign_id: '0' };
        metaForVastBase64.direct.ads[0].bs_data.vastBase64 = { campaign_id: '0' };

        const adapterDataWithVast = tgoAdapter(metaForVast);
        const adapterDataWithVastBase64 = tgoAdapter(metaForVastBase64);

        expect(adapterDataWithVast.meta.direct.ads[0].vast.startsWith('<?xml')).toBe(true);
        expect(adapterDataWithVastBase64.meta.direct.ads[0].vastBase64.startsWith('PD94b')).toBe(true);
    });

    it('should not use videoAdapter for vast/vastBase64 if it is string', () => {
        const metaForVast: BSMeta = cloneDeep(newMetaFormat);
        const metaForVastBase64: BSMeta = cloneDeep(newMetaFormat);

        metaForVast.direct.ads[0].bs_data.vast = '<VAST>';
        metaForVastBase64.direct.ads[0].bs_data.vastBase64 = '<VAST_BASE64>';

        const adapterDataWithVast = tgoAdapter(metaForVast);
        const adapterDataWithVastBase64 = tgoAdapter(metaForVastBase64);

        expect(adapterDataWithVast.meta.direct.ads[0].vast).toBe('<VAST>');
        expect(adapterDataWithVastBase64.meta.direct.ads[0].vastBase64).toBe('<VAST_BASE64>');
    });
});
