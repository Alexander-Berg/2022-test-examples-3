import React from 'react';
import { render } from '@testing-library/react';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { setupTestProvider } from 'test/setupApp';
import { CustomsCommCodeLite, Cis, MdmModificationInfo, MdmGoodGroup } from 'src/java/definitions';
import { resolveCommonRequests } from 'test/commonResolve';
import Api from 'src/Api';
import { App } from 'src/App';

const PAGE_URL = '/mdm/tnved-categories';

function resolveAllCtegoryRequest(api: MockedApiObject<Api>, categories: CustomsCommCodeLite[] = []) {
  api.customsCommCodeController.loadAll.next().resolve(categories);
}

const categories = [
  {
    id: 1,
    parentId: 0,
    code: '333',
    title: 'parent node',
    userComment: '',
    honestSign: {
      cis: Cis.NONE,
    },
    mercury: {
      cis: Cis.NONE,
    },
    documentLink: '',
    modificationInfo: {} as MdmModificationInfo,
  },
  {
    id: 2,
    parentId: 1,
    code: '555',
    title: 'children node',
    userComment: '',
    honestSign: {
      cis: Cis.NONE,
    },
    mercury: {
      cis: Cis.NONE,
    },
    documentLink: '',
    modificationInfo: {} as MdmModificationInfo,
  },
];

function resolveGetAllGoodsGroupRequest(api: MockedApiObject<Api>, goodGroups: MdmGoodGroup[]) {
  api.customsCommCodeController.getAllGoodsGroup.next().resolve(goodGroups);
}

describe('TN VED Categories Page', () => {
  it('renders page correctly', () => {
    const { Provider, api } = setupTestProvider(PAGE_URL);
    const app = render(
      <Provider>
        <App />
      </Provider>
    );
    expect(app.getAllByText('Категории ТН ВЭД')).toHaveLength(1);
    expect(app.getAllByText('Категории')).toHaveLength(1);
    expect(app.getAllByText('Добавление категории')).toHaveLength(1);

    resolveAllCtegoryRequest(api);
    resolveCommonRequests(api);
    resolveGetAllGoodsGroupRequest(api, []);

    expect(api.allActiveRequests).toEqual({});
  });

  xit('render category with children', () => {
    const { Provider, api } = setupTestProvider(PAGE_URL);
    const app = render(
      <Provider>
        <App />
      </Provider>
    );
    resolveCommonRequests(api);
    expect(app.getAllByText('Категории')).toHaveLength(1);
    resolveAllCtegoryRequest(api, categories);

    app.getByText(categories[0].title, { exact: false });
  });
});
