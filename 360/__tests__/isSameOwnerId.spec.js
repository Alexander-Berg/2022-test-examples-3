import EventRecord from '../../EventRecord';
import isSameOwnerId from '../isSameOwnerId';

describe('filterEventsByOwnerId', () => {
  test('вернуть false, если передан не EventRecord', () => {
    expect(isSameOwnerId('someid', new Map())).toBe(false);
  });
  test('вернуть false, если ownerId совпадает с переданным', () => {
    const event = new EventRecord({ownerId: '123'});
    expect(isSameOwnerId('123', event)).toBe(false);
  });
  test('вернуть true, если ownerId не совпадает с переданным', () => {
    const event = new EventRecord({ownerId: '123'});
    expect(isSameOwnerId('312', event)).toBe(true);
  });
});
