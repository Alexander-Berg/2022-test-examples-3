import React, { FC } from 'react';
import { act, waitFor } from '@testing-library/react';

import { setupWithReatom, useDescribeMapping } from 'src/test/withReatom';
import { parameter, rules, shopModel } from 'src/test/data';
import { MarketHeaderParameterEditor } from './MarketHeaderParameterEditor';
import { currentUserAtom, initialUser, setCurrentUserAction } from '../../../../store/user.atom';
import { Role } from '../../../../java/definitions';
import { filteredModelsAtom } from '../../store';
import { setAllShopModelsAction, shopModelsAtom, shopModelsListAtom } from '../../../../store/shopModels';
import userEvent from '@testing-library/user-event';

const user = {
  ...initialUser,
  role: Role.MANAGER,
};
const defaultAtoms = { currentUserAtom, filteredModelsAtom, shopModelsListAtom, shopModelsAtom };
const defaultActions = [setAllShopModelsAction([{ ...shopModel, marketValues: { [parameter.id]: rules } }])];

const TestApp: FC<React.ComponentProps<typeof MarketHeaderParameterEditor>> = ({ mappings = [], ...props }) => {
  useDescribeMapping(mappings);

  return <MarketHeaderParameterEditor {...props} />;
};

const CHANGE_RULE_TITLE = 'Заменить все правила на ручные значения';

describe('<MarketHeaderParameterEditor />', () => {
  test('Отображаем кнопку замены правил на ручные значения только для ролей = ADMIN и OPERATOR', async () => {
    const { app, reatomStore } = setupWithReatom(<TestApp parameter={parameter} onClose={jest.fn()} />, defaultAtoms, [
      setCurrentUserAction(user),
      ...defaultActions,
    ]);

    // не отображаем для обычного пользователя
    await waitFor(() => {
      expect(app.queryByText(CHANGE_RULE_TITLE)).toBeFalsy();
    });

    act(() => {
      reatomStore.dispatch(setCurrentUserAction({ ...user, role: Role.OPERATOR }));
    });
    await app.findByText(CHANGE_RULE_TITLE);

    act(() => {
      reatomStore.dispatch(setCurrentUserAction({ ...user, role: Role.ADMIN }));
    });
    await app.findByText(CHANGE_RULE_TITLE);
  });

  test('Кликаем на кнопку замены правил, должен уходить запрос на изменение товаров', async () => {
    window.confirm = jest.fn(() => true);

    const { app, api } = setupWithReatom(<TestApp parameter={parameter} mappings={[]} />, defaultAtoms, [
      setCurrentUserAction({ ...user, role: Role.OPERATOR }),
      ...defaultActions,
    ]);

    const btn = await app.findByText(CHANGE_RULE_TITLE);
    userEvent.click(btn);

    const saveBtn = app.getByText('Применить ко всем по фильтру', { exact: false });
    userEvent.click(saveBtn);

    expect(api.shopModelController.activeRequests()).toHaveLength(1);
  });

  test('Если для выбранного параметра в товарах нет правил, то кнопка замены на ручные значения не отображается ', () => {
    const { app } = setupWithReatom(<TestApp parameter={parameter} />, defaultAtoms, [
      setCurrentUserAction({ ...user, role: Role.OPERATOR }),
      setAllShopModelsAction([shopModel]),
    ]);

    expect(app.queryByTitle(CHANGE_RULE_TITLE)).toBeFalsy();
  });
});
