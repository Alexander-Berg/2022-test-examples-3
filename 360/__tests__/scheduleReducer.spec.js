import {ActionTypes} from '../scheduleConstants';
import reducer, {INITIAL_STATE} from '../scheduleReducer';

describe('scheduleReducer', () => {
  test('должен устанавливать начальное состояние', () => {
    expect(reducer(undefined, {})).toEqual(INITIAL_STATE);
  });

  describe('UPDATE_RANGE_START', () => {
    test('должен устанавливать isLoading', () => {
      const state = {
        range: {
          isLoading: false
        }
      };
      const expectedState = {
        range: {
          isLoading: true
        }
      };

      const action = {
        type: ActionTypes.UPDATE_RANGE_START
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });
  describe('UPDATE_RANGE_DONE', () => {
    test('должен устанавливать isLoading', () => {
      const state = {
        range: {
          isLoading: true
        }
      };

      const expectedState = {
        range: {
          isLoading: false
        }
      };

      const action = {
        type: ActionTypes.UPDATE_RANGE_DONE
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });
  describe('UPDATE_RANGE_SUCCESS', () => {
    test('должен устанавливать новые start и end', () => {
      const start = Number(new Date(2020, 0, 1));
      const end = Number(new Date(2020, 1, 1));

      const state = {
        range: {
          start: Number(new Date(2019, 0, 1)),
          end: Number(new Date(2019, 1, 1))
        }
      };
      const expectedState = {
        range: {
          start,
          end
        }
      };

      const action = {
        type: ActionTypes.UPDATE_RANGE_SUCCESS,
        start,
        end
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
    test('должен приводить новые start и end к началу соответствующих дней', () => {
      const expectedStart = Number(new Date(2020, 0, 1));
      const expectedEnd = Number(new Date(2020, 1, 1));

      const newStart = Number(new Date(2020, 0, 1, 10));
      const newEnd = Number(new Date(2020, 1, 1, 10));

      const state = {
        range: {
          start: Number(new Date(2019, 0, 1)),
          end: Number(new Date(2019, 1, 1))
        }
      };
      const expectedState = {
        range: {
          start: expectedStart,
          end: expectedEnd
        }
      };

      const action = {
        type: ActionTypes.UPDATE_RANGE_SUCCESS,
        start: newStart,
        end: newEnd
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
    test('должен сохранять исходные значения start и end, если они не были переданы', () => {
      const start = Number(new Date(2020, 0, 1));
      const end = Number(new Date(2020, 1, 1));

      const state = {
        range: {
          start,
          end
        }
      };
      const expectedState = {
        range: {
          start,
          end
        }
      };

      const action = {type: ActionTypes.UPDATE_RANGE_SUCCESS};

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });
  describe('EXTEND_RANGE_UP_START', () => {
    test('должен устанавливать isTopLoading', () => {
      const state = {
        range: {
          isTopLoading: false
        }
      };
      const expectedState = {
        range: {
          isTopLoading: true
        }
      };

      const action = {
        type: ActionTypes.EXTEND_RANGE_UP_START
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });
  describe('EXTEND_RANGE_DOWN_START', () => {
    test('должен устанавливать isBottomLoading', () => {
      const state = {
        range: {
          isBottomLoading: false
        }
      };
      const expectedState = {
        range: {
          isBottomLoading: true
        }
      };

      const action = {
        type: ActionTypes.EXTEND_RANGE_DOWN_START
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });
  describe('EXTEND_RANGE_UP_DONE', () => {
    test('должен устанавливать isTopLoading', () => {
      const state = {
        range: {
          isTopLoading: true
        }
      };
      const expectedState = {
        range: {
          isTopLoading: false
        }
      };

      const action = {
        type: ActionTypes.EXTEND_RANGE_UP_DONE
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });
  describe('EXTEND_RANGE_UP_SUCCESS', () => {
    test('должен устанавливать новое значение start', () => {
      const start = Number(new Date(2020, 0, 1));

      const state = {
        range: {
          start: Number(new Date(2020, 1, 1))
        }
      };
      const expectedState = {
        range: {
          start
        }
      };

      const action = {
        type: ActionTypes.EXTEND_RANGE_UP_SUCCESS,
        start
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });
  describe('EXTEND_RANGE_DOWN_DONE', () => {
    test('должен устанавливать isBottomLoading', () => {
      const state = {
        range: {
          isBottomLoading: true
        }
      };
      const expectedState = {
        range: {
          isBottomLoading: false
        }
      };

      const action = {
        type: ActionTypes.EXTEND_RANGE_DOWN_DONE
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });
  describe('EXTEND_RANGE_DOWN_SUCCESS', () => {
    test('должен устанавливать новое значение end', () => {
      const end = Number(new Date(2020, 1, 1));

      const state = {
        range: {
          end: Number(new Date(2020, 0, 1))
        }
      };
      const expectedState = {
        range: {
          end
        }
      };

      const action = {
        type: ActionTypes.EXTEND_RANGE_DOWN_SUCCESS,
        end
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });
  describe('SET_SHOW_DATE', () => {
    test('должен устанавливать новое значение showDate, если showDate различаются', () => {
      const state = {
        showDate: Number(new Date(2020, 1, 1))
      };

      const expectedState = {
        showDate: Number(new Date(2020, 1, 2))
      };

      const action = {
        type: ActionTypes.SET_SHOW_DATE,
        payload: {
          showDate: Number(new Date(2020, 1, 2))
        }
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
    test('не должен устанавливать новое значение showDate, если showDate не различаются', () => {
      const state = {
        showDate: Number(new Date(2020, 1, 2))
      };

      const action = {
        type: ActionTypes.SET_SHOW_DATE,
        payload: {
          showDate: Number(new Date(2020, 1, 2))
        }
      };

      expect(reducer(state, action)).toBe(state);
    });
  });
});
