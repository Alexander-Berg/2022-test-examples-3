import Access from '../Access';

describe('Access', () => {
  test('isRead', () => {
    expect(Access.isRead(0b01)).toEqual(true);
    expect(Access.isRead(0b00)).toEqual(false);
  });

  test('isEdit', () => {
    expect(Access.isEdit(0b11)).toEqual(true);
    expect(Access.isEdit(0b01)).toEqual(false);
  });

  test('create', () => {
    expect(Access.create({ read: false, edit: false })).toEqual(0b00);
    expect(Access.create({ read: true, edit: false })).toEqual(0b01);
    expect(Access.create({ read: true, edit: true })).toEqual(0b11);
  });

  test('and', () => {
    expect(Access.and(0b00, 0b01)).toEqual(0b00);
    expect(Access.and(0b01, 0b11)).toEqual(0b01);
    expect(Access.and(0b11, 0b11)).toEqual(0b11);
    expect(Access.and(0b10, 0b11)).toEqual(0b10);
  });

  test('normalizeToBoolean', () => {
    expect(Access.normalizeToBoolean(Access.MASKS.read, 0b01)).toEqual(true);
    expect(Access.normalizeToBoolean(Access.MASKS.read, 0b00)).toEqual(false);

    expect(Access.normalizeToBoolean(Access.MASKS.edit, 0b11)).toEqual(true);
    expect(Access.normalizeToBoolean(Access.MASKS.edit, 0b01)).toEqual(false);
  });
});
