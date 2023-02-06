import React from 'react';
import { act, fireEvent, render } from '@testing-library/react';

import { AuditList } from './AuditList';
import { setupTestProvider } from 'test/setupApp';
import { AuditListModel } from './audit-list.model';
import { DataPage, MdmAutoMarkupHistoryEntry } from 'src/java/definitions';

const TEST_HISTORY_DATA = {
  items: [
    {
      id: 6,
      name: 'Разметка жидкости [2021-09-21 17:13]',
      queryId: 2,
      yqlQueryBody:
        "use hahn;\n\nSELECT \n    10263999 as SupplierId,\n    '4630009189756' as ShopSku,\n    'Австрия' as manufacturerCountry\nfrom `//home/market/users/sbye/automarkup`\nlimit 1\n",
      startedAt: '2021-09-21T13:13:56.630188Z',
      finishedAt: '2021-09-21T13:14:07.336024Z',
      state: 'SUCCESS',
      message: null,
    },
    {
      id: 5,
      name: 'Разметка жидкости [2021-09-21 17:07]',
      queryId: 2,
      yqlQueryBody:
        'use hahn;\n\nSELECT \n    SupplierId,\n    ShopSku,\n    manufacturerCountry\nfrom `//home/market/users/sbye/automarkup`\nlimit 100\n',
      startedAt: '2021-09-21T13:07:35.076807Z',
      finishedAt: '2021-09-21T13:08:00.371330Z',
      state: 'ERROR',
      message: 'Ошибка импорта файла: Разметка_жидкости_2021-09-21_17:07.xlsx',
    },
    {
      id: 4,
      name: 'Разметка жидкости[2021-09-21 15:48]',
      queryId: 2,
      yqlQueryBody:
        'use hahn;\n\nSELECT \n    SupplierId,\n    ShopSku,\n    manufacturerCountry\nfrom `//home/market/users/sbye/automarkup`\nlimit 100\n',
      startedAt: '2021-09-21T11:48:57.336669Z',
      finishedAt: '2021-09-21T11:50:22.081067Z',
      state: 'ERROR',
      message: 'Ошибка импорта файла: Разметка_жидкости2021-09-21_15:48.xlsx',
    },
    {
      id: 3,
      name: 'Разметка жидкости[2021-09-21 15:45]',
      queryId: 2,
      yqlQueryBody:
        'use hahn;\n\nSELECT \n    SupplierId,\n    ShopSku,\n    manufacturerCountry\nfrom `//home/market/users/sbye/automarkup`\nlimit 100\n',
      startedAt: '2021-09-21T11:45:54.299693Z',
      finishedAt: '2021-09-21T11:46:44.749253Z',
      state: 'ERROR',
      message: 'Unsupported field: HourOfDay',
    },
    {
      id: 2,
      name: 'Разметка жидкости[2021-09-21 15:39]',
      queryId: 2,
      yqlQueryBody: null,
      startedAt: '2021-09-21T11:39:37.136327Z',
      finishedAt: null,
      state: 'RUNNING',
      message: null,
    },
    {
      id: 1,
      name: 'Разметка жидкости[2021-09-21 15:26]',
      queryId: 2,
      yqlQueryBody:
        'use hahn;\n\nSELECT \n    SupplierId,\n    ShopSku,\n    manufacturerCountry\nfrom `//home/market/users/sbye/automarkup`\nlimit 100\n',
      startedAt: '2021-09-21T11:26:26.517051Z',
      finishedAt: '2021-09-21T11:28:39.537239Z',
      state: 'SUCCESS',
      message:
        'Unable to obtain LocalDateTime from TemporalAccessor: 2021-09-21T11:26:26.517051Z of type java.time.Instant',
    },
  ],
  totalCount: 6,
};

describe('<AuditList />', () => {
  it('renders without errors', async () => {
    const { Provider, api } = setupTestProvider();
    const auditListModel = new AuditListModel(api);
    const app = render(
      <Provider>
        <div style={{ width: 1000 }}>
          <AuditList model={auditListModel} />
        </div>
      </Provider>
    );

    await act(async () => {
      api.mdmAutoMarkupController.findHistory.next().resolve(TEST_HISTORY_DATA as DataPage<MdmAutoMarkupHistoryEntry>);
    });

    const testText = 'Австрия';
    const yqlBodyCell = await app.findByText(testText, { exact: false });
    fireEvent.mouseEnter(yqlBodyCell);
  });
});
