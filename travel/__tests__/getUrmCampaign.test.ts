import {UTM_CAMPAIGN_DEFAULT} from '../constants';

import IStateSeoQueryParams from '../../../interfaces/state/IStateSeoQueryParams';

import getUtmCampaign from '../getUtmCampaign';

describe('getUtmCampaign', () => {
    it('Вернёт ожидаемую метку', () => {
        expect(getUtmCampaign()).toBe(UTM_CAMPAIGN_DEFAULT);
        expect(getUtmCampaign({from: '1'} as IStateSeoQueryParams)).toBe('1');
        expect(
            getUtmCampaign({from: '1', utmSource: '2'} as IStateSeoQueryParams),
        ).toBe('2');
    });
});
