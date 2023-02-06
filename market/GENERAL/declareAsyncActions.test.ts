import { __resetActionTypes } from './declareAction';
import { declareAsyncActions } from './declareAsyncActions';

describe('reatom-helpers/declareAsyncActions', () => {
  afterEach(() => {
    __resetActionTypes();
  });

  it('should have getType property', () => {
    const asyncActionCreator = declareAsyncActions('TEST')();

    expect(asyncActionCreator).toHaveProperty('getType');
    expect(typeof asyncActionCreator.getType).toBe('function');
  });

  it('should returns action type', () => {
    const actionType = 'TEST';
    const asyncActionCreator = declareAsyncActions(actionType)();

    expect(asyncActionCreator.getType()).toBe(actionType);
  });

  describe.each(['start', 'done', 'fail', 'cancel'] as const)('asyncActionCreator.%s ', property => {
    it(`should have ${property} property`, () => {
      const asyncActionCreator = declareAsyncActions('TEST')();

      expect(asyncActionCreator).toHaveProperty(property);
      expect(typeof asyncActionCreator[property]).toBe('function');
    });

    it(`should returns ${property} action`, () => {
      const asyncActionCreator = declareAsyncActions('TEST')();
      const action = asyncActionCreator[property]();

      expect(action).toHaveProperty('type');
    });
  });
});
