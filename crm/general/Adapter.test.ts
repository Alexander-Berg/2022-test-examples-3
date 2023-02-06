import Adapter from './Adapter';

describe('Adapter', () => {
  describe('.copy', () => {
    it('returns new instance with the same adapters', () => {
      const adapter = new Adapter(() => 'default' as string);
      adapter.add('1', () => '1');
      adapter.add('2', () => '2');
      adapter.add('3', () => '3');

      const adapterCopy = adapter.copy();

      expect(adapterCopy.get('1')()).toBe('1');
      expect(adapterCopy.get('2')()).toBe('2');
      expect(adapterCopy.get('3')()).toBe('3');
      expect(adapterCopy.getOrDefault('non-existing key')()).toBe('default');
    });
  });
});
