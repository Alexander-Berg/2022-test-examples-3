export default {
  environment: {
    OSFamily: '',
    BrowserName: '',
    isTouch: false,
    isMobileApp: false,
    appType: ''
  },
  session: {
    version: 'test',
    login: 'rideorgtfo',
    locale: 'ru'
  },
  config: {
    user: {
      type: 'common',
      emails: []
    },
    urls: {
      calendar: '//calendar',
      conferenceRoomMap: '//conferenceRoomMap',
      avatars: '//avatars',
      corpAvatars: '//corpAvatars',
      xiva: 'wss://xiva',
      maps: '//yandex.ru/maps'
    },
    errors: {
      AUTH_NO_AUTH: 'AUTH_NO_AUTH'
    },
    date: {
      serverTime: Date.now(),
      weekStartDay: 1,
      timezoneId: 'Europe/Moscow',
      timezoneOffset: -180,
      geoTimezoneId: 'Europe/Moscow',
      geoTimezoneOffset: -180
    },
    experiments: {
      exp: '',
      eexp: ''
    },
    domainConfig: {
      enableTelemost: false
    }
  },
  preloadedState: {
    'get-user-layers': [],
    'get-user-settings': {
      defaultView: 'week'
    }
  },
  splashScreen: {
    remove() {},
    error() {},
    isExist() {}
  }
};
