import Deferred from './Deferred'

describe('Deferred', () => {
  let deferred: Deferred<{test: string}>

  beforeEach(() => {
    deferred = new Deferred<{test: string}>()
  })

  test('resolve успешно завершает promise', (done) => {
    const expected = {test: 'test'}

    void deferred.then((result) => {
      expect(result).toBe(expected)
      done()
    })

    deferred.resolve(expected)
  })

  test('reject отклоняет promise', (done) => {
    const expected = new Error('test')

    deferred.then(null, (result) => {
      expect(result).toBe(expected)
      done()
    })

    deferred.reject(expected)
  })
})
