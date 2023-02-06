import React from 'react';
import { render, fireEvent, screen, getByRole, waitFor } from '@testing-library/react';
import { Subject } from 'rxjs';
import { withEmitting } from './withEmitting';
import { Tip as TipComponent } from './Tip';
import { Provider, Store, Tree, Tip as TipStore } from '../../State';
import { TargetMeta } from '../../types';

describe('Tip', () => {
  describe('withEmitting', () => {
    describe('on rate buttons click', () => {
      const Tip = withEmitting(TipComponent);
      const tree = new Tree();
      const emitter = new Subject<{
        event: string;
        payload: TargetMeta;
      }>();
      tree.setup({
        byId: {
          0: {
            id: 0,
            name: '0',
            isLeaf: true,
          },
        },
        root: [0],
        valueAsTree: {
          0: {},
        },
        highlightPath: [0],
      });
      const tip = new TipStore();
      tip.setExternalHandler((id) => Promise.resolve('html document ' + id));
      const store = new Store({
        tree,
        emitter,
        tip,
      });

      it('emits like event', async () => {
        const sub = jest.fn();
        emitter.subscribe(sub);
        tip.load(123);
        render(
          <Provider store={store}>
            <Tip />
          </Provider>,
        );

        await waitFor(() => {
          const likeButton = getByRole(screen.getByRole('note'), 'button', { name: 'like' });
          expect(likeButton).toBeEnabled();
          fireEvent.click(likeButton);
        });

        expect(sub).toHaveBeenCalledWith({
          event: 'like',
          payload: {
            categoryId: 0,
          },
        });
      });

      it('emits dislike event', async () => {
        const sub = jest.fn();
        emitter.subscribe(sub);
        tip.load(123);
        render(
          <Provider store={store}>
            <Tip />
          </Provider>,
        );

        await waitFor(() => {
          fireEvent.click(getByRole(screen.getByRole('note'), 'button', { name: 'dislike' }));
        });

        expect(sub).toHaveBeenCalledWith({
          event: 'dislike',
          payload: {
            categoryId: 0,
          },
        });
      });
    });
  });
});
