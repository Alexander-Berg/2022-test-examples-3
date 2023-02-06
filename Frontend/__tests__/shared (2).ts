import { RPC } from '../../typings/RPC';
import { BackendConnector } from '../backend-connector';

type Utilities = {
  _setMockHandlers: () => void,
}

export function getMockBridge(): RPC & Utilities {
  return {
    agent: [] as Function[],
    invokeMethod(type: string) {
      if (type === 'validate' || type === 'refresh') {
        this.agent[`${type}Result`]();
      } else {
        this.agent[type]();
      }
    },
    _setMockHandlers() {
      this.agent.validateResult = jest.fn();
      this.agent.refreshResult = jest.fn();
      this.agent.resetBackend = jest.fn();
    },
  } as unknown as RPC & Utilities;
}

export function getConnector(type: 'visible' | 'invisible' = 'visible') {
  const connector = new BackendConnector(type);
  const checkboxBridge = getMockBridge();
  const advancedBridge = getMockBridge();

  connector.setBridges(checkboxBridge, advancedBridge);
  connector.setReady('checkboxBridge');
  connector.setReady('advancedBridge');

  return {
    connector,
    checkboxBridge,
    advancedBridge,
  };
}
