import { configure } from 'mobx';
import { StateManager } from './StateManager';
import { ActionsManager } from '../ActionsManager';
import { tableData, rowForAdd } from './stub/table';
import { WithEditService } from '../../withEdit.service';
jest.mock('../../withEdit.service');
configure({ safeDescriptors: false });

const editServiceInstance = new WithEditService();
editServiceInstance.actionsManager = ({
  setActionCollection: jest.fn(),
} as unknown) as ActionsManager;

describe('StateManager', () => {
  let stateManager: StateManager;

  beforeEach(() => {
    stateManager = new StateManager(editServiceInstance);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('.setTable', () => {
    it('sets table for state manager', () => {
      stateManager.setTable(tableData);
      expect(stateManager.getTable()).toEqual(tableData);
    });
  });
  describe('.addRowLocally', () => {
    it('adds row to the table', () => {
      stateManager.setTable(tableData);
      stateManager.addRowLocally(rowForAdd);
      const data = stateManager.getTableData();
      expect(data?.[1]).toEqual(rowForAdd);
    });
  });
  describe('.updateRowLocally', () => {
    it('updates row in the table', () => {
      stateManager.setTable(tableData);
      stateManager.updateRowLocally({ ...rowForAdd, id: 'id1' });
      const data = stateManager.getTableData();
      expect(data?.[0]?.fields).toEqual(rowForAdd.fields);
    });
  });
  describe('.updateRowsLocally', () => {
    it('updates rows in the table', () => {
      stateManager.setTable(tableData);
      stateManager.updateRowsLocally([{ ...rowForAdd, id: 'id1' }]);
      const data = stateManager.getTableData();
      expect(data?.[0]?.fields).toEqual(rowForAdd.fields);
    });
  });
  describe('.removeRowLocally', () => {
    it('removes row from the table', () => {
      stateManager.setTable(tableData);
      stateManager.removeRowLocally('id1');
      const data = stateManager.getTableData();
      expect(data).toEqual([]);
    });
  });
});
