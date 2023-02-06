import { DataTable } from '@yandex-market/mbo-components';

import { SettingsPage } from 'src/pages/settings/SettingsPage';
import { setupTestApp } from 'src/test/setupApp';

describe('SettingsPage', () => {
  jest.useFakeTimers();

  it('renders page correctly', () => {
    const { app } = setupTestApp(`/settings`);
    expect(app.find(SettingsPage)).toHaveLength(1);

    app.find(SettingsPage).prop('location').pathname = '/settings';

    const table = app.find(DataTable);
    expect(table).toHaveLength(1);

    expect(table.debug()).toContain('данных');

    app.unmount();
  });
});
