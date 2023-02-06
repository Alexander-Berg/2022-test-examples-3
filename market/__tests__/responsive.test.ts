import {renderHook} from '@testing-library/react-hooks'
import {useIsDesktop, useIsMobile} from 'core-legacy/hooks/responsive'

const mutableWindow = global as {
  innerWidth: number
}

const DESKTOP_MIN = 1025
const TABLET_MAX = 1024
const TABLET_MIN = 769
const MOBILE_MAX = 768

describe('useIsMobile', () => {
  const makeIsMobileTest = (resolution: number, result: boolean) => makeTest(resolution, () => useIsMobile(), result)

  test('возвращает false для десктопного разрешения', makeIsMobileTest(DESKTOP_MIN, false))
  test('возвращает false для минимального планшетного разрешения', makeIsMobileTest(TABLET_MIN, false))
  test('возвращает false для максимального планшетного разрешения', makeIsMobileTest(TABLET_MAX, false))
  test('возвращает true для мобильного разрешения', makeIsMobileTest(MOBILE_MAX, true))

  describe('с withTablet=true', () => {
    const makeWithTabletTest = (resolution: number, result: boolean) =>
      makeTest(resolution, () => useIsMobile(true), result)

    test('возвращает false для десктопного разрешения', makeWithTabletTest(DESKTOP_MIN, false))
    test('возвращает true для минимального планшетного разрешения', makeWithTabletTest(TABLET_MIN, true))
    test('возвращает true для максимального планшетного разрешения', makeWithTabletTest(TABLET_MAX, true))
    test('возвращает true для мобильного разрешения', makeWithTabletTest(MOBILE_MAX, true))
  })
})

describe('useIsDesktop', () => {
  const makeIsDesktopTest = (resolution: number, result: boolean) => makeTest(resolution, () => useIsDesktop(), result)

  test('возвращает true для десктопного разрешения', makeIsDesktopTest(DESKTOP_MIN, true))
  test('возвращает true для минимального планшетного разрешения', makeIsDesktopTest(TABLET_MIN, true))
  test('возвращает true для максимального планшетного разрешения', makeIsDesktopTest(TABLET_MAX, true))
  test('возвращает false для мобильного разрешения', makeIsDesktopTest(MOBILE_MAX, false))

  describe('с withoutTablet=true', () => {
    const makeWithoutTabletTest = (resolution: number, result: boolean) =>
      makeTest(resolution, () => useIsDesktop(true), result)

    test('возвращает true для десктопного разрешения', makeWithoutTabletTest(DESKTOP_MIN, true))
    test('возвращает false для минимального планшетного разрешения', makeWithoutTabletTest(TABLET_MIN, false))
    test('возвращает false для максимального планшетного разрешения', makeWithoutTabletTest(TABLET_MAX, false))
    test('возвращает false для мобильного разрешения', makeWithoutTabletTest(MOBILE_MAX, false))
  })
})

function makeTest(resolution: number, hookUsage: () => boolean, expectedResult: boolean) {
  return () => {
    mutableWindow.innerWidth = resolution
    expect(renderHook(hookUsage).result.current).toBe(expectedResult)
  }
}
