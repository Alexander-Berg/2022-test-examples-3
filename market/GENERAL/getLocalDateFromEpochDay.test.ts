import { getLocalDateFromEpochDay, MILLISECONDS_IN_DAY } from 'src/shared/common-logs/helpers/getLocalDateFromEpochDay';

describe('getLocalDateFromEpochDay', () => {
  it('', () => {
    expect(getLocalDateFromEpochDay(1234).getTime()).toEqual(1234 * MILLISECONDS_IN_DAY);
  });
});
