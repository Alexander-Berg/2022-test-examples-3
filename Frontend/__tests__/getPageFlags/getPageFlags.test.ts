import { getFlagPossibleValues } from '../../getPageFlags';
import { tests, policies } from './data';

describe('getFlags', () => {
  tests.forEach((test) => {
    it(test.name, () => {
      expect(getFlagPossibleValues(test.flagTemplate, policies)).toEqual(test.expected);
    });
  });
});
