import createTelURI from '../createTelURI';

describe('createTelURI', () => {
  test('должен возвращать телефон, предваряющийся tel:', () => {
    const tel = '+79999999';
    expect(createTelURI(tel)).toBe('tel:' + tel);
  });
  test('должен удалять лишние символы', () => {
    const tel = '+7 (999) – 999 - 99 - 99';
    expect(createTelURI(tel)).toBe('tel:+7999999-99-99');
  });
});
