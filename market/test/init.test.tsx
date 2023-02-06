import { initApp } from './utils/initApp';
import * as input from '../sample-data';

describe('initialization', () => {
  it('works', () => {
    const data = initApp({ inputData: input });
    expect(data).toBeDefined();
  });
});
