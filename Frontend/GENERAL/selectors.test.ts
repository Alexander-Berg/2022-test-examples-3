import {
    selectSegmentKeysBySegmentation,
    selectCampaignProviderResourcesById,
    selectProviderDefaultResources,
} from './selectors';

import { mockStoreData } from '../test-data/storeData';
import { campaignSettings } from '../test-data/campaignSettings';
import { Store } from '../store';

const state: Store = {
    ...mockStoreData,
    campaignSettings,
};

describe('common selectors', () => {
    it('SegmentKeysBySegmentation selector', () => {
        const segmentKeysBySegmentation = selectSegmentKeysBySegmentation(state);
        expect(segmentKeysBySegmentation).toMatchSnapshot();
    });

    it('CampaignProviderResourcesById selector', () => {
        const campaignProviderResourcesById = selectCampaignProviderResourcesById(state);
        expect(campaignProviderResourcesById).toMatchSnapshot();
    });

    it('ProviderDefaultResources selector', () => {
        const providerDefaultResources = selectProviderDefaultResources(state);
        expect(providerDefaultResources).toMatchSnapshot();
    });
});
