import { ConnectionChecker } from './ConnectionChecker.service';

jest.mock('services/Rum/RumProvider');
jest.mock('services/Logger');

const mockStart = jest.fn();
const mockAddEventListener = jest.fn();
const mockPostMessage = jest.fn();

jest.mock('utils/createSharedWorker', () => ({
  createSharedWorker: () => ({
    port: {
      start: mockStart,
      addEventListener: mockAddEventListener,
      postMessage: mockPostMessage,
    },
  }),
}));

global.reduxStore = { dispatch: jest.fn() };

describe('ConnectionChecker', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('is Singleton', () => {
    const connectionChecker1 = new ConnectionChecker();
    const connectionChecker2 = new ConnectionChecker();
    expect(connectionChecker1 === connectionChecker2).toBeTruthy;
  });

  it('.run', () => {
    const connectionChecker = new ConnectionChecker();
    connectionChecker.run();
    expect(mockStart).toBeCalled();
    expect(mockPostMessage).toBeCalledWith(['connect', 'http://localhost/ready']);
  });

  it('changes status', () => {
    const connectionChecker = new ConnectionChecker();
    connectionChecker.messageHandler({ data: { status: 'offline' } });
    expect(connectionChecker.isOnline()).toBeFalsy();
    connectionChecker.messageHandler({ data: { status: 'online' } });
    expect(connectionChecker.isOnline()).toBeTruthy();
    expect(global.reduxStore.dispatch).toBeCalledTimes(3);
  });
});
