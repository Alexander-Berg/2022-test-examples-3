import {ILogger} from 'core-legacy'

export default class LoggerStub implements ILogger {
  trace() {}
  debug() {}
  info() {}
  warning() {}
  error() {}
  critical() {}
  promise() {}
  sendLog() {}
  watchQueue() {}
  setAppRoute() {}
  updateRequest() {}
  updateEnv(): void {}
  setMaxRetryCount(): void {}
  setPollInterval(): void {}
  startLogPolling(): void {}
  stopLogPolling(): void {}
  setPartnerId() {}
  setCommonData() {}
}
