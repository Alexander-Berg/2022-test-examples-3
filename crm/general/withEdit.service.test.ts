import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { Table, Row } from 'components/Table/Table.types';
import { PartialBy } from 'utils/PartialBy';
import { Form } from 'types/api/form/Form';
import { WithEditService } from './withEdit.service';

const BASE_URL = '/table';
const creatingForm: Form = {
  data: [
    // @ts-ignore
    {
      fields: [
        {
          id: '1',
          type: 'Textarea',
          data: {
            value: 'default text',
          },
        },
      ],
    },
  ],
  meta: {
    createAction: {
      access: 3,
      defaultFields: [
        {
          id: '1',
          type: 'Textarea',
          data: {
            value: 'default text',
          },
        },
      ],
    },
    fieldsVisibility: ['1'],
    fields: [
      {
        id: '1',
        type: 'Textarea',
        title: 'title',
        access: 3,
        isFieldsUpdateNeeded: false,
      },
    ],
  },
};

const creatingFormWithTitle = {
  ...creatingForm,
  meta: { ...creatingForm.meta, title: 'test title' },
};

const defaultInitialTable: Table = {
  data: [
    {
      id: '1',
      fields: [
        {
          id: '1',
          type: 'Textarea',
          data: {
            value: 'text',
          },
        },
      ],
    },
    {
      id: '2',
      fields: [
        {
          id: '2',
          type: 'Textarea',
          data: {
            value: 'text',
          },
        },
      ],
    },
  ],
  meta: {
    fieldsVisibility: ['1'],
    fields: [
      {
        id: '1',
        type: 'Textarea',
        title: 'title',
        access: 3,
        isFieldsUpdateNeeded: false,
      },
    ],
  },
};

const server = setupServer(
  rest.get(`${BASE_URL}/rows/:rowId`, (req, res, ctx) => {
    return res(
      ctx.json<Row>({
        id: req.params.rowId,
        fields: [],
      }),
    );
  }),

  rest.post(`${BASE_URL}/rows/:rowId`, (req, res, ctx) => {
    const row = req.body! as Row & { mockSaveFlag: boolean };
    row.mockSaveFlag = true;
    return res(ctx.json(row));
  }),

  rest.post(`${BASE_URL}/rows`, (req, res, ctx) => {
    const row = req.body! as PartialBy<Row, 'id'>;
    return res(
      ctx.json({
        ...row,
        id: 'rows-mock-id',
      }),
    );
  }),

  rest.get('/creating-form', (req, res, ctx) => {
    return res(ctx.json(creatingForm));
  }),

  rest.post('/creating-form', (req, res, ctx) => {
    return res(
      ctx.json<Row>({
        id: 'creating-form-mock-id',
        fields: [],
      }),
    );
  }),
  rest.get('/creating-form-with-title', (req, res, ctx) => {
    return res(ctx.json(creatingFormWithTitle));
  }),
);

