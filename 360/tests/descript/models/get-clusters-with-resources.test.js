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

const blockPath = 'components/models/get-clusters-with-resources/get-clusters-with-resources.jsx';
const blockAbsolutePath = path.resolve(blockPath);

describe('get-clusters-with-resources', () => {
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

    it('не должен делать запросов в `cloudAPI` и `mpfs`, если нечего запрашивать', (done) => {
        de._modules = {
            cloudAPI: jest.fn(),
            model: jest.fn()
        };

        fetchClustersAndResources({}).then((result) => {
            expect(result.object()).toEqual({
                clusters: { fetched: [], missing: [] },
                resources: { fetched: [], missing: [] }
            });

            expect(de._modules.cloudAPI).not.toHaveBeenCalled();
            expect(de._modules.model).not.toHaveBeenCalled();

            done();
        });
    });

    it('должен запросить только ресурсы, если кластера уже загружены', (done) => {
        de._modules = {
            cloudAPI: jest.fn(),
            model: mockDescriptModule({
                data: [{
                    id: '/cluster1resource1'
                }, {
                    id: '/cluster1resource2'
                }]
            })
        };

        fetchClustersAndResources({
            cluster1: {
                resources: ['/cluster1resource1', '/cluster1resource2']
            }
        }).then((result) => {
            expect(de._modules.cloudAPI).not.toHaveBeenCalled();
            expect(result.object()).toEqual({
                clusters: { fetched: [], missing: [] },
                resources: { fetched: [{
                    clusterId: 'cluster1',
                    id: '/cluster1resource1'
                }, {
                    clusterId: 'cluster1',
                    id: '/cluster1resource2'
                }], missing: [] }
            });

            done();
        });
    });

    it('должен запросить кластеры и ресурсы', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                items: [{
                    cluster_id: 'cluster1',
                    items: [{
                        path: '/cluster1resource1'
                    }, {
                        path: '/cluster1resource2'
                    }]
                }]
            }),
            model: mockDescriptModule({
                data: [{
                    id: '/cluster1resource1'
                }, {
                    id: '/cluster2resource1'
                }]
            })
        };

        fetchClustersAndResources({
            cluster1: {
                range: [0, 0]
            },
            cluster2: {
                resources: ['/cluster2resource1']
            }
        }).then((result) => {
            expect(result.object()).toEqual({
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 2,
                        albums: null,
                        items: [{ id: '/cluster1resource1' }, { id: '/cluster1resource2' }]
                    }],
                    missing: []
                },
                resources: {
                    fetched: [{
                        clusterId: 'cluster1',
                        id: '/cluster1resource1'
                    }, {
                        clusterId: 'cluster2',
                        id: '/cluster2resource1'
                    }],
                    missing: []
                }
            });

            done();
        });
    });

    it('не должен упасть, если в требуемом интервале нет ресурсов', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                items: [{
                    cluster_id: 'cluster1',
                    items: [{
                        path: '/cluster1resource1'
                    }]
                }]
            }),
            model: mockDescriptModule({
                data: [{
                    id: '/cluster1resource1'
                }]
            })
        };

        fetchClustersAndResources({
            cluster1: {
                range: [0, 1]
            }
        }).then((result) => {
            expect(result.object()).toEqual({
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 1,
                        albums: null,
                        items: [{ id: '/cluster1resource1' }]
                    }],
                    missing: []
                },
                resources: {
                    fetched: [{
                        clusterId: 'cluster1',
                        id: '/cluster1resource1'
                    }],
                    missing: []
                }
            });

            done();
        });
    });

    it('должен вернуть ошибку, если не удалось получить кластера', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                error: {
                    id: 'HTTP_502'
                }
            }),
            model: jest.fn()
        };

        fetchClustersAndResources({
            cluster1: {
                range: [0, 1]
            },
            cluster2: {
                resources: ['/cluster2resource1']
            }
        }).then((result) => {
            expect(de._modules.model).not.toHaveBeenCalled();
            expect(result.object()).toEqual({
                error: {
                    id: 'HTTP_502',
                    requestId: 'bad8a8035fd82e2fc42a2b22b28cb7d8'
                }
            });

            done();
        });
    });

    it('должен правильно обрабатывать ошибку получения ресурсов `{ error: {} }`', (done) => {
        de._modules = {
            cloudAPI: jest.fn(),
            model: mockDescriptModule({
                error: {
                    id: 'MODULE_NOT_FOUND'
                }
            })
        };

        fetchClustersAndResources({
            cluster1: {
                resources: ['/cluster1resource1']
            }
        }).then((result) => {
            expect(de._modules.cloudAPI).not.toHaveBeenCalled();
            expect(result.object()).toEqual({
                error: {
                    id: 'MODULE_NOT_FOUND',
                    requestId: 'bad8a8035fd82e2fc42a2b22b28cb7d8'
                },
                requested: ['/cluster1resource1']
            });

            done();
        });
    });

    it('должен правильно обрабатывать ошибку получения ресурсов `{ data: { error: {} } }`', (done) => {
        de._modules = {
            cloudAPI: jest.fn(),
            model: mockDescriptModule({
                data: {
                    error: {
                        id: 'HTTP_502'
                    }
                }
            })
        };

        fetchClustersAndResources({
            cluster1: {
                resources: ['/cluster1resource1']
            }
        }).then((result) => {
            expect(de._modules.cloudAPI).not.toHaveBeenCalled();
            expect(result.object()).toEqual({
                error: {
                    id: 'HTTP_502',
                    requestId: 'bad8a8035fd82e2fc42a2b22b28cb7d8'
                },
                requested: ['/cluster1resource1']
            });

            done();
        });
    });

    it('должен отдать полученные кластера, даже если есть ошибка в ручке ресурсов', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                items: [{
                    cluster_id: 'cluster1',
                    items: [{
                        path: '/cluster1resource1'
                    }]
                }]
            }),
            model: mockDescriptModule({
                data: {
                    error: {
                        id: 'HTTP_502'
                    }
                }
            })
        };

        fetchClustersAndResources({
            cluster1: {
                range: [0, 0]
            }
        }).then((result) => {
            expect(result.object()).toEqual({
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 1,
                        items: [{ id: '/cluster1resource1' }]
                    }],
                    missing: []
                },
                resources: {
                    error: {
                        id: 'HTTP_502',
                        requestId: 'bad8a8035fd82e2fc42a2b22b28cb7d8'
                    },
                    requested: ['/cluster1resource1']
                }
            });

            done();
        });
    });

    it('должен обновить размер кластера, если есть ненайденные ресурсы', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                items: [{
                    cluster_id: 'cluster1',
                    items: [{
                        path: '/cluster1resource1'
                    }, {
                        path: '/cluster1resource2'
                    }, {
                        path: '/cluster1resource3'
                    }]
                }]
            }),
            model: mockDescriptModule({
                data: [{
                    id: '/cluster1resource1'
                }]
            })
        };

        fetchClustersAndResources({
            cluster1: {
                range: [0, 1]
            }
        }).then((result) => {
            expect(result.object()).toEqual({
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 2,
                        albums: null,
                        items: [{ id: '/cluster1resource1' }, { id: '/cluster1resource3' }]
                    }],
                    missing: []
                },
                resources: {
                    fetched: [{
                        clusterId: 'cluster1',
                        id: '/cluster1resource1'
                    }],
                    missing: ['/cluster1resource2']
                }
            });
            expect(de.log.info).toBeCalledWith('missing photoslice items / uid: 12345 / missing resources (1): /cluster1resource2');

            done();
        });
    });

    it('должен вернуть ненайденные кластера', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                items: [{
                    cluster_id: 'cluster1',
                    items: [{
                        path: '/cluster1resource1'
                    }]
                }]
            }),
            model: mockDescriptModule({
                data: [{
                    id: '/cluster1resource1'
                }]
            })
        };

        fetchClustersAndResources({
            cluster1: {
                range: [0, 0]
            },
            cluster2: {
                range: [0, 1]
            }
        }).then((result) => {
            expect(result.object()).toEqual({
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 1,
                        albums: null,
                        items: [{ id: '/cluster1resource1' }]
                    }],
                    missing: ['cluster2']
                },
                resources: {
                    fetched: [{
                        clusterId: 'cluster1',
                        id: '/cluster1resource1'
                    }],
                    missing: []
                }
            });
            expect(de.log.info).toBeCalledWith('missing photoslice items / uid: 12345 / missing clusters (1): cluster2');

            done();
        });
    });

    it('должен вернуть ненайденные кластера, если для кластера не нашлось ни одного ресурса', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                items: [{
                    cluster_id: 'cluster1',
                    items: [{
                        path: '/cluster1resource1'
                    }]
                }]
            }),
            model: mockDescriptModule({
                data: []
            })
        };

        fetchClustersAndResources({
            cluster1: {
                range: [0, 0]
            }
        }).then((result) => {
            expect(result.object()).toEqual({
                clusters: {
                    fetched: [],
                    missing: ['cluster1']
                },
                resources: {
                    fetched: [],
                    missing: ['/cluster1resource1']
                }
            });
            expect(de.log.info).toBeCalledWith('missing photoslice items / uid: 12345 / missing clusters (1): cluster1 / missing resources (1): /cluster1resource1');

            done();
        });
    });

    it('должен выбирать id ресурсов с учётом фильтров', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                items: [{
                    cluster_id: 'cluster1',
                    items: [
                        { path: '/cluster1resource1', albums: ['beautiful'] },
                        { path: '/cluster1resource2', albums: ['beautiful'] },
                        { path: '/cluster1resource3', albums: ['unbeautiful'] },
                        { path: '/cluster1resource4', albums: ['beautiful'] },
                        { path: '/cluster1resource5', albums: ['unbeautiful'] }
                    ]
                }]
            }),
            model: mockDescriptModule({
                data: [
                    { id: '/cluster1resource3' },
                    { id: '/cluster1resource5' }
                ]
            })
        };

        fetchClustersAndResources({
            cluster1: {
                range: [0, 1]
            }
        }, { filter: 'unbeautiful' }).then((result) => {
            expect(result.object()).toEqual({
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 5,
                        items: [
                            { id: '/cluster1resource1', albums: ['beautiful'] },
                            { id: '/cluster1resource2', albums: ['beautiful'] },
                            { id: '/cluster1resource3', albums: ['unbeautiful'] },
                            { id: '/cluster1resource4', albums: ['beautiful'] },
                            { id: '/cluster1resource5', albums: ['unbeautiful'] }
                        ],
                        albums: {
                            beautiful: 3,
                            unbeautiful: 2,
                        }
                    }],
                    missing: []
                },
                resources: {
                    fetched: [
                        { clusterId: 'cluster1', id: '/cluster1resource3' },
                        { clusterId: 'cluster1', id: '/cluster1resource5' }
                    ],
                    missing: []
                }
            });
            done();
        });
    });

    it('должен обновить размеры альбомов если есть missing-ресурсы', (done) => {
        de._modules = {
            cloudAPI: mockDescriptModule({
                items: [{
                    cluster_id: 'cluster1',
                    items: [
                        { path: '/cluster1resource1', albums: ['beautiful'] },
                        { path: '/cluster1resource2', albums: ['beautiful'] },
                        { path: '/cluster1resource3', albums: ['unbeautiful'] },
                        { path: '/cluster1resource4', albums: ['beautiful'] },
                        { path: '/cluster1resource5', albums: ['unbeautiful'] }
                    ]
                }]
            }),
            model: mockDescriptModule({
                data: [
                    { id: '/cluster1resource3' }
                ]
            })
        };

        fetchClustersAndResources({
            cluster1: {
                range: [0, 1]
            }
        }, { filter: 'unbeautiful' }).then((result) => {
            expect(result.object()).toEqual({
                clusters: {
                    fetched: [{
                        id: 'cluster1',
                        size: 4,
                        items: [
                            { id: '/cluster1resource1', albums: ['beautiful'] },
                            { id: '/cluster1resource2', albums: ['beautiful'] },
                            { id: '/cluster1resource3', albums: ['unbeautiful'] },
                            { id: '/cluster1resource4', albums: ['beautiful'] }
                        ],
                        albums: {
                            beautiful: 3,
                            unbeautiful: 1,
                        }
                    }],
                    missing: []
                },
                resources: {
                    fetched: [
                        { clusterId: 'cluster1', id: '/cluster1resource3' }
                    ],
                    missing: ['/cluster1resource5']
                }
            });
            done();
        });
    });
});

/**
 * @param {Object.<string|import('../../../components/redux/store/actions/photo').ClusterRequest|import('../../../components/redux/store/actions/photo').ClusterResourcesRequest>} clusters
 * @param {ClustersParams} params
 *
 * @returns {Promise.<Object>}
 */
function fetchClustersAndResources(clusters, params) {
    const context = new de.Context({});
    const block = new de.Block.Include(blockPath);

    context.request = { headers: { 'x-request-id': 'bad8a8035fd82e2fc42a2b22b28cb7d8' } };

    return block.run(Object.assign({ clusters: JSON.stringify(clusters), user: { uid: '12345' } }, params), context);
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
