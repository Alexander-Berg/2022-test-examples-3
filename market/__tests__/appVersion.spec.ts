import { APP_VERSION } from '@/constants/app';
import { isLastVersion } from '@/utils/appVersion';

jest.mock('@/constants/app');

const testLowerVersion = '1.5';
const testBiggerVersion1 = '10.8';
const testBiggerVersion2 = '1.8';

describe('Тестирование функции проверки последней версии', () => {
  it('Текущая версия выше', () => {
    expect(isLastVersion(testLowerVersion)).toEqual(false);
  });

  it('Текущая версия не ниже', () => {
    expect(isLastVersion(APP_VERSION)).toEqual(true);
  });

  it('Текущая версия ниже 1', () => {
    expect(isLastVersion(testBiggerVersion1)).toEqual(false);
  });

  it('Текущая версия ниже 2', () => {
    expect(isLastVersion(testBiggerVersion2)).toEqual(false);
  });
});
