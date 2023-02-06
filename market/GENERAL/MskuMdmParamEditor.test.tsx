import React from 'react';
import { render, RenderResult } from '@testing-library/react';

import { setupTestProvider } from 'test/setupApp';
import { testMdmMsku, testMskuId } from 'test/data/mdmMskuParameterEditor';
import { testUser } from 'test/data/user';
import { EDITABLE_ROLES } from 'src/constants';
import { App } from 'src/App';

describe('<MskuMdmParamEditor />', () => {
  describe('MskuMdmParamEditor basic tests', () => {
    const { Provider, api, store } = setupTestProvider(`/mdm/msku/${testMskuId}`);
    let app: RenderResult;
    beforeAll(() => {
      app = render(
        <Provider>
          <App />
        </Provider>
      );
      api.mdmMskuUiController.get.next().resolve(testMdmMsku);
      api.configController.currentUser.next().resolve({ ...testUser, roles: EDITABLE_ROLES });
    });

    it('renders', () => {
      expect(app.getAllByText('Редактор параметров MSKU')).toHaveLength(1);
      const saveBtn = app.getByText('Сохранить').parentElement!;
      expect(saveBtn.getAttribute('aria-disabled')).toBe('false');
    });

    it('Get/store msku correctly', () => {
      expect(api.mdmMskuUiController.metadata).toBeCalledTimes(1);
      expect(api.mdmMskuUiController.get).toBeCalledTimes(1);
      expect(store.getState().mdm.msku.msku).toMatchObject(testMdmMsku);
    });
  });

  it('Disables editor with readonly role', () => {
    const { Provider, api } = setupTestProvider(`/mdm/msku/${testMskuId}`);
    const app = render(
      <Provider>
        <App />
      </Provider>
    );
    api.mdmMskuUiController.get.next().resolve(testMdmMsku);
    api.configController.currentUser.next().resolve({ ...testUser, roles: ['UNKNOWN'] });
    const saveBtn = app.getByText('Сохранить').parentElement!;
    expect(saveBtn.getAttribute('aria-disabled')).toBe('true');
  });
});
