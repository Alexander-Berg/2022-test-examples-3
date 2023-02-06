import getFloors from '../getFloors';
import getGroups from '../getGroups';
import {NO_FLOOR} from '../../inviteConstants';

jest.mock('../getGroups');
getGroups.mockImplementation(floor => floor);

describe('getFloors', () => {
  test('должен возвращать пустой объект, если ничего не передали', () => {
    expect(getFloors()).toEqual({order: []});
  });
  test('должен раскладывать переданный массив переговорок по этажам', () => {
    const resources = [{info: {floor: 5, id: 223}}, {info: {floor: 7, id: 123}}];

    expect(getFloors(resources)).toEqual({
      order: [5, 7],
      7: [{floor: 7, id: 123, isRoomResource: true}],
      5: [{floor: 5, id: 223, isRoomResource: true}]
    });
  });
  test('должен раскладывать переговорки без этажа в отдельную группу', () => {
    const resources = [{info: {id: 223}}, {info: {floor: 7, id: 123}}];

    expect(getFloors(resources)).toEqual({
      order: [NO_FLOOR, 7],
      7: [{floor: 7, id: 123, isRoomResource: true}],
      [NO_FLOOR]: [{id: 223, isRoomResource: true}]
    });
  });
});
