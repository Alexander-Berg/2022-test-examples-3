// tslint:disable:no-empty
import {IEnv} from 'core-legacy/types'
import {ISnackbarNotifier, Snackbar, SnackbarConfig} from 'core-legacy/lib/snackbar-notifier'
import noop from 'lodash-es/noop'
import {ILocalStorage} from 'core-legacy/lib/local-storage'
import {VendorApi} from 'core-legacy/types/restyped'
import ApiScenario from 'core-legacy/api/mock/ApiScenario'
import ApiMock from 'core-legacy/api/mock/ApiMock'
import {Notifier} from 'main-notifications'
import {LoggerStub, MetrikaStub, SocketEventsMock, PlatformMock} from 'shared/test-utils'
import {DialogModel} from 'shared-modals'

/* eslint-disable @typescript-eslint/no-unused-vars */
export class SnackbarNotifierStub implements ISnackbarNotifier {
  get current() {
    return undefined
  }
  enqueue(config: SnackbarConfig) {}
  remove(id: string) {}
  onChangeCurrent(cb: (currentSnackbar?: Snackbar) => void) {
    return noop
  }
}

export class LocalStorageStub implements ILocalStorage {
  set(key: string, value: any) {}
  remove(key: string) {}
  get(key: string) {}
  clear() {}
}
/* eslint-enable @typescript-eslint/no-unused-vars */

export interface TestingEnv extends IEnv {
  api: ApiMock<VendorApi> | ApiScenario<VendorApi>
}

const createEnv = ({api}: Partial<Pick<TestingEnv, 'api'>> = {}): TestingEnv => {
  api = api ?? new ApiScenario()
  const logger = new LoggerStub()
  const metrika = new MetrikaStub()

  return {
    api,
    logger,
    metrika,
    platform: new PlatformMock(),
    localStorage: new LocalStorageStub(),
    snackbarNotifier: new SnackbarNotifierStub(),
    // TODO: Notifier mock?
    notifier: new Notifier(
      {
        api,
        logger,
        metrika
      },
      {
        isLoggedIn: () => false
      }
    ),
    socketEvent$: new SocketEventsMock(),
    dialogs: new DialogModel()
  }
}

export default createEnv
