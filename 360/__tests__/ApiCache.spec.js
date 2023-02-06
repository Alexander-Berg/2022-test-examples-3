import ApiCache from '../ApiCache';

describe('ApiCache', () => {
  describe('key', () => {
    test('должен преобразовывать url и body в ключ', () => {
      const cache = new ApiCache();
      const key = cache.key('/get-events', {
        layerId: [1, 2],
        from: 'from',
        to: 'to'
      });

      expect(key).toEqual('url=/get-events&from="from"&layerId=[1,2]&to="to"');
    });

    test('должен работать без переданного body', () => {
      const cache = new ApiCache();
      const key = cache.key('/get-events');

      expect(key).toEqual('url=/get-events');
    });
  });

  describe('set', () => {
    test('должен записывать данные в кэш', () => {
      const cache = new ApiCache();
      cache.set('/get-events', {from: 'from'}, {events: []});

      expect(cache.cache).toEqual({
        [cache.key('/get-events', {from: 'from'}, {events: []})]: {events: []}
      });
    });
  });

  describe('get', () => {
    test('должен возвращать данные из кэша', () => {
      const cache = new ApiCache();
      cache.set('/get-events', {form: 'form'}, {events: []});

      expect(cache.get('/get-events', {form: 'form'})).toEqual({
        events: []
      });
    });
  });

  describe('has', () => {
    test('должен возвращать true, если данные есть в кэше', () => {
      const cache = new ApiCache();
      cache.set('/get-events', {form: 'form'}, {events: []});

      expect(cache.has('/get-events', {form: 'form'})).toBe(true);
    });

    test('должен возвращать false, если данных нет в кэше', () => {
      const cache = new ApiCache();

      expect(cache.has('/get-events', {form: 'form'})).toBe(false);
    });
  });
});
