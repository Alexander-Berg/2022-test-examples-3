import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { AssortmentBindingPage } from './AssortmentBindingPage';
import { TestCmApp } from 'src/test/setupApp';
import Api from 'src/Api';
import { Bindings } from './components';
import { DemandType } from 'src/java/definitions-replenishment';
import { Attach } from 'src/components';
import { waitForPromises } from 'src/test/utils/utils';

let app: TestCmApp;
let api: MockedApiObject<Api>;

beforeEach(() => {
  app = new TestCmApp('/assortment-binding');
  ({ api } = app);
});

describe('AssortmentBindingPage', () => {
  it('renders', () => {
    expect(app.find(AssortmentBindingPage)).toHaveLength(1);
  });

  it('Should upload excel 1P', async () => {
    const file = new File(['(⌐□_□)'], 'myFile.xlsx', { type: 'application/vnd.ms-excel' });

    app
      .find(AssortmentBindingPage)
      .find(Bindings)
      .at(0)
      .find(Attach)
      .at(0)
      .find('input')
      .simulate('change', { target: { files: [file], value: '' } });

    expect(api.userAssortmentController.checkLogParams).toBeCalledTimes(1);

    api.userAssortmentController.checkLogParams.next().resolve();

    await waitForPromises();
    app.update();

    expect(api.userAssortmentController.addFromExcel).toBeCalledTimes(1);
    expect(api.userAssortmentController.addFromExcel).toBeCalledWith(file, DemandType.TYPE_1P);
  });

  it('Should upload excel 1P', async () => {
    const file = new File(['(⌐□_□)'], 'myFile.xlsx', { type: 'application/vnd.ms-excel' });

    app
      .find(AssortmentBindingPage)
      .find(Bindings)
      .at(1)
      .find(Attach)
      .at(0)
      .find('input')
      .simulate('change', { target: { files: [file], value: '' } });

    expect(api.userAssortmentController.checkLogParams).toBeCalledTimes(1);

    api.userAssortmentController.checkLogParams.next().resolve();

    await waitForPromises();
    app.update();

    expect(api.userAssortmentController.addFromExcel).toBeCalledTimes(1);
    expect(api.userAssortmentController.addFromExcel).toBeCalledWith(file, DemandType.TYPE_3P);
  });
});