describe('WithEditService', () => {
  let service: WithEditService;

  beforeAll(() => {
    server.listen();
  });

  beforeEach(() => {
    service = new WithEditService(BASE_URL);
  });

  afterAll(() => {
    server.close();
  });

  describe('.addRow', () => {
    const defaultFieldsTable: Table = {
      data: [],
      meta: {
        createAction: {
          access: 3,
          defaultFields: [
            {
              id: '1',
              type: 'Textarea',
              data: {
                value: 'default text',
              },
            },
          ],
        },
        fieldsVisibility: ['1'],
        fields: [
          {
            id: '1',
            type: 'Textarea',
            title: 'title',
            access: 3,
            isFieldsUpdateNeeded: false,
          },
        ],
      },
    };

    describe('when createAction.defaultFields', () => {
      it('sets .editingForm with default fields', () => {
        service.stateManager.setTable(defaultFieldsTable);
        service.addRow();

        expect(service.editingForm).toEqual(creatingForm);
      });
    });
  });

  describe('.editRow', () => {
    const initialTable: Table = {
      data: [
        {
          id: '1',
          fields: [
            {
              id: '1',
              type: 'Textarea',
              data: {
                value: 'text',
              },
            },
          ],
        },
        {
          id: '2',
          url: `${BASE_URL}/editing-form-url`,
          fields: [
            {
              id: '2',
              type: 'Textarea',
              data: {
                value: 'text',
              },
            },
          ],
        },
      ],
      meta: {
        createAction: {
          access: 3,
          defaultFields: [
            {
              id: '1',
              type: 'Textarea',
              data: {
                value: 'default text',
              },
            },
          ],
        },
        fieldsVisibility: ['1'],
        fields: [
          {
            id: '1',
            type: 'Textarea',
            title: 'title',
            access: 3,
            isFieldsUpdateNeeded: false,
          },
        ],
      },
    };

    const row1EditingForm: Form = {
      data: [
        {
          id: '1',
          fields: [
            {
              id: '1',
              type: 'Textarea',
              data: {
                value: 'text',
              },
            },
          ],
        },
      ],
      meta: initialTable.meta,
    };

    const row2EditingForm: Form = {
      data: [],
      meta: {
        fields: [],
        fieldsVisibility: [],
      },
    };

    beforeEach(() => {
      service.stateManager.setTable(initialTable);
      server.use(
        rest.get(`${BASE_URL}/editing-form-url`, (req, res, ctx) => res(ctx.json(row2EditingForm))),
      );
    });

    describe('when row has no url', () => {
      it('sets .editingForm', () => {
        service.editRow('1');

        expect(service.editingForm).toEqual(row1EditingForm);
      });
    });
  });

  describe('.createRow', () => {
    it('creates row via API request', async () => {
      const row: PartialBy<Row, 'id'> = {
        fields: [],
      };

      const savedRow = await service.createRow(row);
      expect(savedRow.id).toBe('rows-mock-id');
    });
  });

  describe('.updateRow', () => {
    it('saves row with POST request to rowId endpoint', async () => {
      const rowToUpdate: Row = {
        id: '1',
        fields: [
          {
            id: '1',
            type: 'Textarea',
            data: {
              value: 'text',
            },
          },
        ],
      };
      const response = await service.updateRow(rowToUpdate);

      expect(response).toEqual({
        ...rowToUpdate,
        mockSaveFlag: true,
      });
    });
  });
  describe('.setUpdateTableAction', () => {
    it('sets CB for update table', () => {
      const cb = jest.fn();
      service.actionsManager.setUpdateTableAction(cb);
      service.actionsManager.updateTableAction();
      expect(cb).toBeCalledTimes(1);
    });
  });
  describe('when table is selectable', () => {
    const initialTable: Table = {
      data: [
        {
          id: '1',
          fields: [
            {
              id: '1',
              type: 'Textarea',
              data: {
                value: 'text',
              },
            },
          ],
        },
        {
          id: '2',
          fields: [
            {
              id: '2',
              type: 'Textarea',
              data: {
                value: 'text',
              },
            },
          ],
        },
      ],
      meta: {
        actions: [
          {
            id: 'someId',
            caption: 'someAction',
            isConfirm: false,
            isForm: false,
            actionUrl: 'test',
            refreshMode: 'ApplyResultData',
            mode: 'Bulk',
            order: 1,
            isReport: false,
          },
        ],
        fieldsVisibility: ['1'],
        fields: [
          {
            id: '1',
            type: 'Textarea',
            title: 'title',
            access: 3,
            isFieldsUpdateNeeded: false,
          },
        ],
      },
    };
    beforeEach(() => {
      service.stateManager.setTable(initialTable);
    });
    it('sets selectedRows', async () => {
      expect(service.selectedRows).not.toBeUndefined();
    });
    describe('.selectAllRows', () => {
      it('selects all rows', async () => {
        service.selectAllRows();
        expect(service.selectedRows?.has('1')).toBeTruthy();
        expect(service.selectedRows?.has('2')).toBeTruthy();
      });
    });
    describe('.unselectAllRows', () => {
      it('unselects all rows', async () => {
        service.selectAllRows();
        service.unselectAllRows();
        expect(service.selectedRows?.size).toEqual(0);
      });
    });
    describe('.toggleSelectRow', () => {
      it('toggles one row', async () => {
        service.toggleSelectRow('1');
        expect(service.selectedRows?.has('1')).toBeTruthy();
        service.toggleSelectRow('1');
        expect(service.selectedRows?.has('1')).toBeFalsy();
      });
    });
  });
  describe('.updateRowsByChangeType', () => {
    const successRows = [
      {
        id: '1',
        changeType: 'Create',
        data: {
          id: '1',
          fields: [
            {
              id: '1',
              type: 'Textarea',
              data: {
                value: 'new text',
              },
            },
          ],
        },
      },
    ];
    describe('when change type is "Create"', () => {
      it('inserts at the end of the table', async () => {
        service.stateManager.setTable(defaultInitialTable);
        service.updateRowsByChangeType(successRows);
        const tableData = service.stateManager.getTableData();
        expect(tableData && tableData[tableData.length - 1]).toEqual(successRows[0].data);
      });
    });
    describe('when change type is "Update"', () => {
      it('updates rows by ids', async () => {
        service.stateManager.setTable(defaultInitialTable);
        const successRowsForUpdate = [{ ...successRows[0], changeType: 'Update' }];
        service.updateRowsByChangeType(successRowsForUpdate);
        const tableData = service.stateManager.getTableData();
        // @ts-ignore
        expect(tableData && tableData[0]?.fields[0]?.data!.value).toEqual('new text');
      });
    });
    describe('when change type is "Delete"', () => {
      it('deletes rows by ids', async () => {
        service.stateManager.setTable(defaultInitialTable);
        const successRowsForUpdate = [{ ...successRows[0], changeType: 'Delete' }];
        service.updateRowsByChangeType(successRowsForUpdate);
        const tableData = service.stateManager.getTableData();
        expect(tableData && tableData.length === 1).toBeTruthy();
      });
    });
  });
  describe('fetchAndSetEditingForm', () => {
    it('sets edit from from remote src', async () => {
      service.stateManager.setTable(defaultInitialTable);
      await service.fetchAndSetEditingForm('/creating-form-with-title');
      expect(service.editingForm).toEqual(creatingFormWithTitle);
      expect(service.formTitle).toEqual('test title');
    });
  });
});
