import { mergeMeta, parse } from '../utils/dataConvertor';
import bdata from './backend.json';
import fdata from './frontend.json';
import meta from './meta.json';

describe('nesty json test', () => {
  test('convert to backend', () => {
    const result = mergeMeta(fdata, meta);
    expect(result).toEqual(bdata);
  });

  test('convert to frontend', () => {
    const toFront = parse(bdata, meta);
    expect(toFront).toEqual(fdata);
  });
});
