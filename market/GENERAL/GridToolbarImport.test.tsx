import React from 'react';
import { render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { GridToolbarImport } from './GridToolbarImport';

describe('<GridToolbarImport />', () => {
  it('simulate file selection', async () => {
    const onImport = jest.fn(() => Promise.resolve());
    const app = render(<GridToolbarImport onImport={onImport} />);
    const button = app.getByText('Импорт');
    let fileInput = app.container.querySelector('input[type="file"]') as HTMLInputElement;
    const clickHandler = jest.spyOn(fileInput, 'click');
    const testFile = new File([], 'testik');

    userEvent.click(button);
    expect(clickHandler).toBeCalledTimes(1);

    userEvent.upload(fileInput, testFile);
    expect(onImport).lastCalledWith(testFile);

    // file input should be unmounted because its react key has been changed
    expect(fileInput).not.toBeInTheDocument();

    fileInput = app.container.querySelector('input[type="file"]') as HTMLInputElement;
    await waitFor(jest.fn()); // wait until import promise resolves
    userEvent.upload(fileInput, null as unknown as File);
    expect(onImport).lastCalledWith(null);
  });
});
