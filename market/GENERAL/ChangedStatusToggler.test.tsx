import React, { FC } from 'react';
import userEvent from '@testing-library/user-event';
import { screen } from '@testing-library/react';

import { setupWithReatom, useDescribeCategoryData, useDescribeAllModels } from 'src/test/withReatom';
import {
  ChangedStatusTogglerProps,
  ChangedStatusToggler,
  RESTRICT_CHANGE_TEXT,
  CAN_CHANGE_TEXT,
} from './ChangedStatusToggler';
import { categoryData, shopModel } from 'src/test/data';

const TestApp: FC<ChangedStatusTogglerProps> = props => {
  useDescribeCategoryData(categoryData);
  useDescribeAllModels([shopModel]);

  return <ChangedStatusToggler {...props} />;
};

describe('ChangedStatusToggler', () => {
  test('CHANGEABLE -> UNCHANGEABLE', () => {
    const onUpdateFilter = jest.fn();
    const { app } = setupWithReatom(<TestApp filters={{ canChangeCategory: true }} updateFilter={onUpdateFilter} />);

    userEvent.click(app.getByText(CAN_CHANGE_TEXT));
    userEvent.click(screen.getByText(RESTRICT_CHANGE_TEXT));

    expect(onUpdateFilter).toHaveBeenLastCalledWith({ canChangeCategory: false });
  });

  test('UNCHANGEABLE -> CHANGEABLE', () => {
    const onUpdateFilter = jest.fn();
    const { app } = setupWithReatom(<TestApp filters={{ canChangeCategory: false }} updateFilter={onUpdateFilter} />);

    userEvent.click(app.getByText(RESTRICT_CHANGE_TEXT));
    userEvent.click(screen.getByText(CAN_CHANGE_TEXT));

    expect(onUpdateFilter).toHaveBeenLastCalledWith({ canChangeCategory: true });
  });
});
