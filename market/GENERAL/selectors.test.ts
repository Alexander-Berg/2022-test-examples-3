import testStore from 'src/helpers/testStore';
import { selectPhotographerInstructionState } from './selectors';

const state = testStore().store.getState();

describe('photographer selectors', () => {
  describe('selectPhotographerInstructionState', () => {
    it('should return PhotographerState', () => {
      expect(selectPhotographerInstructionState(state)).toEqual(state.pages.photographerInstruction);
    });
  });
});
