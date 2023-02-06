import { getCurrentUser } from './actions';
import reducer, { initialState } from './reducer';

describe('Current user reducer', () => {
  describe('getCurrentUser.request', () => {
    it('should set authenticating to true', () => {
      expect(
        reducer(
          {
            ...initialState,
            authenticating: false,
          },
          getCurrentUser.request()
        )
      ).toEqual({
        ...initialState,
        authenticating: true,
      });
    });
  });

  describe('getCurrentUser.success', () => {
    it('should set authenticating to false and set user', () => {
      const user = { id: 1, login: 'some-user', roles: [] };

      expect(
        reducer(
          {
            ...initialState,
            authenticating: true,
          },
          getCurrentUser.success(user)
        )
      ).toEqual({
        ...initialState,
        authenticating: false,
        user,
      });
    });
  });

  describe('getCurrentUser.failure', () => {
    it('should set authenticating to false', () => {
      expect(
        reducer(
          {
            ...initialState,
            authenticating: true,
          },
          getCurrentUser.failure()
        )
      ).toEqual({
        ...initialState,
        authenticating: false,
      });
    });
  });
});
