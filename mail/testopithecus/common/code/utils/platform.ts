export const enum PlatformType {
  android = 'android',
  electron = 'electron',
  ios = 'ios',
  touch = 'touch',
}
export function platformToClient(platform: Platform): string {
  switch (platform.type) {
    case PlatformType.android: return platform.isTablet ? 'apad' : 'aphone';
    case PlatformType.ios: return platform.isTablet ? 'ipad' : 'iphone';
    case PlatformType.electron: return 'unknown';
    case PlatformType.touch: return 'unknown';
  }
}

export interface Platform {
  readonly type: PlatformType
  readonly isTablet: boolean
}

export class MockPlatform implements Platform {

  constructor(
    public readonly type: PlatformType,
    public readonly isTablet: boolean,
  ) { }

  public static androidDefault: Platform = new MockPlatform(PlatformType.android, false)

}
