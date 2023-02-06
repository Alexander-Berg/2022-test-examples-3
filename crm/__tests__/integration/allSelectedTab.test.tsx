import React from 'react';
import { waitFor, render, screen, fireEvent, getByRole } from '@testing-library/react';
import { Store, Provider } from '../../State';
import { Widget } from '../../components/Widget';
import { Modal } from '../../components/Modal';
import { ById } from '../../types';
import { createById, createCategory } from '../../utils';

describe('all selected tab behavior', () => {
  let byId: ById;
  let root: number[];

  beforeEach(() => {
    byId = createById([
      createCategory(0, [
        createCategory(3),
        createCategory(4),
        createCategory(5, [createCategory(6, [createCategory(7)])]),
      ]),
    ]);
    root = [0];

    HTMLElement.prototype.scrollIntoView = jest.fn();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('changes category values', async () => {
    const handleLoad = () =>
      Promise.resolve({
        byId,
        root,
        highlightPath: [],
        valueAsTree: {
          0: {
            3: {},
            4: {},
            5: {
              6: {
                7: {},
              },
            },
          },
        },
      });
    const store = new Store();

    render(
      <Provider store={store}>
        <Widget targetMeta={{ id: 1, type: 'Mail' }} onLoad={handleLoad} />
        <Modal />
      </Provider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Разметить' }));

    await waitFor(() => {
      screen.getByRole('dialog');
      screen.getByRole('list');
    });

    await waitFor(() => {
      expect(screen.getAllByRole('listitem')).toHaveLength(3);
    });

    fireEvent.click(getByRole(screen.getAllByRole('listitem')[0], 'checkbox'));
    await waitFor(() => {
      expect(screen.getAllByRole('listitem')).toHaveLength(2);
    });

    fireEvent.click(getByRole(screen.getAllByRole('listitem')[0], 'checkbox'));
    await waitFor(() => {
      expect(screen.getAllByRole('listitem')).toHaveLength(1);
    });

    fireEvent.click(getByRole(screen.getAllByRole('listitem')[0], 'checkbox'));
    await waitFor(() => {
      expect(screen.queryAllByRole('listitem')).toHaveLength(0);
    });
  });
});
