import { isCookieAllowedByGDPR } from 'neo/lib/cookies/isCookieAllowedByGDPR';
import { ECOOKIE_TYPE } from 'neo/types/cookies';

const testCases = [
  {
    gdpr: '0',
    cookieType: ECOOKIE_TYPE.TECH,
    isAllowed: true,
  },
  {
    gdpr: '0',
    cookieType: ECOOKIE_TYPE.ANALYTIC,
    isAllowed: true,
  },
  {
    gdpr: '0',
    cookieType: ECOOKIE_TYPE.OTHER,
    isAllowed: true,
  },
  // Тесты для значения gdpr = '1'
  {
    gdpr: '1',
    cookieType: ECOOKIE_TYPE.TECH,
    isAllowed: true,
  },
  {
    gdpr: '1',
    cookieType: ECOOKIE_TYPE.ANALYTIC,
    isAllowed: false,
  },
  {
    gdpr: '1',
    cookieType: ECOOKIE_TYPE.OTHER,
    isAllowed: false,
  },
  // Тесты для значения gdpr = '2'
  {
    gdpr: '2',
    cookieType: ECOOKIE_TYPE.TECH,
    isAllowed: true,
  },
  {
    gdpr: '2',
    cookieType: ECOOKIE_TYPE.ANALYTIC,
    isAllowed: true,
  },
  {
    gdpr: '2',
    cookieType: ECOOKIE_TYPE.OTHER,
    isAllowed: false,
  },
  // Тесты для значения gdpr = '3'
  {
    gdpr: '3',
    cookieType: ECOOKIE_TYPE.TECH,
    isAllowed: true,
  },
  {
    gdpr: '3',
    cookieType: ECOOKIE_TYPE.ANALYTIC,
    isAllowed: false,
  },
  {
    gdpr: '3',
    cookieType: ECOOKIE_TYPE.OTHER,
    isAllowed: true,
  },
];

testCases.forEach((testCase) => {
  test(`Для значения gdpr ${testCase.gdpr} кука ${ECOOKIE_TYPE[testCase.cookieType]} ${testCase.isAllowed ? 'разрешена' : 'запрещена'}`, () => {
    expect(isCookieAllowedByGDPR(testCase.cookieType, testCase.gdpr))
      .toEqual(testCase.isAllowed);
  });
});
