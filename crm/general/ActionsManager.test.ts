import { rest } from 'msw';
import { setupServer } from 'msw/node';
import { waitFor } from '@testing-library/react';
import { configure } from 'mobx';
import { formatFormValuesToDataSet } from 'components/FormByScheme/utils/formatFormValuesToDataSet';
import { ActionsManager } from './ActionsManager';
import { ActionResponse } from './ActionsManager.types';
import { actions } from './stub/actions';
import { WithEditService } from '../../withEdit.service';
import { StateManager } from '../StateManager';
jest.mock('../../withEdit.service');
jest.mock('components/FormByScheme/utils/formatFormValuesToDataSet', () => {
  return {
    formatFormValuesToDataSet: jest.fn((row, form, id) => {
      if (id) {
        return { ...row, id };
      }
      return row;
    }),
  };
});
configure({ safeDescriptors: false });

const editServiceInstance = new WithEditService();
editServiceInstance.stateManager = ({
  getTableData: jest.fn(),
} as unknown) as StateManager;
const rowMock = {
  id: '1',
  fields: [
    {
      data: { value: '73048684' },
      id: 'id',
      type: 'Text',
    },
  ],
};

const actionResponseMock: ActionResponse = {
  results: [
    { id: '1', isSuccess: false, data: rowMock, message: 'ok', changeType: 'Update' },
    { id: '2', isSuccess: false, data: rowMock, message: 'ok', changeType: 'Update' },
    { id: '3', isSuccess: true, data: rowMock, message: 'ok', changeType: 'Update' },
  ],
};

const requestListener = jest.fn();

const server = setupServer(
  rest.post(`/action1`, (req, res, ctx) => {
    const { body } = req;
    requestListener(body);
    return res(ctx.json(actionResponseMock));
  }),
  rest.post(`/action2`, (req, res, ctx) => {
    return res(ctx.json(actionResponseMock));
  }),
  rest.post(`/action2`, (req, res, ctx) => {
    return res(ctx.json(actionResponseMock));
  }),
);

