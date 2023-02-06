/* eslint-env mocha */

const assert = require('assert');

const convertLingui = require('../lib/convert').fromLinguiToTanker;
const convertTanker = require('../lib/convert').fromTankerToLingui;

describe('LinguiTanker.Lib.Convert', () => {
    const config = {
        dir: '/locale',
        project: 'project_name',
        keysets: 'keyset-1',
        keyset: 'keyset-1',
        locale: 'ru',
        branch: 'branch',
        token: 'access-token',
    };

    const tankerData = status => {
        /* eslint-disable */
        return {
            export_info: {
                request: {
                    'project-id': 'project_name',
                    'keyset-id': 'keyset-1',
                    'branch-id': 'branch'
                },
                name: 'project_name',
                branch: 'branch'
            },
            keysets: {
                'keyset-1': {
                    meta: {
                        languages: ['ru', 'en']
                    },
                    keys: {
                        'key-1': {
                            info: {
                                context: 'key-context-1',
                                is_plural: false
                            },
                            translations: {
                                ru: {
                                    status: 'approved',
                                    form: 'key-1-translate-ru'
                                },
                                en: {
                                    status: 'approved',
                                    form: 'key-1-translate-en'
                                }
                            }
                        },
                        'key-2': {
                            info: {
                                context: 'key-context-2',
                                is_plural: false
                            },
                            translations: {
                                ru: {
                                    status: status,
                                    form: 'key-2-translate-ru'
                                },
                                en: {
                                    status: status,
                                    form: 'key-2-translate-en'
                                }
                            }
                        }
                    }
                }
            }
        };
        /* eslint-enable */
    };

    const linguiData = {
        ru: {
            'key-1': {
                translation: 'key-1-translate-ru',
                description: 'key-context-1',
            },
            'key-2': {
                translation: 'key-2-translate-ru',
                description: 'key-context-2',
            },
        },
        en: {
            'key-1': {
                translation: 'key-1-translate-en',
                description: 'key-context-1',
            },
            'key-2': {
                translation: 'key-2-translate-en',
                description: 'key-context-2',
            },
        },
    };

    it('should convert tjson to ljson', () => {
        let res;

        res = convertTanker(config, tankerData('review'));

        assert.deepStrictEqual(res, {
            ru: {
                'key-1': {
                    translation: linguiData.ru['key-1'].translation,
                },
            },
            en: {
                'key-1': {
                    translation: linguiData.en['key-1'].translation,
                },
            },
        });

        res = convertTanker(config, tankerData('approved'));

        assert.deepStrictEqual(res, {
            ru: {
                'key-1': {
                    translation: linguiData.ru['key-1'].translation,
                },
                'key-2': {
                    translation: linguiData.ru['key-2'].translation,
                },
            },
            en: {
                'key-1': {
                    translation: linguiData.en['key-1'].translation,
                },
                'key-2': {
                    translation: linguiData.en['key-2'].translation,
                },
            },
        });
    });

    it('should convert ljson to tjson', () => {
        const res = convertLingui(config, linguiData);
        const tankerRes = tankerData('approved');

        assert.deepStrictEqual(res, {
            ...tankerRes,
            // eslint-disable-next-line camelcase
            export_info: {
                branch: config.branch,
                request: {
                    'project-id': config.project,
                    'keyset-id': config.keyset,
                },
            },
            keysets: {
                [config.keyset]: {
                    keys: tankerRes.keysets[config.keyset].keys,
                },
            },
        });
    });
});
