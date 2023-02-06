import rumProvider from '../RumProvider';
import { UXExperiment } from './UXExperiment';
jest.mock('../RumProvider');

let experiment;

const UXExperimentConfig = {
  expId: 'testExpId',
  componentName: 'testSubpageName',
  startFlow: ['start'],
  endFlow: ['finish', 'cancel'],
  additional: {
    test: 'testValue',
  },
};

const paramsObject = { param: 'value' };

describe('Rum-Service: UXExperiment ', () => {
  beforeEach(() => {
    experiment = new UXExperiment(UXExperimentConfig);
  });
  describe('.constructor', () => {
    it('sets config', () => {
      expect(experiment.config).toEqual(UXExperimentConfig);
    });
    it('sets expId', () => {
      expect(experiment.expId).toEqual(UXExperimentConfig.expId);
    });
    it('sets additional', () => {
      expect(experiment.additional).toEqual(UXExperimentConfig.additional);
    });
  });
  describe('.checkPoint', () => {
    it('call sendTime via RumProvider', () => {
      experiment.checkPoint('start', Date.now(), {});
      expect(rumProvider.sendTimeMark).toBeCalled();
    });
  });
  describe('.startFlow', () => {
    it('gets FlowId', () => {
      experiment.checkPoint('start', Date.now(), {});
      expect(experiment.userFlowId).toEqual(`${experiment.expId}_${experiment.startCount}`);
    });
    it('sets "isStarted" flag to true', () => {
      experiment.checkPoint('start', Date.now(), {});
      expect(experiment.isStarted).toEqual(true);
    });
    it('adds "userFlow" to additional', () => {
      const experiment = new UXExperiment(UXExperimentConfig);
      experiment.checkPoint('start', Date.now(), {});
      expect(experiment.userFlowId).toEqual(experiment.additional.userFlowId);
    });
  });

  describe('.endFlow', () => {
    it('cleans FlowId', () => {
      experiment.checkPoint('start', Date.now(), {});
      experiment.checkPoint('finish', Date.now(), {});
      expect(experiment.userFlowId).toEqual(``);
    });
    it('sets "isStarted" to false', () => {
      experiment.checkPoint('start', Date.now(), {});
      experiment.checkPoint('finish', Date.now(), {});
      expect(experiment.isStarted).toEqual(false);
    });
    it('cleans "userFlow" in additional', () => {
      const experiment = new UXExperiment(UXExperimentConfig);
      experiment.checkPoint('start', Date.now(), {});
      experiment.checkPoint('finish', Date.now(), {});
      expect(experiment.additional.userFlowId).toEqual('');
    });
  });

  describe('.mergeAdditional', () => {
    it('gets merged additional', () => {
      const mergedAdditional = experiment.mergeAdditional(paramsObject);
      expect(mergedAdditional).toEqual({ experiment: experiment.additional, event: paramsObject });
    });
  });

  describe('.setAdditional', () => {
    it('sets experiment additional', () => {
      experiment.setAdditional(paramsObject);
      expect(experiment.additional).toEqual({ ...UXExperimentConfig.additional, ...paramsObject });
    });
  });
});
