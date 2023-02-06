const de = require('@ps-int/ufo-descript');
const no = require('nommon');
const path = require('path');

/**
 * @typedef {Object} ClustersParams
 * @property {string} filter
 */

jest.mock('helpers/path', () => ({
    normalize: (path) => path
}), { virtual: true });

const blockPath = 'components/models/get-albums-slices/get-albums-slices.jsx';
const blockAbsolutePath = path.resolve(blockPath);

describe('get-albums-slices.jsx', () => {
    let deLogInfo;
    beforeEach(() => {
        process.argv = [];

        de.script.init();
        deLogInfo = de.log.info;
        de.log.info = jest.fn();
    });

    afterEach(() => {
        de.file.unwatch();
        de.events.trigger('loaded-file-changed', blockAbsolutePath);
        de.log.info = deLogInfo;
    });

    it('должен вернуть ошибку, если в модуле `model` произошла ошибка в момент инициализации снепшота', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule([]),
            model: mockDescriptModuleFunctions({
                initSnapshot: {
                    error: {
                        id: 'Module error'
                    }
                }
            })
        };

        getAlbumsSlices().then((result) => {
            expect(result.object()).toEqual({ error: { id: 'Module error' } });
            expect(de._modules.cloudAPI).not.toHaveBeenCalled();
            expect(getModuleFunctionCalls(de._modules.model, 'initSnapshot')).toHaveLength(1);
            expect(getModuleFunctionCalls(de._modules.model, 'getResources')).toHaveLength(0);

            done();
        });
    });

    it('должен вернуть ошибку, если запрос инициализации снепшота вернул ошибку', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule([]),
            model: mockDescriptModuleFunctions({
                initSnapshot: {
                    data: {
                        error: {
                            id: 'Module error'
                        }
                    }
                }
            })
        };

        getAlbumsSlices().then((result) => {
            expect(result.object()).toEqual({ error: { id: 'Module error' } });
            expect(de._modules.cloudAPI).not.toHaveBeenCalled();
            expect(getModuleFunctionCalls(de._modules.model, 'initSnapshot')).toHaveLength(1);
            expect(getModuleFunctionCalls(de._modules.model, 'getResources')).toHaveLength(0);

            done();
        });
    });

    it('должен передать правильные параметры от `initSnapshot` до `getAlbums`', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule([]),
            model: mockDescriptModuleFunctions({
                initSnapshot: {
                    data: {
                        photoslice_id: '1'
                    }
                }
            })
        };

        getAlbumsSlices({ user: { uid: '1' } }).then((result) => {
            expect(result.object()).toEqual({ albums: {} });
            expect(de._modules.cloudAPI).toHaveBeenCalled();
            expect(de._modules.cloudAPI.mock.calls[0][0]).toEqual({ idSlice: '1', user: { uid: '1' }, resourcesIds: [] });

            expect(getModuleFunctionCalls(de._modules.model, 'initSnapshot')).toHaveLength(1);
            expect(getModuleFunctionCalls(de._modules.model, 'getResources')).toHaveLength(0);

            done();
        });
    });

    it('должен вернуть пустой массив, если альбомов нет', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule([]),
            model: mockDescriptModuleFunctions({
                initSnapshot: {
                    data: {
                        photoslice_id: '1'
                    }
                }
            })
        };

        getAlbumsSlices().then((result) => {
            expect(result.object()).toEqual({ albums: {} });
            expect(de._modules.cloudAPI).toHaveBeenCalled();
            expect(getModuleFunctionCalls(de._modules.model, 'initSnapshot')).toHaveLength(1);
            expect(getModuleFunctionCalls(de._modules.model, 'getResources')).toHaveLength(0);

            done();
        });
    });

    it('должен вернуть ошибку, если запрос за альбомами вернул ошибку', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                error: {
                    id: 'Module error'
                }
            }),
            model: mockDescriptModuleFunctions({
                initSnapshot: {
                    data: {
                        photoslice_id: '1'
                    }
                }
            })
        };

        getAlbumsSlices().then((result) => {
            expect(result.object()).toEqual({ error: { id: 'Module error' } });
            expect(de._modules.cloudAPI).toHaveBeenCalled();
            expect(getModuleFunctionCalls(de._modules.model, 'initSnapshot')).toHaveLength(1);
            expect(getModuleFunctionCalls(de._modules.model, 'getResources')).toHaveLength(0);

            done();
        });
    });

    it('не должен упасть, если в модуле `model` произойдет ошибка в момент получения ресурсов', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                albums: {
                    items: [{
                        album: 'camera',
                        count: 2,
                        previews: ['/disk/camera1.jpg', '/disk/camera2.jpg']
                    }, {
                        album: 'screenshots',
                        count: 3,
                        previews: ['/disk/screenshots1.jpg', '/disk/screenshots2.jpg']
                    }]
                }
            }),
            model: mockDescriptModuleFunctions({
                initSnapshot: {
                    data: {
                        photoslice_id: '1'
                    }
                },
                getResources: {
                    error: {
                        id: 'Module error'
                    }
                }
            })
        };

        getAlbumsSlices().then((result) => {
            expect(result.object()).toEqual({
                albums: {
                    camera: {
                        id: 'camera',
                        count: 2
                    },
                    screenshots: {
                        id: 'screenshots',
                        count: 3
                    }
                }
            });
            expect(de._modules.cloudAPI).toHaveBeenCalled();
            expect(getModuleFunctionCalls(de._modules.model, 'initSnapshot')).toHaveLength(1);
            expect(getModuleFunctionCalls(de._modules.model, 'getResources')).toHaveLength(1);

            done();
        });
    });

    it('не должен упасть, если запрос за ресурсами вернет ошибку', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                albums: {
                    items: [{
                        album: 'camera',
                        count: 2,
                        previews: ['/disk/camera1.jpg', '/disk/camera2.jpg']
                    }, {
                        album: 'screenshots',
                        count: 3,
                        previews: ['/disk/screenshots1.jpg', '/disk/screenshots2.jpg']
                    }]
                }
            }),
            model: mockDescriptModuleFunctions({
                initSnapshot: {
                    data: {
                        photoslice_id: '1'
                    }
                },
                getResources: {
                    data: {
                        error: {
                            id: 'Module error'
                        }
                    }
                }
            })
        };

        getAlbumsSlices().then((result) => {
            expect(result.object()).toEqual({
                albums: {
                    camera: {
                        id: 'camera',
                        count: 2
                    },
                    screenshots: {
                        id: 'screenshots',
                        count: 3
                    }
                }
            });
            expect(de._modules.cloudAPI).toHaveBeenCalled();
            expect(getModuleFunctionCalls(de._modules.model, 'initSnapshot')).toHaveLength(1);
            expect(getModuleFunctionCalls(de._modules.model, 'getResources')).toHaveLength(1);

            done();
        });
    });

    it('должен вернуть альбомы без обложек, если ни одного ресурса не было найдено', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                albums: {
                    items: [{
                        album: 'camera',
                        count: 2,
                        previews: ['/disk/camera1.jpg', '/disk/camera2.jpg']
                    }, {
                        album: 'screenshots',
                        count: 3,
                        previews: ['/disk/screenshots1.jpg', '/disk/screenshots2.jpg']
                    }]
                }
            }),
            model: mockDescriptModuleFunctions({
                initSnapshot: {
                    data: {
                        photoslice_id: '1'
                    }
                },
                getResources: {
                    data: []
                }
            })
        };

        getAlbumsSlices().then((result) => {
            expect(result.object()).toEqual({
                albums: {
                    camera: {
                        id: 'camera',
                        count: 2
                    },
                    screenshots: {
                        id: 'screenshots',
                        count: 3
                    }
                }
            });
            expect(de._modules.cloudAPI).toHaveBeenCalled();
            expect(getModuleFunctionCalls(de._modules.model, 'initSnapshot')).toHaveLength(1);
            expect(getModuleFunctionCalls(de._modules.model, 'getResources')).toHaveLength(1);

            done();
        });
    });

    it('должен правильно отработать, если нет некоторых ресурсов', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                albums: {
                    items: [{
                        album: 'camera',
                        count: 2,
                        previews: ['/disk/camera1.jpg', '/disk/camera2.jpg']
                    }, {
                        album: 'screenshots',
                        count: 3,
                        previews: ['/disk/screenshots1.jpg', '/disk/screenshots2.jpg']
                    }]
                }
            }),
            model: mockDescriptModuleFunctions({
                initSnapshot: {
                    data: {
                        photoslice_id: '1'
                    }
                },
                getResources: {
                    data: [{
                        id: '/disk/camera2.jpg',
                        meta: {
                            sizes: [{
                                url: 'https://downloader.yandex.net/camera2_default.jpg',
                                name: 'DEFAULT'
                            }]
                        }
                    }, {
                        id: '/disk/screenshots2.jpg',
                        meta: {
                            sizes: [{
                                url: 'https://downloader.yandex.net/screenshots2_default.jpg',
                                name: 'DEFAULT'
                            }]
                        }
                    }]
                }
            })
        };

        getAlbumsSlices().then((result) => {
            expect(result.object()).toEqual({
                albums: {
                    camera: {
                        id: 'camera',
                        count: 2,
                        preview: 'https://downloader.yandex.net/camera2_default.jpg'
                    },
                    screenshots: {
                        id: 'screenshots',
                        count: 3,
                        preview: 'https://downloader.yandex.net/screenshots2_default.jpg'
                    }
                }
            });
            expect(de._modules.cloudAPI).toHaveBeenCalled();
            expect(getModuleFunctionCalls(de._modules.model, 'initSnapshot')).toHaveLength(1);
            expect(getModuleFunctionCalls(de._modules.model, 'getResources')).toHaveLength(1);

            done();
        });
    });

    it('должен правильно отработать, если у некоторых ресурсов нет подходящих превью', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                albums: {
                    items: [{
                        album: 'camera',
                        count: 2,
                        previews: ['/disk/camera1.jpg', '/disk/camera2.jpg']
                    }, {
                        album: 'screenshots',
                        count: 3,
                        previews: ['/disk/screenshots1.jpg', '/disk/screenshots2.jpg']
                    }]
                }
            }),
            model: mockDescriptModuleFunctions({
                initSnapshot: {
                    data: {
                        photoslice_id: '1'
                    }
                },
                getResources: {
                    data: [{
                        id: '/disk/camera1.jpg',
                        meta: {
                            sizes: [{
                                url: 'https://downloader.yandex.net/camera1_default.jpg',
                                name: 'LARGE'
                            }]
                        }
                    }, {
                        id: '/disk/camera2.jpg',
                        meta: {
                            sizes: [{
                                url: 'https://downloader.yandex.net/camera2_default.jpg',
                                name: 'DEFAULT'
                            }]
                        }
                    }, {
                        id: '/disk/screenshots1.jpg',
                        meta: {
                            sizes: [{
                                url: 'https://downloader.yandex.net/screenshots1_default.jpg',
                                name: 'SMALL'
                            }]
                        }
                    }, {
                        id: '/disk/screenshots2.jpg',
                        meta: {
                            sizes: [{
                                url: 'https://downloader.yandex.net/screenshots2_default.jpg',
                                name: 'DEFAULT'
                            }]
                        }
                    }]
                }
            })
        };

        getAlbumsSlices().then((result) => {
            expect(result.object()).toEqual({
                albums: {
                    camera: {
                        id: 'camera',
                        count: 2,
                        preview: 'https://downloader.yandex.net/camera2_default.jpg'
                    },
                    screenshots: {
                        id: 'screenshots',
                        count: 3,
                        preview: 'https://downloader.yandex.net/screenshots2_default.jpg'
                    }
                }
            });
            expect(de._modules.cloudAPI).toHaveBeenCalled();
            expect(getModuleFunctionCalls(de._modules.model, 'initSnapshot')).toHaveLength(1);
            expect(getModuleFunctionCalls(de._modules.model, 'getResources')).toHaveLength(1);

            done();
        });
    });

    it('должен правильно отработать, если некоторые ресурсы исключены из альбома', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                albums: {
                    items: [{
                        album: 'camera',
                        count: 2,
                        previews: ['/disk/camera1.jpg', '/disk/camera2.jpg']
                    }, {
                        album: 'screenshots',
                        count: 3,
                        previews: ['/disk/screenshots1.jpg', '/disk/screenshots2.jpg']
                    }]
                }
            }),
            model: mockDescriptModuleFunctions({
                initSnapshot: {
                    data: {
                        photoslice_id: '1'
                    }
                },
                getResources: {
                    data: [{
                        id: '/disk/camera1.jpg',
                        meta: {
                            sizes: [{
                                url: 'https://downloader.yandex.net/camera1_default.jpg',
                                name: 'DEFAULT'
                            }],
                            albums_exclusions: ['camera']
                        }
                    }, {
                        id: '/disk/camera2.jpg',
                        meta: {
                            sizes: [{
                                url: 'https://downloader.yandex.net/camera2_default.jpg',
                                name: 'DEFAULT'
                            }]
                        }
                    }, {
                        id: '/disk/screenshots1.jpg',
                        meta: {
                            sizes: [{
                                url: 'https://downloader.yandex.net/screenshots1_default.jpg',
                                name: 'DEFAULT'
                            }],
                            albums_exclusions: ['screenshots']
                        }
                    }, {
                        id: '/disk/screenshots2.jpg',
                        meta: {
                            sizes: [{
                                url: 'https://downloader.yandex.net/screenshots2_default.jpg',
                                name: 'DEFAULT'
                            }]
                        }
                    }]
                }
            })
        };

        getAlbumsSlices().then((result) => {
            expect(result.object()).toEqual({
                albums: {
                    camera: {
                        id: 'camera',
                        count: 2,
                        preview: 'https://downloader.yandex.net/camera2_default.jpg'
                    },
                    screenshots: {
                        id: 'screenshots',
                        count: 3,
                        preview: 'https://downloader.yandex.net/screenshots2_default.jpg'
                    }
                }
            });
            expect(de._modules.cloudAPI).toHaveBeenCalled();
            expect(getModuleFunctionCalls(de._modules.model, 'initSnapshot')).toHaveLength(1);
            expect(getModuleFunctionCalls(de._modules.model, 'getResources')).toHaveLength(1);

            done();
        });
    });
});

/**
 * @param {Object} [params={}]
 *
 * @returns {Promise.<Object>}
 */
function getAlbumsSlices(params = {}) {
    const context = new de.Context({});
    const block = new de.Block.Include(blockPath);

    context.request = { headers: { 'x-request-id': 'bad8a8035fd82e2fc42a2b22b28cb7d8' } };

    return block.run(params, context);
}

/**
 * @param {any} returnValue
 *
 * @returns {Function}
 */
function mockDescriptModule(returnValue) {
    return jest.fn(() => {
        const result = new no.Promise();

        result.resolve(new de.Result.Value(returnValue));

        return result;
    });
}

/**
 * @param {Object.<string, any>} funcs
 *
 * @returns {Function}
 */
function mockDescriptModuleFunctions(funcs) {
    return jest.fn((params, context, func) => {
        const result = new no.Promise();
        const returnValue = funcs[func];

        result.resolve(new de.Result.Value(returnValue));

        return result;
    });
}

/**
 * @param mockedFn
 * @param funcName
 */
function getModuleFunctionCalls(mockedFn, funcName) {
    return mockedFn.mock.calls.filter(([,, callFuncName]) => funcName === callFuncName);
}
