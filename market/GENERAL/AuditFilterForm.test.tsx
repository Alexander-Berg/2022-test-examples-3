import { createStore } from '@reatom/core';
import userEvent from '@testing-library/user-event';
import { QueryParamsProvider } from '@yandex-market/react-typesafe-query';
import { createBrowserHistory } from 'history';
import React from 'react';
import { act, fireEvent, render, screen } from '@testing-library/react';
import { Provider } from '@reatom/react';
import { MockedApiObject } from '@yandex-market/mbo-test-utils/dist/apiMock/types';

import { setupTestApp, TestApp } from 'src/test/setupTestApp';
import { ROUTES } from 'src/constants/routes';
import { api } from 'src/test/singletons/apiSingleton';
import { AuditFilterForm } from './AuditFilterForm';
import { AuditAction, AuditConfig, DataPage, MboRole } from 'src/java/definitions';
import { AUDIT_ACTIONS, AUDIT_CONFIG, CATEGORIES, USERS } from 'src/test/data';
import { StaffLoginOptionsAtom } from 'src/store/atoms';
import { RestService } from 'src/services';

/*
 Тест был засипан тут: https://st.yandex-team.ru/MBO-40118
 Будет раскипан тут: https://st.yandex-team.ru/MBO-40119
*/

describe.skip('AuditFilterForm:', () => {
  let testApp: TestApp;

  async function resolveLoadDataResponses(api: MockedApiObject<RestService>) {
    await api.configController.getConfig.next().resolve({ login: '', uid: 0, roles: [] });
    await api.auditController.getAuditConfig.next().resolve(AUDIT_CONFIG as AuditConfig);
    await api.categoryTreeController.getCategories.next().resolve(CATEGORIES);
    await api.auditController.getActions.next().resolve(AUDIT_ACTIONS as DataPage<AuditAction>);
    await api.userController.getUsers.next().resolve(
      USERS.map(u => ({
        ...u,
        globalRoles: [MboRole.OPERATOR, MboRole.ADMIN],
        manager: false,
        managerCategories: [],
        subordinates: [],
      }))
    );

    // await api.userRolesController.findRoles.next().resolve([]);
    await api.parameterController.loadRusParamNamesLarge.next().resolve({});
  }

  beforeEach(async () => {
    testApp = setupTestApp({ route: ROUTES.AUDIT.path });
    await act(async () => {
      await resolveLoadDataResponses(api);
    });
  });

  it('should render the audit filter form', async () => {
    expect(screen.getByText('Применить фильтр')).toBeInTheDocument();
  });

  it('should leave no pending api requests', async () => {
    const { api } = testApp;

    expect(api.allActiveRequests).toEqual({});
  });

  it('should filter & sort staffLoginOptions in alphabetic order', async () => {
    const { reatomStore } = testApp;

    const staffLoginOptions = reatomStore.getState(StaffLoginOptionsAtom);

    const loginOptions = [
      { label: 'bolotov', value: 'bolotov' },
      { label: 'robot', value: 'robot' },
    ];

    expect(staffLoginOptions).toEqual(loginOptions);
  });

  it('should render the table', () => {
    expect(screen.getByText('Имя объекта')).toBeInTheDocument();
  });

  it('should render the link button disabled while data is being loaded', () => {
    const links = screen.getAllByText('Применить фильтр');
    expect(links).toHaveLength(1);
    expect(links[0].getAttribute('aria-disabled')).toEqual(true);
  });

  it('should render the data table having N data rows', async () => {
    const { app } = testApp;
    // eslint-disable-next-line testing-library/no-node-access
    expect(app.container.querySelectorAll('tbody tr')).toHaveLength(AUDIT_ACTIONS.items.length);
  });

  it('should render download button', () => {
    const downloadLink = screen.getByText('Скачать CSV');
    expect(downloadLink).toBeInTheDocument();
    // eslint-disable-next-line testing-library/no-node-access
    expect(downloadLink.closest('a')?.getAttribute('href')).toMatch(new RegExp('^/ui/api/audit/csv.*'));
  });

  it('should hide fields in case blue offers is selected', async () => {
    const reatomStore = createStore();
    const history = createBrowserHistory();
    expect(api.allActiveRequests).toStrictEqual({});
    render(
      <QueryParamsProvider history={history}>
        <Provider value={reatomStore}>
          <AuditFilterForm />
        </Provider>
      </QueryParamsProvider>
    );
    await act(async () => {
      await api.auditController.getAuditConfig.next().resolve(AUDIT_CONFIG as AuditConfig);
    });

    // eslint-disable-next-line testing-library/no-node-access
    const select = (await screen.findByText('Тип объекта')).parentElement?.getElementsByTagName('input').item(0);
    expect(select).toBeTruthy();
    const DOWN_ARROW = { keyCode: 40 }; // down arrow key code

    fireEvent.keyDown(select!, DOWN_ARROW);

    userEvent.click(screen.getByText('Синие офферы из MBOC'));

    expect(api.auditController.getPropertyNames.activeRequests()).toHaveLength(0);
    expect(screen.queryByText('Категория')).toBeNull();
    expect(screen.getByText('Синие офферы из MBOC')).not.toBeNull();

    fireEvent.keyDown(select!, DOWN_ARROW);

    userEvent.click(screen.getByText('Значения параметров SKU'));

    expect(api.auditController.getPropertyNames.activeRequests()).toHaveLength(1);
    expect(screen.getByText('Категория')).not.toBeNull();
    expect(screen.queryByText('Синие офферы из MBOC')).toBeNull();
  });
});
