import { rest } from 'msw';
import { Table as ITable } from '../../Table.types';

export const handlers = [
  rest.get('/filledTable', (req, res, ctx) => {
    const table: ITable = {
      meta: {
        fieldsVisibility: ['1', '2'],
        fields: [
          {
            id: '1',
            sortable: false,
            type: 'Link',
            title: 'First column',
            access: 3,
            isFieldsUpdateNeeded: false,
          },
          {
            id: '2',
            sortable: false,
            type: 'Link',
            title: 'Second column',
            access: 3,
            isFieldsUpdateNeeded: false,
          },
        ],
      },
      data: [
        {
          id: '1',
          fields: [
            {
              id: '1',
              type: 'Link',
              data: {
                text: {
                  value: 'LinkText',
                },
                link: 'yandex.ru',
              },
            },
            {
              id: '2',
              type: 'Link',
              data: {
                text: {
                  value: 'LinkText',
                },
                link: 'yandex.ru',
              },
            },
          ],
        },
        {
          id: '2',
          fields: [
            {
              id: '1',
              type: 'Link',
              data: {
                text: {
                  value: 'LinkText',
                },
                link: 'yandex.ru',
              },
            },
            {
              id: '2',
              type: 'Link',
              data: {
                text: {
                  value: 'LinkText',
                },
                link: 'yandex.ru',
              },
            },
          ],
        },
      ],
    };

    return res(ctx.json(table));
  }),

  rest.get('/emptyTable', (req, res, ctx) => {
    const table: ITable = {
      meta: {
        fieldsVisibility: ['1', '2'],
        fields: [
          {
            id: '1',
            sortable: false,
            type: 'Link',
            title: 'First column',
            access: 3,
            isFieldsUpdateNeeded: false,
          },
          {
            id: '2',
            sortable: false,
            type: 'Link',
            title: 'Second column',
            access: 3,
            isFieldsUpdateNeeded: false,
          },
        ],
      },
      data: [],
    };

    return res(ctx.json(table));
  }),

  rest.get('/notTableInterface', (req, res, ctx) => {
    return res(
      ctx.json({
        data: {
          accounts: [],
        },
      }),
    );
  }),
];
