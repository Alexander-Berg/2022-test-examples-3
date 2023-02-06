import * as patterns from '../patterns';

describe('patterns test', () => {
  // number
  test('number 1', () => {
    expect(patterns.number.test('1')).toBe(true);
  });
  test('number -1', () => {
    expect(patterns.number.test('-1')).toBe(true);
  });
  test('number --1', () => {
    expect(patterns.number.test('--1')).toBe(false);
  });
  test('number &-1', () => {
    expect(patterns.number.test('&-1')).toBe(false);
  });

  // numbers
  test('numbers 1, 2,3,-4', () => {
    expect(patterns.numbers.test('1, 2,3,-4')).toBe(true);
  });
  test('numbers -1,-2, -3, 4', () => {
    expect(patterns.numbers.test('-1,-2, -3, 4')).toBe(true);
  });
  test('numbers -1,-2, --3, 4', () => {
    expect(patterns.numbers.test('-1,-2, --3, 4')).toBe(false);
  });

  // phone
  test('phone +7(925)111-22-33', () => {
    expect(patterns.phone.test('+7(925)111-22-33')).toBe(true);
  });
  test('phone +7-925-111-22-33', () => {
    expect(patterns.phone.test('+7-925-111-22-33')).toBe(true);
  });
  test('phone +79251112233', () => {
    expect(patterns.phone.test('+79251112233')).toBe(true);
  });
  test('phone 89251112233', () => {
    expect(patterns.phone.test('89251112233')).toBe(true);
  });
  test('phone 9-8-10-9251112233', () => {
    expect(patterns.phone.test('9-8-10-9251112233')).toBe(true);
  });
  test('phone -79251112233abc', () => {
    expect(patterns.phone.test('-79251112233abc')).toBe(false);
  });
  test('phone +79251$1122335', () => {
    expect(patterns.phone.test('+79251$1122335')).toBe(false);
  });

  // email
  test('email rr@ddd.ru', () => {
    expect(patterns.email.test('rr@ddd.ru')).toBe(true);
  });
  test('email: rr@ddd.ru(_)', () => {
    expect(patterns.email.test('rr@ddd.ru ')).toBe(true);
  });
  test('email: (_)rr@ddd.ru', () => {
    expect(patterns.email.test(' rr@ddd.ru')).toBe(true);
  });
  test('email: (_)rr@ddd.ru(_)', () => {
    expect(patterns.email.test(' rr@ddd.ru ')).toBe(true);
  });
  test('email: (___)rr@ddd.ru(___)', () => {
    expect(patterns.email.test('   rr@ddd.ru   ')).toBe(true);
  });

  // emailsCorp
  test('emailsCorp', () => {
    expect(patterns.emailCorp.test('rr@yandex-team.ru')).toBe(true);
  });
  test('emailCorp with whitespaces', () => {
    expect(patterns.emailCorp.test('  rr@yandex-team.ru  ')).toBe(true);
  });

  // emails
  test('emails', () => {
    expect(patterns.emails.test('rr@ddd.ru, rr@ddd.ru')).toBe(true);
  });
  test('emails with no whitespace', () => {
    expect(patterns.emails.test('rr@ddd.ru,rr@ddd.ru')).toBe(true);
  });
  test('emails with whitespaces', () => {
    expect(patterns.emails.test('  rr@ddd.ru,    rr@ddd.ru  ')).toBe(true);
  });

  // emailsCorp
  test('emailsCorp', () => {
    expect(patterns.emailsCorp.test('rr@yandex-team.ru, rr@yandex-team.ru')).toBe(true);
  });
  test('emailsCorp with whitespaces', () => {
    expect(patterns.emails.test('  rr@yandex-team.ru,    rr@yandex-team.ru  ')).toBe(true);
  });
  test('emailCorp + email', () => {
    expect(patterns.emailsCorp.test('rr@yandex-team.ru, rr@team.ru')).toBe(false);
  });
});
