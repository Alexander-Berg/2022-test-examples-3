import { getComplexity, DEFAULT_COMPLEXITY, COMPLEXITIES } from '../complexity';
import { changeUrl } from './shared';

describe('complexity', () => {
  test('should use complexity from query parameter', () => {
    for (let complexity of COMPLEXITIES) {
      changeUrl(`localhost?complexity=${complexity}`);

      expect(getComplexity()).toBe(complexity);
    }
  });

  test('should return default complexity on invalid query parameter', () => {
    changeUrl('localhost?complexity=invalid_complexity');

    expect(getComplexity()).toBe(DEFAULT_COMPLEXITY);
  });

  test('should return default complexity on missing query parameter', () => {
    changeUrl('localhost');

    expect(getComplexity()).toBe(DEFAULT_COMPLEXITY);
  });

  test('should show error on invalid query parameter', () => {
    const fn = jest.spyOn(global.console, 'error').mockImplementation();

    changeUrl('localhost?complexity=invalid_complexity');
    getComplexity();

    expect(fn).toBeCalled();
  });

  test('should show error on missing query parameter', () => {
    const fn = jest.spyOn(global.console, 'error').mockImplementation();

    changeUrl('localhost');
    getComplexity();

    expect(fn).toBeCalled();
  });
});
