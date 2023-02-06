import { getConnector } from './shared';

describe('backend-connector', () => {
  test('should set handlers `validateResult` and `refreshResult` when setting bridges', () => {
    const { checkboxBridge, advancedBridge } = getConnector();

    for (let mockBridge of [checkboxBridge, advancedBridge]) {
      expect(mockBridge.agent).toHaveProperty('validateResult');
      expect(mockBridge.agent).toHaveProperty('refreshResult');
    }
  });

  test('should remove handlers from rpc bridge on clean', () => {
    const { connector, checkboxBridge, advancedBridge } = getConnector();

    connector.clean();

    for (let bridge of [checkboxBridge, advancedBridge]) {
      expect(bridge).not.toHaveProperty('validateResult');
      expect(bridge).not.toHaveProperty('refreshResult');
    }
  });

  test('should call methods on checkboxBridge when type `visible`', () => {
    const { connector, checkboxBridge, advancedBridge } = getConnector();

    checkboxBridge._setMockHandlers();
    advancedBridge._setMockHandlers();

    connector.validate();

    expect(checkboxBridge.agent.validateResult).toHaveBeenCalledTimes(1);
    expect(advancedBridge.agent.validateResult).not.toHaveBeenCalled();

    connector.refresh();

    expect(checkboxBridge.agent.refreshResult).toHaveBeenCalled();
    expect(advancedBridge.agent.refreshResult).not.toHaveBeenCalled();
  });

  test('should call validate on checkboxBridge when type `visible` and bridges are not ready', () => {
    const { connector, checkboxBridge, advancedBridge } = getConnector('visible');

    checkboxBridge._setMockHandlers();
    advancedBridge._setMockHandlers();

    connector._readyBridges.checkboxBridge = false;
    connector._readyBridges.advancedBridge = false;

    connector.validate();

    expect(checkboxBridge.agent.validateResult).not.toHaveBeenCalled();
    expect(advancedBridge.agent.validateResult).not.toHaveBeenCalled();

    connector.setReady('checkboxBridge');

    expect(checkboxBridge.agent.validateResult).toHaveBeenCalledTimes(1);
    expect(advancedBridge.agent.validateResult).not.toHaveBeenCalled();
  });

  test('should call refresh on checkboxBridge when type `visible` and bridges are not ready', () => {
    const { connector, checkboxBridge, advancedBridge } = getConnector('visible');

    checkboxBridge._setMockHandlers();
    advancedBridge._setMockHandlers();

    connector._readyBridges.checkboxBridge = false;
    connector._readyBridges.advancedBridge = false;

    connector.refresh();

    expect(checkboxBridge.agent.refreshResult).not.toHaveBeenCalled();
    expect(advancedBridge.agent.refreshResult).not.toHaveBeenCalled();

    connector.setReady('checkboxBridge');

    expect(checkboxBridge.agent.refreshResult).toHaveBeenCalledTimes(1);
    expect(advancedBridge.agent.refreshResult).not.toHaveBeenCalled();
  });

  test('should call validate on advancedBridge when type `invisible` and bridges are not ready', () => {
    const { connector, checkboxBridge, advancedBridge } = getConnector('invisible');

    checkboxBridge._setMockHandlers();
    advancedBridge._setMockHandlers();

    connector._readyBridges.checkboxBridge = false;
    connector._readyBridges.advancedBridge = false;

    connector.validate();

    expect(checkboxBridge.agent.validateResult).not.toHaveBeenCalled();
    expect(advancedBridge.agent.validateResult).not.toHaveBeenCalled();

    connector.setReady('advancedBridge');

    expect(checkboxBridge.agent.validateResult).not.toHaveBeenCalled();
    expect(advancedBridge.agent.validateResult).toHaveBeenCalledTimes(1);
  });

  test('should call refresh on advancedBridge when type `invisible` and bridges are not ready', () => {
    const { connector, checkboxBridge, advancedBridge } = getConnector('invisible');

    checkboxBridge._setMockHandlers();
    advancedBridge._setMockHandlers();

    connector._readyBridges.checkboxBridge = false;
    connector._readyBridges.advancedBridge = false;

    connector.refresh();

    expect(checkboxBridge.agent.refreshResult).not.toHaveBeenCalled();
    expect(advancedBridge.agent.refreshResult).not.toHaveBeenCalled();

    connector.setReady('advancedBridge');

    expect(checkboxBridge.agent.refreshResult).not.toHaveBeenCalled();
    expect(advancedBridge.agent.refreshResult).toHaveBeenCalledTimes(1);
  });

  test('should call methods on advancedBridge when type `invisible`', () => {
    const { connector, checkboxBridge, advancedBridge } = getConnector('invisible');

    checkboxBridge._setMockHandlers();
    advancedBridge._setMockHandlers();

    connector.validate();

    expect(checkboxBridge.agent.validateResult).not.toHaveBeenCalled();
    expect(advancedBridge.agent.validateResult).toHaveBeenCalled();

    connector.refresh();

    expect(checkboxBridge.agent.refreshResult).not.toHaveBeenCalled();
    expect(advancedBridge.agent.refreshResult).toHaveBeenCalled();
  });

  test('should call callback on validate', () => {
    const { connector } = getConnector();

    const mockFn = jest.fn();

    connector.on({ event: 'validate', callback: mockFn });

    connector.validate();

    expect(mockFn).toHaveBeenCalled();
  });

  test('should call multiple callbacks on validate', () => {
    const { connector } = getConnector();

    const mockFns = [jest.fn(), jest.fn(), jest.fn()];

    for (let mockFn of mockFns) {
      connector.on({ event: 'validate', callback: mockFn });
    }

    connector.validate();

    for (let mockFn of mockFns) {
      expect(mockFn).toHaveBeenCalled();
    }
  });

  test('should call callback on refresh', () => {
    const { connector } = getConnector();

    const mockFn = jest.fn();

    connector.on({ event: 'refresh', callback: mockFn });

    connector.refresh();

    expect(mockFn).toHaveBeenCalled();
  });

  test('should call multiple callbacks on refresh', () => {
    const { connector } = getConnector();

    const mockFns = [jest.fn(), jest.fn(), jest.fn()];

    for (let mockFn of mockFns) {
      connector.on({ event: 'refresh', callback: mockFn });
    }

    connector.refresh();

    for (let mockFn of mockFns) {
      expect(mockFn).toHaveBeenCalled();
    }
  });

  test('should call reset method via checkboxBridge when type is `visible`', () => {
    const { connector, checkboxBridge, advancedBridge } = getConnector();

    checkboxBridge._setMockHandlers();
    advancedBridge._setMockHandlers();

    connector.reset();

    expect(checkboxBridge.agent.resetBackend).toHaveBeenCalled();
    expect(advancedBridge.agent.resetBackend).not.toHaveBeenCalled();
  });
  test('should call reset method via advancedBridge when type is `invisible`', () => {
    const { connector, checkboxBridge, advancedBridge } = getConnector('invisible');

    checkboxBridge._setMockHandlers();
    advancedBridge._setMockHandlers();

    connector.reset();

    expect(checkboxBridge.agent.resetBackend).not.toHaveBeenCalled();
    expect(advancedBridge.agent.resetBackend).toHaveBeenCalled();
  });
});
