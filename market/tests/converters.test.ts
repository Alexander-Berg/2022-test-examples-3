import { decodeDate, encodeDate, encodeDateTime, QueryTypes } from '..';

describe('converters', () => {
  describe('QueryTypes.number', () => {
    it('should decode number values', () => {
      expect(QueryTypes.number.decode('0')).toBe(0);
      expect(QueryTypes.number.decode('1')).toBe(1);
      expect(QueryTypes.number.decode('invalid')).toBeUndefined();
      expect(QueryTypes.number.decode(undefined)).toBeUndefined();
    });

    it('should encode number values', () => {
      expect(QueryTypes.number.encode(1)).toBe('1');
      expect(QueryTypes.number.encode(0)).toBe('0');
      expect(QueryTypes.number.encode(undefined)).toBeUndefined();
    });
  });

  describe('QueryTypes.string', () => {
    it('should decode string values', () => {
      expect(QueryTypes.string.decode('some')).toBe('some');
      expect(QueryTypes.string.decode('')).toBeUndefined();
      expect(QueryTypes.string.decode(undefined)).toBeUndefined();
    });

    it('should encode string values', () => {
      expect(QueryTypes.string.encode('some')).toBe('some');
      expect(QueryTypes.string.encode('')).toBe('');
      expect(QueryTypes.string.encode(undefined)).toBeUndefined();
    });
  });

  describe('QueryTypes.boolean', () => {
    it('should decode boolean values', () => {
      expect(QueryTypes.boolean.decode('true')).toBe(true);
      expect(QueryTypes.boolean.decode('1')).toBe(true);
      expect(QueryTypes.boolean.decode('false')).toBe(false);
      expect(QueryTypes.boolean.decode('0')).toBe(false);
      expect(QueryTypes.boolean.decode('FalSe')).toBeUndefined();
      expect(QueryTypes.boolean.decode('invalid')).toBeUndefined();
      expect(QueryTypes.boolean.decode(undefined)).toBeUndefined();
    });

    it('should encode boolean values', () => {
      expect(QueryTypes.boolean.encode(true)).toBe('1');
      expect(QueryTypes.boolean.encode(false)).toBe('0');
      expect(QueryTypes.boolean.encode(undefined)).toBeUndefined();
    });
  });

  describe('QueryTypes.json', () => {
    it('should decode json values', () => {
      expect(QueryTypes.json.decode('{}')).toEqual({});
      expect(QueryTypes.json.decode('{"a":1,"b":true,"c":"some"}')).toEqual({
        a: 1,
        b: true,
        c: 'some',
      });
      expect(QueryTypes.json.decode('{a:1}')).toBeUndefined();
      expect(QueryTypes.json.decode('invalid')).toBeUndefined();
      expect(QueryTypes.json.decode(undefined)).toBeUndefined();
    });

    it('should encode json values', () => {
      expect(QueryTypes.json.encode({})).toBe('{}');
      expect(
        QueryTypes.json.encode({
          a: 1,
          b: true,
          c: 'some',
        })
      ).toBe('{"a":1,"b":true,"c":"some"}');
      expect(QueryTypes.json.encode(undefined)).toBeUndefined();
    });
  });

  describe('Date', () => {
    it('should decode iso dates', () => {
      expect(decodeDate('2012-12-12')).toEqual(new Date('2012-12-12'));
      expect(decodeDate('2020-04-16T09:57:50.283Z')).toEqual(new Date('2020-04-16T09:57:50.283Z'));
      expect(decodeDate('segwefgwer2crq235v 2')).toBeUndefined();
      expect(decodeDate(undefined as any)).toBeUndefined();
    });

    it('should encode Date values', () => {
      expect(encodeDate(new Date('2012-12-12'))).toEqual('2012-12-12');
      expect(encodeDate(new Date('2020-04-16T09:57:50.283Z'))).toEqual('2020-04-16');
      expect(encodeDate(new Date('Invalid date'))).toBeUndefined();
      expect(encodeDate(undefined as any)).toBeUndefined();
    });

    it('should encode DateTime values', () => {
      expect(encodeDateTime(new Date('2012-12-12'))).toEqual('2012-12-12T00:00:00.000Z');
      expect(encodeDateTime(new Date('2020-04-16T09:57:50.283Z'))).toEqual('2020-04-16T09:57:50.283Z');
      expect(encodeDate(new Date('Invalid date'))).toBeUndefined();
      expect(encodeDateTime(undefined as any)).toBeUndefined();
    });
  });

  describe('QueryTypes.arrayOf', () => {
    const arrayOf = QueryTypes.arrayOf(QueryTypes.number);

    it('should decode array values', () => {
      expect(arrayOf.decode([])).toEqual([]);
      expect(arrayOf.decode(['-1', '0', '1'])).toEqual([-1, 0, 1]);
      expect(arrayOf.decode('invalid')).toEqual([]);
      expect(arrayOf.decode(undefined)).toBeUndefined();
    });

    it('should encode array values', () => {
      expect(arrayOf.encode([-1, 0, 1])).toEqual(['-1', '0', '1']);
      expect(arrayOf.encode([])).toEqual([]);
      expect(arrayOf.encode(undefined)).toBeUndefined();
    });
  });
});
