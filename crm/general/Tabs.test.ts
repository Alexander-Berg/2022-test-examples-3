import { Tabs } from './Tabs';

describe('Tabs', () => {
  let tabs = new Tabs();
  beforeEach(() => {
    tabs = new Tabs();
  });

  describe('.go', () => {
    describe('sets current', () => {
      test('case #1', () => {
        tabs.go('search');

        expect(tabs.current).toBe('search');
      });

      test('case #2', () => {
        tabs.go('tree');

        expect(tabs.current).toBe('tree');
      });

      test('case #3', () => {
        tabs.go('selected');

        expect(tabs.current).toBe('selected');
      });
    });
  });

  describe('.reset', () => {
    it('resets all data', () => {
      tabs.go('search');

      tabs.reset();

      expect(tabs.current).toBe('tree');
    });
  });
});
