import {getPath, numberWithSpaces, priceConverter, priceWithCurrency} from './metrics'

describe('metrics', () => {
  describe('calls #getPath()', () => {
    it('create correct svg path', () => {
      const result = getPath(0, 0, 10, 10)
      expect(result).toBe('M 0,0 h 10 v 10 h -10 Z')
    })
  })

  describe('calls #numberWithSpaces()', () => {
    it('expect with 0 value', () => {
      const result = numberWithSpaces(0)

      expect(result).toBe('0')
    })

    it('expect with 1 mlrd value', () => {
      const result = numberWithSpaces(1000000000)

      expect(result).toBe('1 000 000 000')
    })
  })

  describe('call #priceConverter()', () => {
    it('returns price value', () => {
      const result = priceConverter(1000)
      expect(result).toBe('1 000')
    })

    it('returns price with 1m value', () => {
      const result = priceConverter(10000000)
      expect(result).toBe('10 млн')
    })

    it('returns price with 1mlr value', () => {
      const result = priceConverter(100050000000)
      expect(result).toBe('100,05 млрд')
    })

    it('returns price with 1.5m value', () => {
      const result = priceConverter(10050000)
      expect(result).toBe('10,05 млн')
    })

    it('returns price with 1m+ value', () => {
      const result = priceConverter(100001)
      expect(result).toBe('100 001')
    })

    it('returns price with 1.5m+ value', () => {
      const result = priceConverter(1555001)
      expect(result).toBe('1,55 млн')
    })
  })

  describe('call #priceWithCurrency()', () => {
    it('returns fn passed price with $', () => {
      const priceFn = priceWithCurrency('$')
      expect(priceFn(10000)).toBe('10 000 $')
    })

    it('returns passed price with ₸', () => {
      const result = priceWithCurrency('₸', 1050000)
      expect(result).toBe('1,05 млн ₸')
    })

    it('returns passed price with ₸', () => {
      const result = priceWithCurrency('₸', 1000000)
      expect(result).toBe('1 млн ₸')
    })
  })
})
