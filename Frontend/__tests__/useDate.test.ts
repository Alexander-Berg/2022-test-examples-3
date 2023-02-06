import { renderHook } from 'neo/tests/renderHook';
import 'sport/tests/mocks/hooks/contexts/useDataSourceCtx';

import { useDate } from 'sport/hooks/useDate';

describe('useDate', () => {
  it('get current date', () => {
    const {
      getAlias,
      getFormattedDate,
      getTimestampByAlias,
      setDate,
      ...tDateFields
    } = renderHook(() => useDate())();

    expect(tDateFields).toEqual({
      currentTimestamp: 1590573235493,
      timestamp: 1590573235493,
      timezone: 5,
    });
  });

  it('get specific timestamp', () => {
    const {
      getAlias,
      getFormattedDate,
      getTimestampByAlias,
      setDate,
      ...tDateFields
    } = renderHook(() => useDate({ timestamp: 1800000000000 }))();

    expect(tDateFields).toEqual({
      currentTimestamp: 1590573235493,
      timestamp: 1800000000000,
      timezone: 5,
    });
  });

  it('get specific timezone', () => {
    const {
      getAlias,
      getFormattedDate,
      getTimestampByAlias,
      setDate,
      ...tDateFields
    } = renderHook(() => useDate({ timezone: -2 }))();

    expect(tDateFields).toEqual({
      currentTimestamp: 1590573235493,
      timestamp: 1590573235493,
      timezone: -2,
    });
  });
});
