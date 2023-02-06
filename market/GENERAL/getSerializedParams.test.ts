import {getSerializedParams} from './getSerializedParams'

describe('getSerializedParams', () => {
  it('should return string params with serializing an array', () => {
    const result = getSerializedParams({id: ['1', '2', '3'], date: '2020-05-25T15:32:14.815Z', limit: 30})

    expect(result).toBe('id=1,2,3&date=2020-05-25T15:32:14.815Z&limit=30')
  })

  it('should return string params with serializing an array if array length === 1', () => {
    const result = getSerializedParams({id: ['1'], date: '2020-05-25T15:32:14.815Z', limit: 30})

    expect(result).toBe('id=1&date=2020-05-25T15:32:14.815Z&limit=30')
  })

  it('should return string params with serializing an array if array length  === 0', () => {
    const result = getSerializedParams({id: [], date: '2020-05-25T15:32:14.815Z', limit: 30})

    expect(result).toBe('id=[]&date=2020-05-25T15:32:14.815Z&limit=30')
  })

  it('should return string params without data', () => {
    const result = getSerializedParams({
      id: ['1', '2', '3'],
      date: '2020-05-25T15:32:14.815Z',
      limit: 30,
      data: undefined
    })

    expect(result).toBe('id=1,2,3&date=2020-05-25T15:32:14.815Z&limit=30')
  })

  it('should return string if params key === 0', () => {
    const result = getSerializedParams({id: [], date: '2020-05-25T15:32:14.815Z', limit: 0})

    expect(result).toBe('id=[]&date=2020-05-25T15:32:14.815Z&limit=0')
  })

  it('should return empty sting', () => {
    const result = getSerializedParams({})

    expect(result).toBe('')
  })
})
