import { createAction } from './createAction';

const TYPE = 'TYPE';

describe('createAction', () => {
  it('should return redux action', () => {
    const action = createAction(TYPE);

    expect(action).toEqual({
      type: TYPE,
    });
  });

  it('should return redux action with payload', () => {
    const action = createAction(TYPE, 'payload');

    expect(action).toEqual({
      type: TYPE,
      payload: 'payload',
    });
  });
});
