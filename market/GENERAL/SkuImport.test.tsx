import React from 'react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { SkuImport } from './SkuImport';
import { ImportResult } from 'src/java/definitions';

describe('<SkuImport />', () => {
  it('upload as admin', async () => {
    const onImport = jest.fn(() => Promise.resolve({} as ImportResult));
    const app = render(<SkuImport onImport={onImport} title="Импорт скю" name="input-name" />);
    const attach = app.container.getElementsByTagName('input')[0] as HTMLInputElement;
    const file = new File([], 'The one', { type: 'csv' });
    await act(async () => {
      userEvent.upload(attach, file);
    });

    expect(onImport).toHaveBeenLastCalledWith(file, 'MDM_ADMIN');
  });

  it('upload as operator', async () => {
    const onImport = jest.fn(() => Promise.reject(new Error('You are too nice to work')));
    const app = render(<SkuImport onImport={onImport} title="Импорт скю" name="input-name" />);

    const attachBtn = app.getByText('Импорт скю');
    userEvent.hover(attachBtn);

    const overrideControl = screen.getByText('Импортировать с возможностью переопределения');
    userEvent.click(overrideControl);
    const attach = app.container.getElementsByTagName('input')[0] as HTMLInputElement;
    const file = new File([], 'The one', { type: 'csv' });
    await act(async () => {
      userEvent.upload(attach, file);
    });

    expect(onImport).toHaveBeenLastCalledWith(file, 'MDM_OPERATOR');
  });
});
