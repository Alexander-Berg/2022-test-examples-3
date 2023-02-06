import React, { FC } from 'react';
import userEvent from '@testing-library/user-event';

import { ActionsCell, ActionsCellProps, InfoAboutNoEditable } from './ActionsCell';
import { simpleMapping, categoryData } from 'src/test/data';
import { ParamMappingRow } from '../../types';
import { setupWithReatom, useDescribeCategoryData } from 'src/test/withReatom';
import { render, waitFor } from '@testing-library/react';

const TestApp: FC<ActionsCellProps> = props => {
  useDescribeCategoryData(categoryData);
  return <ActionsCell {...props} />;
};

describe('ActionsCell', () => {
  test('suggested mapping', () => {
    const openMapping = jest.fn();

    const { app } = setupWithReatom(
      <ActionsCell row={{ ...simpleMapping, id: 0 } as ParamMappingRow} openMapping={openMapping} />
    );

    app.getByText('Не предлагать');
  });

  test('non-editable mapping', () => {
    const openMapping = jest.fn();

    const { app } = setupWithReatom(
      <ActionsCell row={{ ...simpleMapping, editable: false } as ParamMappingRow} openMapping={openMapping} />
    );

    app.getByTestId('non-editable-mapping-info');
  });

  test('edit mapping', async () => {
    const openMapping = jest.fn();

    const { app } = setupWithReatom(
      <TestApp row={{ ...simpleMapping } as ParamMappingRow} openMapping={openMapping} />
    );

    await waitFor(() => {
      userEvent.click(app.getByTitle('Редактировать сопоставление'));
    });

    expect(openMapping).toBeCalled();
  });

  test('remove mapping', async () => {
    const openMapping = jest.fn();

    const { app } = setupWithReatom(
      <TestApp row={{ ...simpleMapping } as ParamMappingRow} openMapping={openMapping} />
    );

    const removeBtn = app.getByTitle('Удалить сопоставление');

    userEvent.click(removeBtn);
  });

  test('reject mapping', async () => {
    const { app } = setupWithReatom(
      <TestApp row={{ ...simpleMapping, id: 0 } as ParamMappingRow} openMapping={jest.fn()} />
    );

    const rejectBtn = app.getByText('Не предлагать');
    userEvent.click(rejectBtn);
  });

  test('InfoAboutNoEditable', async () => {
    const app = render(<InfoAboutNoEditable />);

    app.getByText(new RegExp('Предустановленные правила сформированы на основании данных'));
  });
});
