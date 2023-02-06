import { DataTable } from '@yandex-market/mbo-components';

import { AuditPage } from 'src/pages/audit/AuditPage';
import { setupTestApp } from 'src/test/setupApp';

describe('Audit Page', () => {
  jest.useFakeTimers();

  it('renders page correctly', () => {
    const { app } = setupTestApp(`/audit`);
    expect(app.find(AuditPage)).toHaveLength(1);

    app.find(AuditPage).prop('location').pathname = '/audit';

    const table = app.find(DataTable);
    expect(table).toHaveLength(1);

    expect(table.debug()).toContain('данных');

    app.unmount();
  });
});
