import { reaction } from 'mobx';
import { TabVisibilityStore } from './TabVisibilityStore';

const setVisibilityState = (value: 'hidden' | 'visible') => {
  Object.defineProperty(document, 'visibilityState', {
    configurable: true,
    get: function() {
      return value;
    },
  });
  document.dispatchEvent(new Event('visibilitychange'));
};

describe('TabVisibilityStore', () => {
  it('inits with right value', () => {
    setVisibilityState('visible');
    const tabVisibilityStore = new TabVisibilityStore();
    tabVisibilityStore.destroy();

    expect(tabVisibilityStore.state).toBe('visible');
  });

  it('uses mobx observable', () => {
    const reactionMock = jest.fn();

    setVisibilityState('visible');
    const tabVisibilityStore = new TabVisibilityStore();

    const dispose = reaction(() => tabVisibilityStore.state, reactionMock);
    setVisibilityState('hidden');

    dispose();
    tabVisibilityStore.destroy();

    expect(reactionMock).toBeCalled();
    expect(reactionMock.mock.calls[0][0]).toBe('hidden');
  });
});
