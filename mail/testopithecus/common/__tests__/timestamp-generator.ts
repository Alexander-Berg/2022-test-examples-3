import { range } from '../ys/ys'

describe('timestamp generator', () => {
  it('should generate', (done) => {
    console.log('[')
    const start = new Date(2019, 11, 6, 1, 21)
    for (const i of range(0, 100)) {
      const d = new Date(start)
      d.setDate(d.getDate() - i)
      console.log(`'${d.toISOString()}',`)
    }
    console.log(']')
    done()
  })
})
