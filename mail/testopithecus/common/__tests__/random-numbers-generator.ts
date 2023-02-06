import { range } from '../ys/ys';

describe('default oauth service', () => {
  it('should get oauth token', (done) => {
    const min = 0
    const max = 1000
    console.log('[')
    for (const i of range(0, 1000)) {
      const num = Math.floor(Math.random() * (max - min + 1)) + min
      console.log(`${num},`)
    }
    console.log(']')
    done()
  });
});
