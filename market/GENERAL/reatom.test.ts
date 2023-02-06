import { createAction } from './reatom';

describe('helpers/reatom', () => {
  describe('createAction', () => {
    it('must return the static type of the action', () => {
      const action = createAction('FOO')();

      expect(action.getType()).toBe('FOO');
    });

    it('must return the dynamic type of the action', () => {
      const action1 = createAction()();
      const action2 = createAction(['FOO'])();

      expect(action1.getType()).toMatch(/action \[[0-9]+\]/);
      expect(action2.getType()).toMatch(/FOO \[[0-9]+\]/);
    });
  });
});
