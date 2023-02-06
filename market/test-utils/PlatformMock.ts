import {IPlatform} from '../../core-platform'
import {XivaClientName} from '../../core-notifications'
import {NotifyDeviceType} from '../../core-platform/types'

interface Params {
  xivaClientName: XivaClientName
  notifyDeviceType: NotifyDeviceType
}

export default class PlatformMock implements IPlatform {
  public readonly xivaClientName: XivaClientName
  public readonly notifyDeviceType: NotifyDeviceType

  constructor({xivaClientName = 'restapp_mobile', notifyDeviceType = 'android'}: Partial<Params> = {}) {
    this.xivaClientName = xivaClientName
    this.notifyDeviceType = notifyDeviceType
  }

  addNotification(): void {}

  initializeNotifications(): void {}
}
