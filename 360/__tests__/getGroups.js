import getGroups from '../getGroups';
import {NO_GROUP} from '../../inviteConstants';

describe('getGroups', () => {
  test('должен группировать переговорки по имени группы', () => {
    const res1 = {group: 'g1', id: 123, isRoomResource: true};
    const res2 = {group: 'g2', id: 223, isRoomResource: true};
    const resources = [res1, res2];
    const expectedResult = {
      g1: [res1],
      g2: [res2],
      order: ['g1', 'g2']
    };

    expect(getGroups(resources)).toEqual(expectedResult);
  });
  test('должен группировать переговорки без имени группы в NO_GROUP', () => {
    const res1 = {group: 'g1', id: 123, isRoomResource: true};
    const res2 = {group: 'g2', id: 223, isRoomResource: true};
    const res3 = {id: 223, isRoomResource: true};
    const resources = [res1, res2, res3];
    const expectedResult = {
      g1: [res1],
      g2: [res2],
      [NO_GROUP]: [res3],
      order: [NO_GROUP, 'g1', 'g2']
    };

    expect(getGroups(resources)).toEqual(expectedResult);
  });
});
