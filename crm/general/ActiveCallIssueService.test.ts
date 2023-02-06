import { runInAction } from 'mobx';
import { webphoneService } from 'services/WebphoneService';
import { ActiveCallIssueService } from './ActiveCallIssueService';

const issueObj = { id: 1, typeId: 1 };

describe('ActiveCallIssueService', () => {
  describe('when has active call', function() {
    it('sets issue', () => {
      const activeCallIssueService = new ActiveCallIssueService();

      runInAction(() => {
        webphoneService.hasActiveCall = true;
      });

      activeCallIssueService.issue = issueObj;
      activeCallIssueService.destroy();

      expect(activeCallIssueService.issue).toStrictEqual(issueObj);
    });
  });

  describe('when has no active call', function() {
    it('does not set issue', () => {
      const activeCallIssueService = new ActiveCallIssueService();

      runInAction(() => {
        webphoneService.hasActiveCall = false;
      });

      activeCallIssueService.issue = issueObj;
      activeCallIssueService.destroy();

      expect(activeCallIssueService.issue).toBe(undefined);
    });
  });

  describe('.clear', () => {
    it('cleans issue', () => {
      const activeCallIssueService = new ActiveCallIssueService();

      runInAction(() => {
        webphoneService.hasActiveCall = true;
      });

      activeCallIssueService.issue = issueObj;
      activeCallIssueService.clear();
      activeCallIssueService.destroy();

      expect(activeCallIssueService.issue).toBe(undefined);
    });
  });

  describe('when call end', () => {
    it('cleans issue data', () => {
      runInAction(() => {
        webphoneService.hasActiveCall = true;
      });

      const activeCallIssueService = new ActiveCallIssueService();

      activeCallIssueService.issue = issueObj;

      runInAction(() => {
        webphoneService.hasActiveCall = false;
      });

      activeCallIssueService.destroy();

      expect(activeCallIssueService.issue).toBe(undefined);
    });
  });
});
