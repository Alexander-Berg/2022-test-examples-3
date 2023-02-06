import React from 'react';
import { render } from '@testing-library/react';
import { EntityType } from '@yandex-market/cms-editor-core';

import { ToastContainer } from '@/components/toast-container/toast-container';
import { getTestProvider } from '@/test-utils';
import { addToastAction } from '@/store/toasts';
import { setEntitiesDataAction } from '@/pages/document-editor/actions';

describe('<Toast />', () => {
  it('render toast with details', async () => {
    const { Provider, store } = getTestProvider();
    store.dispatch(
      addToastAction({
        type: 'error',
        title: 'Внутренняя ошибка сервера',
        description:
          'Дубликат уникального ключа device=desktop#domain=ru#format=json#nid=54437#type=catalog#zoom=full: , template=device+domain+ds+format+nid+type+zoom, schemaId=5',
        details: {
          code: 'DUPLICATE_DOCUMENTS',
          details: [
            {
              schemaId: 5,
              key: {
                key: 'device=desktop#domain=ru#format=json#nid=54437#type=catalog#zoom=full',
              },
              documentIds: [43578],
            },
          ],
          message:
            'Дубликат уникального ключа device=desktop#domain=ru#format=json#nid=54437#type=catalog#zoom=full: , template=device+domain+ds+format+nid+type+zoom, schemaId=5',
          context: {
            type: 'DOCUMENT',
            id: 159440,
          },
        },
        id: 'toast1',
        isActive: true,
        timestamp: 1638569518559,
      })
    );

    const app = render(
      <Provider>
        <ToastContainer />
      </Provider>
    );

    const link = await app.findByText('43578');
    expect(link.tagName).toBe('A');
    expect(link.attributes.getNamedItem('href')?.value).toBe('/documents/43578/edit');
  });

  it('renders link in toast', () => {
    const { Provider, store } = getTestProvider();
    store.dispatch(
      addToastAction({
        type: 'error',
        title: 'Неожиданное значение параметра',
        description:
          'Публикация невозможна! Не заполнены обязательные поля: {Не указано обязательное значение: Содержимое/Ряд реакт/Столбец реакт/Шапка каталога/Параметры виджета/Ссылка=[Ссылка]}',
        details: {
          code: 'MISSING_REQUIRED_PARAMETER',
          details: [
            {
              nodeId: 105942525,
              fields: ['LINK', 'LINK2'],
            },
            {
              nodeId: 76523456,
            },
          ],
          message:
            'Публикация невозможна! Не заполнены обязательные поля: {Не указано обязательное значение: Содержимое/Ряд реакт/Столбец реакт/Шапка каталога/Параметры виджета/Ссылка=[Ссылка]}',
          context: {
            type: 'DOCUMENT',
            id: 159440,
          },
        },
        id: 'toast2',
        isActive: true,
        timestamp: 1638574096740,
      })
    );
    store.dispatch(
      setEntitiesDataAction({
        [EntityType.Entry]: {
          105942525: {
            id: '105942525',
            type: EntityType.Entry,
            sys: { contentType: { id: 'qwerty' } },
          },
        },
        [EntityType.ContentType]: {
          qwerty: {
            id: 'qwerty',
            properties: { label: 'Узелок' },
            fields: [{ name: 'LINK', properties: { label: 'Ссылка' } }],
          },
        },
      } as any)
    );
    const app = render(
      <Provider>
        <ToastContainer />
      </Provider>
    );

    const node1 = app.getByText('Узелок (Ссылка, LINK2)');
    const node2 = app.getByText('/documents/159440/edit?entry_id=76523456');
    expect(node1.tagName).toBe('A');
    expect(node2.tagName).toBe('A');
    expect(node1.attributes.getNamedItem('href')?.value).toBe('/documents/159440/edit?entry_id=105942525');
    expect(node2.attributes.getNamedItem('href')?.value).toBe('/documents/159440/edit?entry_id=76523456');
  });
});
