import React, { FC } from 'react';
import userEvent from '@testing-library/user-event';

import { setupWithReatom, useDescribeCategoryData } from 'src/test/withReatom';
import {
  ExportStateTogglerProps,
  ExportStateToggler,
  RESTRICT_EXPORT_TEXT,
  CAN_EXPORT_TEXT,
  NEED_FIX_ERROR,
} from './ExportStateToggler';
import { categoryData, shopModel } from 'src/test/data';
import { modelCanBeExportedFilter, modelRestrictedToExportFilter, modelFixErrorFilter } from 'src/filters/modelFilters';
import { screen } from '@testing-library/react';

const TestApp: FC<ExportStateTogglerProps> = props => {
  useDescribeCategoryData(categoryData);

  return <ExportStateToggler {...props} />;
};

describe('ExportModelStateToggler', () => {
  test('CAN_EXPORT_TEXT -> RESTRICT_EXPORT_TEXT', () => {
    const onUpdateFilter = jest.fn();

    const { app } = setupWithReatom(<TestApp filters={modelCanBeExportedFilter} updateFilter={onUpdateFilter} />);

    // по фильтру таких товаров должно быть
    userEvent.click(app.getByText(CAN_EXPORT_TEXT));
    userEvent.click(screen.getByText(RESTRICT_EXPORT_TEXT));

    expect(onUpdateFilter).toHaveBeenCalledTimes(1);
    expect(onUpdateFilter).toHaveBeenLastCalledWith(modelRestrictedToExportFilter);
  });

  test('RESTRICT_EXPORT_TEXT -> CAN_EXPORT_TEXT', () => {
    const onUpdateFilter = jest.fn();
    const { app } = setupWithReatom(
      <TestApp models={[shopModel]} filters={modelRestrictedToExportFilter} updateFilter={onUpdateFilter} />
    );

    userEvent.click(app.getByText(RESTRICT_EXPORT_TEXT));
    userEvent.click(screen.getByText(CAN_EXPORT_TEXT));

    expect(onUpdateFilter).toHaveBeenCalledTimes(1);
    expect(onUpdateFilter).toHaveBeenLastCalledWith(modelCanBeExportedFilter);
  });

  test('RESTRICT_EXPORT_TEXT -> NEED_FIX_ERROR', () => {
    const onUpdateFilter = jest.fn();
    const { app } = setupWithReatom(
      <TestApp models={[shopModel]} filters={modelRestrictedToExportFilter} updateFilter={onUpdateFilter} />
    );

    userEvent.click(app.getByText(RESTRICT_EXPORT_TEXT));
    userEvent.click(screen.getByText(NEED_FIX_ERROR));

    expect(onUpdateFilter).toHaveBeenCalledTimes(1);
    expect(onUpdateFilter).toHaveBeenLastCalledWith(modelFixErrorFilter);
  });
});
