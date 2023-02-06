import { UniqIdGenerator } from '../UniqIdGenerator';

describe('UniqIdGenerator', () => {
  it('supports prefix (ex, for log info)', () => {
    const uniqIdGenerator = new UniqIdGenerator('name');

    expect(uniqIdGenerator.next()).toMatch(/name/);
  });

  describe('.next', () => {
    describe('when same instance', () => {
      it('returns new value each call', () => {
        const uniqIdGenerator = new UniqIdGenerator();

        expect(uniqIdGenerator.next()).not.toBe(uniqIdGenerator.next());
      });
    });

    describe('when different instances', () => {
      it('returns new value each call', () => {
        const uniqIdGenerator1 = new UniqIdGenerator();
        const uniqIdGenerator2 = new UniqIdGenerator();

        expect(uniqIdGenerator1.next()).not.toBe(uniqIdGenerator2.next());
      });
    });
  });
});
