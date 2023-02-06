import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import Api from 'src/Api';
import { testUser } from 'test/data/user';
import { testFrontendConfig } from 'test/data/frontendConfig';

type AppApiMock = MockedApiObject<Api>;

/**
 * Requests present on all pages.
 */
export function resolveCommonRequests(api: AppApiMock) {
  resolveCurrentUser(api);
  resolveConfig(api);
}

export function resolveCurrentUser(api: AppApiMock) {
  api.configController.currentUser.next().resolve(testUser);
}

export function resolveConfig(api: AppApiMock) {
  api.configController.config.next().resolve(testFrontendConfig());
}
