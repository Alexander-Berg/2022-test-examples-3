import React from 'react';
import userEvent from '@testing-library/user-event';

import { shopModel } from 'src/test/data';
import { ASSORTMENT_TITLE, ModelDetails } from './ModelDetails';
import { setupWithReatom } from '../../test/withReatom';
import { DEBUG_TITLE } from '../Links/CheckModelLink';
import { setCurrentUserAction } from '../../store/user.atom';
import { Role } from '../../java/definitions';
import { act } from 'react-dom/test-utils';

describe('ModelDetails', () => {
  test('render parameters', async () => {
    const onClose = jest.fn();

    const { app } = setupWithReatom(<ModelDetails model={shopModel} onClose={onClose} />);

    app.getByText(ASSORTMENT_TITLE);

    userEvent.click(app.getByText('Закрыть'));

    expect(onClose.mock.calls.length).toEqual(1);
  });

  test('shows debug only for admin', async () => {
    const { app, reatomStore } = setupWithReatom(<ModelDetails model={shopModel} onClose={jest.fn()} />);

    let link = await app.queryByTitle(DEBUG_TITLE);
    expect(link).toBeNull();

    act(() => {
      reatomStore.dispatch(setCurrentUserAction({ login: 'test', role: Role.ADMIN, userId: 1, shops: [] }));
    });

    link = await app.queryByTitle(DEBUG_TITLE);
    expect(link).not.toBeNull();
  });
});
