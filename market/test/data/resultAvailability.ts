import { OfferAvailability, Reason, ResultAvailability } from 'src/java/definitions';
import { testDisplayOffer } from './displayOffer';
import { testDisplayMsku } from './displayMsku';

export const testResultAvailability = (ssku: Partial<ResultAvailability> = {}): ResultAvailability => {
  return {
    msku: testDisplayMsku(),
    offer: testDisplayOffer(),
    availabilitiesByWarehouseId: {
      '100': [
        {
          reason: Reason.MSKU,
          shortText: '',
          fullText: '',
          params: {},
          available: false,
        },
      ],
    },
    hidings: [],
    dropshipFit: 0,
    fulfillmentFit: 0,
    sskuStatus: OfferAvailability.ACTIVE,
    ...ssku,
  };
};
