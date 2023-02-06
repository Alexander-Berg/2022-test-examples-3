import {ShardedStore} from '../ShardedStore';

describe('ShardedStore', () => {
  describe('#getState', () => {
    test('должен применить переданный stateTransformer, при выдаче текущего стейта', () => {
      const store = {
        getState: () => ({state: 1})
      };

      const stateTransformer = state => ({modifiedState: true, ...state});

      const shardedStore = new ShardedStore(store, stateTransformer);

      expect(shardedStore.getState()).toEqual({state: 1, modifiedState: true});
    });
  });
});
