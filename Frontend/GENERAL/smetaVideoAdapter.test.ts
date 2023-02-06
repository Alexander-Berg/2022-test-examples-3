import { SMETA_VIDEO_BS_META_MOCK } from './mocks/SMETA_VIDEO_BS_META_MOCK';
import { smetaVideoAdapter, isSmetaVideoBSMeta } from './smetaVideoAdapter';

describe('smetaVideoAdapter', () => {
    it('should return correct value', () => {
        const bsMeta = SMETA_VIDEO_BS_META_MOCK;
        const adaptedBSMeta = smetaVideoAdapter(bsMeta);

        expect(isSmetaVideoBSMeta(bsMeta)).toEqual(true);
        expect(adaptedBSMeta.rtb.vast.bids[0].dc_params.data_params['72057606345282754']).toEqual(bsMeta.rtb.data_params);
        expect(adaptedBSMeta.settings['4'].videoInComboDesign).toEqual('morda-tzar');
    });
});
