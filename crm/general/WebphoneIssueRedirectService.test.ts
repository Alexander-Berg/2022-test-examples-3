import { createMemoryHistory } from 'history';
import { IssueType } from 'modules/issues/types';
import { WebphoneIssueRedirectService } from './WebphoneIssueRedirectService';

const history = createMemoryHistory();
const historyPushSpy = jest.spyOn(history, 'push');

let mockCreateLink;
jest.mock('modules/issues/utils/createLink', () => {
  mockCreateLink = jest.fn(({ id, typeId }) => `${id} ${typeId}`);
  return mockCreateLink;
});

jest.mock('./WebphoneIncomingCallService', () => ({
  webphoneIncomingCallService: {
    onCallAccept: (callback) => {
      callback({
        issueId: 123,
        issueTypeId: 3, // IssueType.Ticket
      });
    },
  },
}));

describe('WebphoneIssueRedirectService', () => {
  it('redirects to specified issue', () => {
    const _webphoneIssueRedirectService = new WebphoneIssueRedirectService(history);
    expect(mockCreateLink).toBeCalledWith(
      expect.objectContaining({ id: 123, typeId: IssueType.Ticket }),
    );
    expect(historyPushSpy).toBeCalledWith('123 3');
  });
});
