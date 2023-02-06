import isReservableTablesOffice from '../isReservableTablesOffice';

describe('isReservableTableOffice', () => {
  test('должен отдать true, если officeId входит в [2]', () => {
    expect(isReservableTablesOffice(1047)).toBe(true);
  });
  test('должен отдавать false, если officeId не входит в [2]', () => {
    expect(isReservableTablesOffice(3)).toBe(false);
  });
});
