import getApiHost from '../getApiHost';

describe('getApiHost', () => {
  it('should be null for /index.html', () => {
    expect(getApiHost('/index.html')).toBe(null);
  });

  it('should be "/space1" for /space1/index.html', () => {
    expect(getApiHost('/space1/index.html')).toBe('/space1');
  });

  it('should be "/space1/space2" for /space1/space2/index.html', () => {
    expect(getApiHost('/space1/space2/index.html')).toBe('/space1/space2');
  });
});
