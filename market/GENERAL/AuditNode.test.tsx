import React from 'react';
import { act, render, screen } from '@testing-library/react';

import { ROUTES } from 'src/constants';
import AUDIT_DATA from 'src/test/data/task-audit/task-audit-data.json';
import { AuditView } from 'src/java/definitions';
import { setupTestProvider } from 'src/test/setupTestProvider';
import { TaskAuditTable } from 'src/pages/TaskAudit/TaskAuditTable';
import { api } from 'src/test/singletons/apiSingleton';

describe('AuditNode::', () => {
  it('should render at least the first table header column', async () => {
    const Provider = setupTestProvider({ route: `${ROUTES.TASK_AUDIT.path}?id=1062808` });
    render(
      <Provider>
        <TaskAuditTable />
      </Provider>
    );

    await act(async () => {
      api.billingAuditController.getAuditForTask.next().resolve((AUDIT_DATA as unknown) as AuditView);
    });

    expect(screen.getAllByText('Задание')).toBeTruthy();
  });
});
