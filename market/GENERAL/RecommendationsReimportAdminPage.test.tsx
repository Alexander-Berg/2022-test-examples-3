import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { act } from '@testing-library/react';

import { RecommendationsReimportAdminPage } from './RecommendationsReimportAdminPage';
import { Button, Select, SelectOptionGeneric, TextInput } from 'src/components';
import { RecommendationsImportEvent } from 'src/java/definitions-replenishment';
import { TestCmApp } from 'src/test/setupApp';
import Api from 'src/Api';
import { waitForPromises } from 'src/test/utils/utils';

let app: TestCmApp;
let api: MockedApiObject<Api>;

beforeEach(() => {
  app = new TestCmApp('/recommendations-reimport-admin');
  ({ api } = app);
});

describe('RecommendationsReimportAdminPage', () => {
  it('renders', () => {
    expect(app.find(RecommendationsReimportAdminPage)).toHaveLength(1);
  });

  it('reimport 1P', async () => {
    const selectType = app.find(RecommendationsReimportAdminPage).find(Select);
    const selectOptions: SelectOptionGeneric<RecommendationsImportEvent>[] = selectType.first().prop('options');

    act(() => {
      selectType.first().props().onChange(selectOptions[0]);
      app.update();
    });

    await waitForPromises();
    app.update();

    act(() => {
      app.find(RecommendationsReimportAdminPage).find(Button).simulate('click');
    });

    expect(api.recommendationsImportController.reimportRecommendations).toBeCalledTimes(1);
    expect(api.recommendationsImportController.reimportRecommendations).toBeCalledWith({
      recommendationsImportEvent: RecommendationsImportEvent.TYPE_1P,
      tablePath: '',
    });
  });

  it('reimport 3P', async () => {
    const selectType = app.find(RecommendationsReimportAdminPage).find(Select);
    const selectOptions: SelectOptionGeneric<RecommendationsImportEvent>[] = selectType.first().prop('options');

    act(() => {
      selectType.first().props().onChange(selectOptions[1]);
      app.update();
    });

    await waitForPromises();
    app.update();

    act(() => {
      app.find(RecommendationsReimportAdminPage).find(Button).simulate('click');
    });

    expect(api.recommendationsImportController.reimportRecommendations).toBeCalledTimes(1);
    expect(api.recommendationsImportController.reimportRecommendations).toBeCalledWith({
      recommendationsImportEvent: RecommendationsImportEvent.TYPE_3P,
      tablePath: '',
    });
  });

  it('reimport inter warehouse', async () => {
    const selectType = app.find(RecommendationsReimportAdminPage).find(Select);
    const selectOptions: SelectOptionGeneric<RecommendationsImportEvent>[] = selectType.first().prop('options');

    act(() => {
      selectType.first().props().onChange(selectOptions[3]);
      app.update();
    });

    await waitForPromises();
    app.update();

    act(() => {
      app.find(RecommendationsReimportAdminPage).find(Button).simulate('click');
    });

    expect(api.recommendationsImportController.reimportRecommendations).toBeCalledTimes(1);
    expect(api.recommendationsImportController.reimportRecommendations).toBeCalledWith({
      recommendationsImportEvent: RecommendationsImportEvent.INTER_WAREHOUSE,
      tablePath: '',
    });
  });

  it('reimport 1P with table path', async () => {
    const selectType = app.find(RecommendationsReimportAdminPage).find(Select);
    const selectOptions: SelectOptionGeneric<RecommendationsImportEvent>[] = selectType.first().prop('options');

    act(() => {
      selectType.first().props().onChange(selectOptions[0]);
    });

    await waitForPromises();
    app.update();

    act(() => {
      app
        .find(RecommendationsReimportAdminPage)
        .find(TextInput)
        .find('input')
        .simulate('change', { target: { value: '//testpath' } });
    });

    app.update();

    act(() => {
      app.find(RecommendationsReimportAdminPage).find(Button).simulate('click');
    });

    expect(api.recommendationsImportController.reimportRecommendations).toBeCalledTimes(1);
    expect(api.recommendationsImportController.reimportRecommendations).toBeCalledWith({
      recommendationsImportEvent: RecommendationsImportEvent.TYPE_1P,
      tablePath: '//testpath',
    });
  });
});
