import testStore from 'src/helpers/testStore';
import { selectNotifications } from './selectors';

const state = testStore().store.getState();

describe('notifications selectors', () => {
  describe('selectNotifications', () => {
    it('should return Notification[]', () => {
      expect(selectNotifications(state)).toEqual(state.notifications);
    });
  });
});
