import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { act } from '@testing-library/react';
import { ReactWrapper } from 'enzyme';

import { AssortmentBindingAdminPage } from './AssortmentBindingAdminPage';
import { DemandType } from 'src/java/definitions-replenishment';
import { Select, SelectOption, Attach } from 'src/components';
import { users } from 'src/test/data/replenishmentUsers';
import { UserSelect } from '../replenishment/components';
import { setupTestApp } from 'src/test/setupApp';
import { Bindings } from './components';
import Api from 'src/Api';
import { waitForPromises } from 'src/test/utils/utils';

let app: ReactWrapper;
let api: MockedApiObject<Api>;

describe('AssortmentBindingAdminPage', () => {
  beforeEach(() => {
    ({ app, api } = setupTestApp('/assortment-binding-admin'));
    api.replenishmentUserController.getAll.next().resolve(users);
    app.update();
  });

  it('renders', () => {
    expect(app.find(AssortmentBindingAdminPage)).toHaveLength(1);
  });

  it('Should upload excel 1P', async () => {
    const file = new File(['(⌐□_□)'], 'myFile.xlsx', { type: 'application/vnd.ms-excel' });

    const userSelect = app.find(AssortmentBindingAdminPage).find(Bindings).at(0).find(UserSelect).find(Select);

    const selectOptions: SelectOption[] = userSelect.first().prop('options');
    expect(selectOptions).toHaveLength(users.length + 1);

    act(() => {
      userSelect.first().props().onChange(selectOptions[2]);
      app.update();
    });

    await waitForPromises();
    app.update();

    app
      .find(AssortmentBindingAdminPage)
      .find(Bindings)
      .at(0)
      .find(Attach)
      .at(0)
      .find('input')
      .simulate('change', { target: { files: [file], value: '' } });

    expect(api.userAssortmentController.checkLogParamsByLogin).toBeCalledTimes(1);

    api.userAssortmentController.checkLogParamsByLogin.next().resolve();

    await waitForPromises();
    app.update();

    expect(api.userAssortmentController.addFromExcelByLogin).toBeCalledTimes(1);
    expect(api.userAssortmentController.addFromExcelByLogin).toBeCalledWith(
      file,
      selectOptions[2].value,
      DemandType.TYPE_1P
    );
  });

  it('Should upload excel 3P', async () => {
    const file = new File(['(⌐□_□)'], 'myFile.xlsx', { type: 'application/vnd.ms-excel' });

    const userSelect = app.find(AssortmentBindingAdminPage).find(Bindings).at(1).find(UserSelect).find(Select);

    const selectOptions: SelectOption[] = userSelect.first().prop('options');
    expect(selectOptions).toHaveLength(users.length + 1);

    await act(() => {
      userSelect.first().props().onChange(selectOptions[2]);
      app.update();
    });

    await waitForPromises();
    app.update();

    app
      .find(AssortmentBindingAdminPage)
      .find(Bindings)
      .at(1)
      .find(Attach)
      .at(0)
      .find('input')
      .simulate('change', { target: { files: [file], value: '' } });

    expect(api.userAssortmentController.checkLogParamsByLogin).toBeCalledTimes(1);

    api.userAssortmentController.checkLogParamsByLogin.next().resolve();

    await waitForPromises();
    app.update();

    expect(api.userAssortmentController.addFromExcelByLogin).toBeCalledTimes(1);
    expect(api.userAssortmentController.addFromExcelByLogin).toBeCalledWith(
      file,
      selectOptions[2].value,
      DemandType.TYPE_3P
    );
  });
});
