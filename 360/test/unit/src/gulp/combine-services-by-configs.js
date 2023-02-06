'use strict';

import { describe } from 'ava-spec';
import combineServicesByConfigs from '../../../../src/gulp/helpers/combine-services-configs.js';

describe('#combineServicesByConfigs', (it) => {
    it('Должен правильно объединить в один конфиг', (t) => {
        const settingsConfig = { domains: ['ru', 'ua', 'kz'], optionalDomains: [] };
        const localConfig = {
            mail: {
                domains: {
                    ru: {
                        url: 'mailru'
                    },

                    kz: {
                        url: 'mailkz'
                    },

                    ua: {
                        url: 'mailua'
                    }
                }
            },

            disk: {
                domains: {
                    ru: {
                        url: 'diskru'
                    },

                    kz: {
                        url: 'diskkz'
                    },

                    ua: {
                        url: 'diskua'
                    }
                }
            }
        };

        t.deepEqual(combineServicesByConfigs(settingsConfig, localConfig), {
            mail: localConfig.mail.domains, disk: localConfig.disk.domains
        });
    });

    it('Должен правильно подставить дефолтную информацию для не указанного домена', (t) => {
        const settingsConfig = { domains: ['ru', 'ua', 'kz', 'test'], optionalDomains: [] };
        const localConfig = {
            mail: {
                domains: {
                    ru: {
                        url: 'mailru'
                    },

                    kz: {
                        url: 'mailkz'
                    },

                    ua: {
                        url: 'mailua'
                    },

                    test: {
                        url: 'mailtest'
                    }
                }
            },

            disk: {
                defaultDomain: 'ru',

                domains: {
                    ru: {
                        url: 'diskru'
                    },

                    kz: {
                        url: 'diskkz'
                    },

                    ua: {
                        url: 'diskua'
                    }
                }
            }
        };

        const disk = localConfig.disk.domains;

        t.deepEqual(combineServicesByConfigs(settingsConfig, localConfig), {
            mail: localConfig.mail.domains,
            disk: { ru: disk.ru, kz: disk.kz, ua: disk.ua, test: disk.ru }
        });
    });

    it('Должен не подставлять дефолтные значения в не обязательные домены', (t) => {
        const settingsConfig = { domains: ['ru', 'ua', 'kz', 'test'], optionalDomains: ['yandex-team'] };

        const localConfig = {
            mail: {
                domains: {
                    ru: {
                        url: 'mailru'
                    },

                    kz: {
                        url: 'mailkz'
                    },

                    ua: {
                        url: 'mailua'
                    },

                    test: {
                        url: 'mailtest'
                    },

                    'yandex-team': {
                        url: "yanndexteamtest"
                    }
                }
            },

            disk: {
                defaultDomain: 'ru',

                domains: {
                    ru: {
                        url: 'diskru'
                    },

                    kz: {
                        url: 'diskkz'
                    },

                    ua: {
                        url: 'diskua'
                    }
                }
            }
        };

        const disk = localConfig.disk.domains;

        t.deepEqual(combineServicesByConfigs(settingsConfig, localConfig), {
            mail: localConfig.mail.domains,
            disk: { ru: disk.ru, kz: disk.kz, ua: disk.ua, test: disk.ru }
        });
    });

    it('Должен не подставлять информацию дефолтного домена в другие домены, если стоит флаг yandexTeamOnly', (t) => {
        const settingsConfig = { domains: ['ru', 'ua', 'kz', 'test'], optionalDomains: ['yandex-team'] };

        const localConfig = {
            mail: {
                defaultDomain: 'ru',
                yandexTeamOnly: true,

                domains: {
                    ru: {
                        url: 'mailru'
                    },

                    kz: {
                        url: 'mailkz'
                    },

                    'yandex-team': {
                        url: "yandexteamtest"
                    }
                }
            },

            disk: {
                defaultDomain: 'ru',

                domains: {
                    ru: {
                        url: 'diskru'
                    },

                    kz: {
                        url: 'diskkz'
                    },

                    ua: {
                        url: 'diskua'
                    }
                }
            }
        };

        const disk = localConfig.disk.domains;

        t.deepEqual(combineServicesByConfigs(settingsConfig, localConfig), {
            mail: { 'yandex-team': localConfig.mail.domains['yandex-team'] },
            disk: { ru: disk.ru, kz: disk.kz, ua: disk.ua, test: disk.ru }
        });
    });
});

