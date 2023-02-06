import { isActionOf } from './redux-observable';
import { createAction } from './reatom';

describe('helpers/redux-observable', () => {
  describe('isActionOf', () => {
    it('should match single action', () => {
      const action = createAction('SOME')<string>();

      expect(isActionOf(action, { type: 'SOME' })).toBe(true);
    });

    it('should match multiple actions', () => {
      const foo = createAction('FOO')<string>();
      const bar = createAction('BAR')<string>();

      expect(isActionOf([foo, bar], { type: 'FOO' })).toBe(true);
      expect(isActionOf([foo, bar], { type: 'BAR' })).toBe(true);
    });

    it('should does not match single action', () => {
      const foo = createAction('FOO')<string>();

      expect(isActionOf(foo, { type: 'BAR' })).toBe(false);
    });

    it('should does not match multiple actions', () => {
      const foo = createAction('FOO')<string>();
      const bar = createAction('BAR')<string>();

      expect(isActionOf([foo, bar], { type: 'BAZ' })).toBe(false);
    });

    it('should support carrying', () => {
      const foo = createAction('FOO')<string>();
      const match = isActionOf(foo);

      expect(typeof match).toBe('function');
      expect(match({ type: 'FOO' })).toBe(true);
    });
  });
});
