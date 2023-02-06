import { EObjectFormatter } from '../EObjectFormatter';

describe('EObjectFormatter', () => {
  describe('default formatter use ":"', () => {
    const defaultFormatter = new EObjectFormatter();

    describe('.parse', () => {
      describe('when string correct', () => {
        it('parses with no error', () => {
          expect(defaultFormatter.parse('Mail:1')).toStrictEqual({ etype: 'Mail', eid: 1 });
        });
      });

      describe('when wrong etype', () => {
        it('parses with error', () => {
          expect(() => defaultFormatter.parse('NotExistType:1')).toThrowError(
            /etype is not supported/,
          );
        });
      });

      describe('when wrong eid', () => {
        it('parses with error', () => {
          expect(() => defaultFormatter.parse('Mail:a1')).toThrowError(/eid is not number/);
        });
      });
    });

    describe('.parseSafe', () => {
      describe('when string correct', () => {
        it('returns obj', () => {
          expect(defaultFormatter.parseSafe('Mail:1')).toStrictEqual({ etype: 'Mail', eid: 1 });
        });
      });

      describe('when wrong etype', () => {
        it('returns undefined', () => {
          expect(defaultFormatter.parseSafe('NotExistType:1')).toBeUndefined();
        });
      });
    });

    describe('.format', () => {
      it('returns correct string', () => {
        expect(defaultFormatter.format({ etype: 'Mail', eid: 1 })).toBe('Mail:1');
      });
    });
  });

  describe('custom formatter use "_"', () => {
    const customFormatter = new EObjectFormatter({ delemitter: '_' });

    describe('.parse', () => {
      describe('when string correct', () => {
        it('parses with no error', () => {
          expect(customFormatter.parse('Mail_1')).toStrictEqual({ etype: 'Mail', eid: 1 });
        });
      });

      describe('when wrong etype', () => {
        it('parses with error', () => {
          expect(() => customFormatter.parse('NotExistType_1')).toThrowError(
            /etype is not supported/,
          );
        });
      });

      describe('when wrong eid', () => {
        it('parses with error', () => {
          expect(() => customFormatter.parse('Mail_a1')).toThrowError(/eid is not number/);
        });
      });
    });

    describe('.parseSafe', () => {
      describe('when string correct', () => {
        it('returns obj', () => {
          expect(customFormatter.parseSafe('Mail_1')).toStrictEqual({ etype: 'Mail', eid: 1 });
        });
      });

      describe('when wrong etype', () => {
        it('returns undefined', () => {
          expect(customFormatter.parseSafe('NotExistType_1')).toBeUndefined();
        });
      });
    });

    describe('.format', () => {
      it('returns correct string', () => {
        expect(customFormatter.format({ etype: 'Mail', eid: 1 })).toBe('Mail_1');
      });
    });
  });
});
