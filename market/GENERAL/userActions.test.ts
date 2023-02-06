import { UserActionRecorder } from './userActions';
import { UserActionTypes } from './userActionTypes';
import { setupApi } from 'src/test/api/setupApi';

test('record user events', () => {
  const api = setupApi();
  const userAction = UserActionRecorder.getInstance(api);
  userAction.setShop({ name: 'Ножи', id: 1, externalId: 1 });
  userAction.record(UserActionTypes.OPEN_PAGE);

  expect((api as any).userActionController).toBeTruthy();
});
