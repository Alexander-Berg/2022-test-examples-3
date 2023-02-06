import { declareAction, __resetActionTypes } from './declareAction';

describe('reatom-helpers/declareAction', () => {
  afterEach(() => {
    __resetActionTypes();
  });

  it('should set static name', () => {
    const actionName = 'TEST';
    const actionCreator = declareAction(actionName)();

    expect(actionCreator()).toHaveProperty('type', actionName);
  });

  it('should throw error for duplicate action type', () => {
    const actionName = 'TEST';

    declareAction(actionName)();

    expect(() => {
      declareAction(actionName);
    }).toThrow();
  });
});
