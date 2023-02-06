import { BoolState } from './BoolState';

describe('BoolState', () => {
  describe('.constructor', () => {
    it('sets state to defaultState argument', () => {
      const boolStateTrue = new BoolState(true);
      const boolStateFalse = new BoolState(false);

      expect(boolStateTrue.state).toBeTruthy();
      expect(boolStateFalse.state).toBeFalsy();
    });
  });

  describe('.set', () => {
    it('changes state to false', () => {
      const boolState = new BoolState(false);

      boolState.set(true);

      expect(boolState.state).toBeTruthy();
    });

    it('changes state to true', () => {
      const boolState = new BoolState(true);

      boolState.set(false);

      expect(boolState.state).toBeFalsy();
    });
  });

  describe('.on', () => {
    it('changes state to true', () => {
      const boolState = new BoolState(false);

      boolState.on();

      expect(boolState.state).toBeTruthy();
    });
  });

  describe('.off', () => {
    it('changes state to false', () => {
      const boolState = new BoolState(true);

      boolState.off();

      expect(boolState.state).toBeFalsy();
    });
  });
});
