import { waitFor } from '@testing-library/react';
import { Store } from '../Store';
import { createById, createCategory } from '../../../utils';

describe('Search', () => {
  let store = new Store();
  let search = store.search;
  beforeEach(() => {
    store = new Store();
    search = store.search;
    store.runReactions();
  });

  afterEach(() => {
    store.stopReactions();
  });

  describe('.handler', () => {
    describe('sets .text', () => {
      test('case #1', () => {
        search.handler('test');

        expect(search.text).toBe('test');
      });

      test('case #2', () => {
        search.handler('');

        expect(search.text).toBe('');
      });
    });

    describe('when .externalHandler returns result', () => {
      it('filters root ids', async () => {
        const testText = 'external handler test';
        const mockExternalHandler = jest.fn(() =>
          Promise.resolve({
            resultIds: [1, 2, 3],
            highlightRangesById: {},
          }),
        );
        store.tree.byId = createById([createCategory(1, [createCategory(2), createCategory(3)])]);
        search.setExternalHandler(mockExternalHandler);
        search.handler(testText);

        await waitFor(() => {
          expect(search.resultIds).toHaveLength(2);
          expect(search.resultIds).toContain(2);
          expect(search.resultIds).toContain(3);
        });
      });
    });

    describe('when .text.length > 2', () => {
      it('goes to search tab', async () => {
        const testText = 'goes to search tab';
        search.handler(testText);

        await waitFor(() => {
          expect(store.tabs.current).toBe('search');
        });
      });

      it('starts to call .externalHandler', async () => {
        const testText = 'starts to call .searchHandler';
        const mockExternalHandler = jest.fn(() =>
          Promise.resolve({
            resultIds: [1, 2, 3],
            highlightRangesById: {},
          }),
        );
        search.setExternalHandler(mockExternalHandler);
        search.handler(testText);

        await waitFor(() => {
          expect(mockExternalHandler).lastCalledWith(testText);
        });
      });

      describe('when switch from search tab', () => {
        beforeEach(async () => {
          search.handler('12345678');

          await waitFor(() => {
            expect(store.tabs.current).toBe('search');
          });

          store.tabs.go('tree');
        });

        it('clears the text', async () => {
          await waitFor(() => {
            expect(search.text).toHaveLength(0);
          });
        });
      });
    });

    describe('when .text.length was decreased to 2', () => {
      beforeEach(async () => {
        const mockExternalHandler = jest.fn(() =>
          Promise.resolve({
            resultIds: [1, 2, 3, 4],
            highlightRangesById: {},
          }),
        );
        store.tree.byId = createById([
          createCategory(1, [createCategory(2), createCategory(3), createCategory(4)]),
        ]);
        search.setExternalHandler(mockExternalHandler);
        search.handler('123456');

        await waitFor(() => {
          expect(search.resultIds).toHaveLength(3);
        });
      });

      it('goes to previous tab', () => {
        const previousTab = store.tabs.previous;
        search.handler('1');

        expect(store.tabs.current).toBe(previousTab);
      });

      it('sets .isLoading', async () => {
        search.handler('1');

        await waitFor(() => {
          expect(search.loading.state).toBeTruthy();
        });
      });

      it('resets .resultIds', async () => {
        search.handler('1');

        await waitFor(() => {
          expect(search.resultIds).toHaveLength(0);
        });
      });
    });
  });

  describe('.reset', () => {
    it('resets all data', () => {
      search.loading.off();
      search.text = '1234';
      search.resultIds = [1, 2, 3];

      search.reset();

      expect(search.loading.state).toBeTruthy();
      expect(search.text).toBe('');
      expect(search.resultIds).toHaveLength(0);
    });
  });
});
