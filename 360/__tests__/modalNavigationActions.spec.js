import {ActionTypes} from '../modalNavigationConstants';
import {openModal, closeModal} from '../modalNavigationActions';

describe('ModalNavigation actions', () => {
  test('должен создавать правильный action openModal', () => {
    const id = 1;
    const expectedAction = {
      type: ActionTypes.OPEN_MODAL,
      id
    };
    expect(openModal(id)).toEqual(expectedAction);
  });
  test('должен создавать правильный action closeModal', () => {
    const id = 1;
    const expectedAction = {
      type: ActionTypes.CLOSE_MODAL,
      id
    };
    expect(closeModal(id)).toEqual(expectedAction);
  });
});
