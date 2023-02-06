import {ActionTypes} from '../modalNavigationConstants';
import reducer, {initialState} from '../modalNavigationReducer';

describe('ModalNavigation reducer', () => {
  test('должен возвращать начальное состояние', () => {
    expect(reducer(undefined, {})).toEqual(initialState);
  });

  describe('OPEN_MODAL', () => {
    test('должен добавлять новый элемент стека', () => {
      const action = {
        type: ActionTypes.OPEN_MODAL,
        id: 4
      };

      const state = [1, 2, 3];
      const expectedState = [1, 2, 3, 4];

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });

  describe('CLOSE_MODAL', () => {
    test('должен удалять последний элемент стека', () => {
      const action = {
        type: ActionTypes.CLOSE_MODAL,
        id: 3
      };

      const state = [1, 2, 3];
      const expectedState = [1, 2];

      expect(reducer(state, action)).toEqual(expectedState);
    });

    test('должен удалять элементы стека до id включительно', () => {
      const action = {
        type: ActionTypes.CLOSE_MODAL,
        id: 3
      };

      const state = [1, 2, 3, 4, 5];
      const expectedState = [1, 2];

      expect(reducer(state, action)).toEqual(expectedState);
    });

    test('не должен удалять элемент из стека если элемента с таким id нет', () => {
      const action = {
        type: ActionTypes.CLOSE_MODAL,
        id: 20
      };

      const state = [1, 2, 3, 4, 5];
      const expectedState = [1, 2, 3, 4, 5];

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });
});
