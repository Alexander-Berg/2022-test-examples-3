import { VerdictMessageType } from 'src/entities/datacampOffer/types';
import { getIncorrectOfferReasons } from './getIncorrectOfferReasons';

const stringReasons = [VerdictMessageType.ERROR];
const offer = {
  publish: VerdictMessageType.ERROR,
};

const fnReasons = [
  (data: any) => {
    if (data.publish === VerdictMessageType.ERROR) {
      return VerdictMessageType.ERROR;
    }
    return undefined;
  },
];

describe('getIncorrectOfferReasons', () => {
  test('getIncorrectOfferReasons string reasons', () => {
    const reasons = getIncorrectOfferReasons(offer, stringReasons);
    expect(reasons).toHaveLength(1);
  });

  test('getIncorrectOfferReasons function reasons', () => {
    const reasons = getIncorrectOfferReasons(offer, fnReasons);
    expect(reasons).toHaveLength(1);
  });
});
