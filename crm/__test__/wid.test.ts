import wid from '../wid';

const mockMath = Object.create(global.Math);
mockMath.random = () => 0.999999999;
global.Math = mockMath;

describe('wid', () => {
  test('empty string', () => {
    expect(wid('')).toBe('?_wid=999999999');
  });

  test('url with ?', () => {
    expect(wid('view/list?workplace=phone')).toBe('view/list?workplace=phone&_wid=999999999');
  });

  test('url without ?', () => {
    expect(wid('view')).toBe('view?_wid=999999999');
  });

  test('min value should be 100000000', () => {
    const mockMath = Object.create(global.Math); // eslint-disable-line no-shadow
    mockMath.random = () => 0;
    global.Math = mockMath;
    expect(wid('view')).toBe('view?_wid=100000000');
  });
});