describe('ActionsManager', () => {
  let actionsManager: ActionsManager;

  beforeAll(() => {
    server.listen();
  });

  beforeEach(() => {
    actionsManager = new ActionsManager(editServiceInstance);
  });

  afterAll(() => {
    server.close();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('.setActionsCollection', () => {
    it('sets action collection for instance', () => {
      actionsManager.setActionCollection(actions);
      expect(actionsManager.getActionById(actions[0].id)).toEqual(actions[0]);
    });
  });
  describe('.actionResponseHandler', () => {
    describe('if refreshMode is "ReloadGrid"', () => {
      it('reloads all grid', () => {
        const gridReloadAction = jest.fn();
        const actionWithReload = actions[0];
        actionsManager.setActionCollection(actions);
        actionsManager.setUpdateTableAction(gridReloadAction);
        actionsManager.setCurrentAction(actionWithReload);
        actionsManager.actionResponseHandler(actionResponseMock);
        expect(gridReloadAction).toBeCalledTimes(1);
      });
    });
    describe('if refreshMode is "ApplyResultData"', () => {
      it('updates rows by change type', () => {
        const actionWithReload = actions[2];
        actionsManager.setActionCollection(actions);
        actionsManager.setCurrentAction(actionWithReload);
        actionsManager.actionResponseHandler(actionResponseMock);
        expect(editServiceInstance.updateRowsByChangeType).toBeCalledTimes(1);
        expect(editServiceInstance.updateRowsByChangeType).toBeCalledWith([
          actionResponseMock.results[2],
        ]);
      });
    });
  });
  describe('.doActionRequest', () => {
    it('makes request with correct args', () => {
      const currentAction = actions[0];
      actionsManager.setActionCollection(actions);
      actionsManager.setCurrentAction(currentAction);
      actionsManager.doActionRequest([rowMock]);
      waitFor(() => expect(requestListener).toBeCalledTimes(1));
      waitFor(() => expect(requestListener).toBeCalledWith({ args: [rowMock] }));
    });
    it('returns expected results', async () => {
      const currentAction = actions[0];
      actionsManager.setActionCollection(actions);
      actionsManager.setCurrentAction(currentAction);
      const spy = jest.spyOn(actionsManager, 'actionResponseHandler');
      await actionsManager.doActionRequest([rowMock]);
      expect(spy).toBeCalledWith(actionResponseMock);
    });
    describe('when isReport is true', () => {
      it('calls .actionResponseHandler with request results', async () => {
        const currentAction = actions[1];
        actionsManager.setActionCollection(actions);
        actionsManager.setCurrentAction(currentAction);
        await actionsManager.doActionRequest([rowMock]);
        expect(editServiceInstance.setEditingReport).toBeCalledTimes(1);
      });
    });
  });
  describe('.doAction', () => {
    it('calls depending mode action', async () => {
      const currentAction = actions[0];
      actionsManager.setActionCollection(actions);
      const spy = jest.spyOn(actionsManager, 'simpleAction');
      await actionsManager.doAction(currentAction.id);
      expect(spy).toBeCalledTimes(1);
    });
  });

  describe('.bulkAction', () => {
    describe('when isForm === true', () => {
      it('fetches and sets the editing form', async () => {
        const currentAction = actions[2];
        actionsManager.setActionCollection(actions);
        await actionsManager.bulkAction(currentAction);
        expect(editServiceInstance.fetchAndSetEditingForm).toBeCalledTimes(1);
        expect(editServiceInstance.fetchAndSetEditingForm).toBeCalledWith(currentAction.actionUrl);
      });
    });
    describe('when isForm === false', () => {
      it('does action request with selected rows', async () => {
        const currentAction = { ...actions[2], isForm: false };
        actionsManager.setActionCollection(actions);
        // @ts-ignore
        editServiceInstance.stateManager.getTableData.mockReturnValueOnce([{ id: '1' }]);
        actionsManager.doActionRequest = jest.fn();
        editServiceInstance.selectedRows = new Set(['1']);
        await actionsManager.bulkAction(currentAction);
        expect(actionsManager.doActionRequest).toBeCalledTimes(1);
        expect(actionsManager.doActionRequest).toBeCalledWith([{ id: '1' }]);
      });
    });
  });
  describe('.simpleAction', () => {
    describe('when isForm === true', () => {
      it('fetches and sets the editing form', async () => {
        const currentAction = { ...actions[0], isForm: true };
        actionsManager.setActionCollection(actions);
        await actionsManager.simpleAction(currentAction);
        expect(editServiceInstance.fetchAndSetEditingForm).toBeCalledTimes(1);
        expect(editServiceInstance.fetchAndSetEditingForm).toBeCalledWith(currentAction.actionUrl);
      });
    });
    describe('when isForm === false', () => {
      it('does action request without params', async () => {
        const currentAction = actions[0];
        actionsManager.setActionCollection(actions);
        actionsManager.doActionRequest = jest.fn();
        await actionsManager.simpleAction(currentAction);
        expect(actionsManager.doActionRequest).toBeCalledTimes(1);
        expect(actionsManager.doActionRequest).toBeCalledWith();
      });
    });
  });
  describe('.rowAction', () => {
    describe('when isForm === true', () => {
      it('fetches and sets the editing form', async () => {
        const currentAction = { ...actions[3], isForm: true };
        actionsManager.setActionCollection(actions);
        const rowId = '1';
        await actionsManager.rowAction(currentAction, rowId);
        expect(editServiceInstance.fetchAndSetEditingForm).toBeCalledTimes(1);
        expect(editServiceInstance.fetchAndSetEditingForm).toBeCalledWith(
          `${currentAction.actionUrl}?id=${rowId}`,
        );
      });
    });
    describe('when isForm === false', () => {
      it('does action request with rowId', async () => {
        const currentAction = actions[3];
        actionsManager.setActionCollection(actions);
        actionsManager.doActionRequest = jest.fn();
        const rowId = '1';
        await actionsManager.rowAction(currentAction, rowId);
        expect(actionsManager.doActionRequest).toBeCalledTimes(1);
        expect(actionsManager.doActionRequest).toBeCalledWith(undefined, { id: rowId });
      });
    });
  });
  describe('.prepareRowsToActionRequest', () => {
    describe('when action type === "Bulk" ', () => {
      it('returns updated selected rows', async () => {
        const currentAction = actions[1];
        actionsManager.setActionCollection(actions);
        const rowMock = {
          fields: [{ id: '1', type: 'Text', data: { value: 'test text' } }],
        };
        editServiceInstance.selectedRows = new Set(['1']);
        const rows = actionsManager.prepareRowsToActionRequest(currentAction, rowMock);
        expect(rows).toEqual([{ id: '1', ...rowMock }]);
      });
    });
    describe('when action type === "Simple" ', () => {
      it('returns rows without id', async () => {
        const currentAction = actions[0];
        actionsManager.setActionCollection(actions);
        const rowMock = {
          fields: [{ id: '1', type: 'Text', data: { value: 'test text' } }],
        };
        const rows = actionsManager.prepareRowsToActionRequest(currentAction, rowMock);
        expect(rows).toEqual([rowMock]);
        expect(formatFormValuesToDataSet).toBeCalledWith(
          rowMock,
          editServiceInstance.editingForm,
          undefined,
        );
      });
    });
    describe('when action type === "Row" ', () => {
      it('returns rows with rowId from the form', async () => {
        const currentAction = actions[3];
        actionsManager.setActionCollection(actions);
        const rowMock = {
          fields: [{ id: '1', type: 'Text', data: { value: 'test text' } }],
        };
        actionsManager.setEditingRowId('1');
        // @ts-ignore
        editServiceInstance.getEditingFormId.mockReturnValueOnce('1');
        const rows = actionsManager.prepareRowsToActionRequest(currentAction, rowMock);
        expect(rows).toEqual([{ id: '1', ...rowMock }]);
        expect(formatFormValuesToDataSet).toBeCalledWith(
          rowMock,
          editServiceInstance.editingForm,
          '1',
        );
      });
    });
  });
  describe('.cancelAction', () => {
    beforeEach(() => {
      actionsManager.setCurrentAction(actions[0]);
      actionsManager.setEditingRowId('rowid');
    });
    it('clears current Action', async () => {
      actionsManager.cancelAction();
      expect(actionsManager.currentAction).toEqual(undefined);
    });
    it('clears editingRowId Action', async () => {
      actionsManager.cancelAction();
      expect(actionsManager.editingRowId).toEqual(undefined);
    });
    it('calls clearEditingForm from editing service', async () => {
      actionsManager.cancelAction();
      expect(editServiceInstance.clearEditingForm).toBeCalledTimes(1);
    });
  });
});
