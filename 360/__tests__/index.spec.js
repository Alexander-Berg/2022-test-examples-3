import device from 'device';
import {offlineSelector} from 'features/appStatus/appStatusSelectors';

import {skip} from '../actions';
import middleware from '../index';

jest.mock('features/appStatus/appStatusSelectors');
jest.mock('../actions');

function createMiddlewareMocks({next: customNext, store: customStore, action: customAction} = {}) {
  return {
    next: customNext || jest.fn(),
    store: customStore || {
      dispatch: jest.fn(),
      getState: jest.fn()
    },
    action: customAction || {}
  };
}

function callMiddleware(store, next, action, options = {}) {
  return middleware(options)(store)(next)(action);
}

describe('offlineMiddleware', function() {
  beforeEach(function() {
    offlineSelector.mockReturnValue(true);
    sinon.stub(device, 'allowPersistData').value(true);
  });

  it('если нет метаинформации об оффлайне, то не должна вызывать store.dispatch', function() {
    const {next, store, action} = createMiddlewareMocks();
    callMiddleware(store, next, action);

    expect(next).toHaveBeenCalledTimes(1);
    expect(store.dispatch).toHaveBeenCalledTimes(0);
  });

  describe('должна вызвать networkAction', function() {
    it('если есть флаг useOnlyNetwork', function() {
      const networkAction = {networkAction: true};

      const {next, store, action} = createMiddlewareMocks({
        action: {meta: {offline: {network: networkAction}}}
      });
      callMiddleware(store, next, action, {useOnlyNetwork: true});

      expect(next).toHaveBeenCalledTimes(1);
      expect(store.dispatch).toHaveBeenCalledTimes(1);
      expect(store.dispatch).toHaveBeenCalledWith(networkAction);
    });
    it('если нельзя персистить данные', function() {
      sinon.stub(device, 'allowPersistData').value(false);

      const networkAction = {networkAction: true};

      const {next, store, action} = createMiddlewareMocks({
        action: {meta: {offline: {network: networkAction}}}
      });
      callMiddleware(store, next, action);

      expect(next).toHaveBeenCalledTimes(1);
      expect(store.dispatch).toHaveBeenCalledTimes(1);
      expect(store.dispatch).toHaveBeenCalledWith(networkAction);
    });
    it('если не оффлайн', function() {
      offlineSelector.mockReturnValue(false);
      const networkAction = {networkAction: true};

      const {next, store, action} = createMiddlewareMocks({
        action: {meta: {offline: {network: networkAction}}}
      });
      callMiddleware(store, next, action);

      expect(next).toHaveBeenCalledTimes(1);
      expect(store.dispatch).toHaveBeenCalledTimes(1);
      expect(store.dispatch).toHaveBeenCalledWith(networkAction);
    });
  });

  describe('должна вызвать offlineAction', function() {
    it('если оффлайн и есть кастомный оффлайн экшен', function() {
      const networkAction = {networkAction: true};
      const offlineAction = {offlineAction: true};

      const {next, store, action} = createMiddlewareMocks({
        action: {
          meta: {
            offline: {
              network: networkAction,
              rollback: offlineAction
            }
          }
        }
      });
      callMiddleware(store, next, action);

      expect(next).toHaveBeenCalledTimes(1);
      expect(store.dispatch).toHaveBeenCalledTimes(1);
      expect(store.dispatch).toHaveBeenCalledWith(offlineAction);
    });
    it('если оффлайн и нет кастомного оффлайн экшена', function() {
      const skipAction = {skip: true};
      const networkAction = {networkAction: true};

      skip.mockReturnValue(skipAction);

      const {next, store, action} = createMiddlewareMocks({
        action: {meta: {offline: {network: networkAction}}}
      });
      callMiddleware(store, next, action);

      expect(next).toHaveBeenCalledTimes(1);
      expect(store.dispatch).toHaveBeenCalledTimes(1);
      expect(store.dispatch).toHaveBeenCalledWith(skipAction);
    });
  });
});
