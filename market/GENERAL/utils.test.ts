import {fixPennyPrecision, formatCurrencyWithPenny} from './utils'

describe('feature-penny', () => {
  describe('fixPennyPrecision', () => {
    test('.1 + .2', () => {
      const currencyValue = 0.1 + 0.2 // is 0.30000000000000004
      const expected = 0.3
      const actual = fixPennyPrecision(currencyValue)

      expect(actual).toEqual(expected)
    })
    test('1.40 * 165', () => {
      const currencyValue = 1.4 * 165 // is 230.99999999999997
      const expected = 231
      const actual = fixPennyPrecision(currencyValue)

      expect(actual).toEqual(expected)
    })
  })
  describe('formatCurrencyWithPenny', () => {
    test('100', () => {
      const currencyValue = 100
      const expected = '100.00'
      const actual = formatCurrencyWithPenny(currencyValue)

      expect(actual).toEqual(expected)
    })
    test('100.25', () => {
      const currencyValue = 100.25
      const expected = '100.25'
      const actual = formatCurrencyWithPenny(currencyValue)

      expect(actual).toEqual(expected)
    })
    test('.25', () => {
      const currencyValue = 0.25
      const expected = '0.25'
      const actual = formatCurrencyWithPenny(currencyValue)

      expect(actual).toEqual(expected)
    })
  })
})
