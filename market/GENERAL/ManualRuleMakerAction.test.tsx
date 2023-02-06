import React from 'react';

import { setupWithReatom } from 'src/test/withReatom';
import { parameter, shopModel, rules } from 'src/test/data';
import { ManualRuleMakerAction, CHANGE_RULE_TITLE } from './ManualRuleMakerAction';
import { currentUserAtom, initialUser, setCurrentUserAction } from 'src/store/user.atom';
import { Role } from 'src/java/definitions';
import { setAllShopModelsAction, shopModelsListAtom } from 'src/store/shopModels';
import { filterAtom } from '../../store/filter.atom';
import { categoriesByParentHidAtom } from 'src/store/categories';
import userEvent from '@testing-library/user-event';

const defaultAtoms = { currentUserAtom, shopModelsListAtom, filterAtom, categoriesByParentHidAtom };

const defaultActions = [setCurrentUserAction({ ...initialUser, role: Role.ADMIN })];

describe('<ManualRuleMakerAction />', () => {
  test('has rule values', () => {
    window.confirm = jest.fn(() => true);

    const { app, api } = setupWithReatom(<ManualRuleMakerAction parameter={parameter} />, defaultAtoms, [
      ...defaultActions,
      setAllShopModelsAction([{ ...shopModel, marketValues: { [parameter.id]: rules } }]),
    ]);

    const btn = app.getByTitle(CHANGE_RULE_TITLE);

    userEvent.click(btn);

    expect(api.shopModelController.updateModelsV2.activeRequests()).toHaveLength(1);
  });

  test('without rule', () => {
    const { app } = setupWithReatom(<ManualRuleMakerAction parameter={parameter} />, defaultAtoms, defaultActions);
    expect(app.queryByTitle(CHANGE_RULE_TITLE)).not.toBeInTheDocument();
  });

  test('show confirm', () => {
    window.confirm = jest.fn(() => false);

    const { app, api } = setupWithReatom(<ManualRuleMakerAction parameter={parameter} />, defaultAtoms, [
      ...defaultActions,
      setAllShopModelsAction([{ ...shopModel, marketValues: { [parameter.id]: rules } }]),
    ]);

    userEvent.click(app.getByTitle(CHANGE_RULE_TITLE));

    // никаких запросов не должно уходить так как не подтвердили действие
    expect(api.shopModelController.updateModelsV2.activeRequests()).toHaveLength(0);
  });
});
