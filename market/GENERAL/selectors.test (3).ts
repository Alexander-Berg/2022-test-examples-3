import testStore from 'src/helpers/testStore';
import { selectUser } from './selectors';

const state = testStore().store.getState();

describe('photographer selectors', () => {
  describe('selectUser', () => {
    it('should return CurrentUserState', () => {
      expect(selectUser(state)).toEqual(state.user);
    });
  });
});
