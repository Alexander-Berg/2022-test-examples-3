import { getFirstContantId } from '../selectors';
import state from './data.json';

describe('getFirstContantId', () => {
  it('should be 3483', () => {
    expect(getFirstContantId(state)).toBe(3483);
  });
});
