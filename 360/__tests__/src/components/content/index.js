import _ from 'lodash';
import Content from 'components/content';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { Provider } from 'react-redux';

[
    [
        '',
        {}
    ],
    [
        'with dv_web_ad_custom_direct_exp experiment',
        {
            dv_web_ad_custom_direct_exp: {
                bottomWideDesktopDirectId: 'bottomWideDesktopDirectId',
                bottomNormalDesktopDirectId: 'bottomNormalDesktopDirectId',
                bottomNarrowDesktopDirectId: 'bottomNarrowDesktopDirectId'
            }
        }
    ]
].forEach(([title, flags]) => {
    const runTest = (state) => {
        global.LANG = 'ru';
        global.requestAnimationFrame = () => {};
        const store = init(Object.assign({
            direct: {
                names: {
                    placeholder: 'placeholder',
                    direct: 'direct',
                    'top-direct': 'top-direct',
                    'bottom-direct': 'bottom-direct',
                    'top-0': 'top-0',
                    'bottom-0': 'bottom-0'
                },
                orders: {
                    top: [true],
                    bottom: [false]
                }
            },
            cfg: {
                ua: {},
                experiments: { flags }
            },
            url: {
                query: {}
            },
            user: { features: {} }
        }, state));
        const component = render(
            <Provider store={store}>
                <Content />
            </Provider>
        );
        expect(component).toMatchSnapshot();
    };

    describe(title, () => {
        it('content-no-url', () => {
            const state = {
                doc: {
                    pages: [],
                    state: 'NO_URL'
                }
            };

            runTest(state);
        });

        it('content-wait', () => {
            const state = {
                doc: {
                    pages: [],
                    state: 'WAIT'
                }
            };

            runTest(state);
        });

        it('content-error-404', () => {
            const state = {
                doc: {
                    pages: [],
                    state: 'FAIL',
                    errorCode: '404'
                }
            };

            runTest(state);
        });

        it('content-iframe', () => {
            const state = {
                doc: {
                    title: 'wow.pdf',
                    pages: [],
                    state: 'READY',
                    iframe: true,
                    contentType: 'application/pdf',
                    url: '/some/iframe/url'
                }
            };

            runTest(state);
        });

        it('content-html-1-page', () => {
            const state = {
                doc: {
                    title: 'тюлень_олень_и_еноты.odt',
                    state: 'READY',
                    pages: [
                        {
                            index: 1,
                            state: 'READY',
                            html: '<span>вжух</span>'
                        }
                    ],
                    actions: {}
                }
            };

            runTest(state);
        });

        it('content-html-3-pages', () => {
            const state = {
                url: {
                    query: {
                        page: 1
                    }
                },
                doc: {
                    title: 'тюлень_олень_и_еноты.odt',
                    state: 'READY',
                    pages: [
                        {
                            index: 1,
                            state: 'READY',
                            html: '<div>yo</div>'
                        }, {
                            index: 2,
                            state: 'FAIL'
                        }, {
                            index: 3,
                            state: 'WAIT'
                        }
                    ],
                    actions: {}
                }
            };

            runTest(state);
        });

        it('content-html-2-pages-withoutSize (page load not started)', () => {
            const state = {
                doc: {
                    title: 'тюлень_олень_и_еноты.odt',
                    state: 'READY',
                    withoutSize: true,
                    pages: [
                        {
                            index: 1
                        }, {
                            index: 2
                        }
                    ],
                    actions: {}
                },
                url: {
                    query: {
                        page: 2
                    }
                }
            };

            runTest(state);
        });

        it('content-html-3-pages-withoutSize (page is loading)', () => {
            const state = {
                doc: {
                    title: 'тюлень_олень_и_еноты.odt',
                    state: 'READY',
                    withoutSize: true,
                    pages: [
                        {
                            index: 1
                        }, {
                            index: 2,
                            state: 'WAIT'
                        }, {
                            index: 3
                        }
                    ],
                    actions: {}
                },
                url: {
                    query: {
                        page: 2
                    }
                }
            };

            runTest(state);
        });

        it('content-html-4-pages-withoutSize (page ready)', () => {
            const state = {
                doc: {
                    title: 'тюлень_олень_и_еноты.odt',
                    state: 'READY',
                    withoutSize: true,
                    pages: [
                        {
                            index: 1
                        }, {
                            index: 2
                        }, {
                            index: 3,
                            state: 'READY',
                            html: '<div>some content</div>'
                        }, {
                            index: 4
                        }
                    ],
                    actions: {}
                },
                url: {
                    query: {
                        page: 3
                    }
                }
            };

            runTest(state);
        });

        it('content-archive-fail', () => {
            const state = {
                doc: {
                    pages: [],
                    state: 'ARCHIVE',
                    errorCode: 'FILE_IS_PASSWORD_PROTECTED'
                },
                archive: {
                    state: 'FAIL'
                }
            };

            runTest(state);
        });

        it('content-archive-wait', () => {
            const state = {
                doc: {
                    pages: [],
                    state: 'ARCHIVE'
                },
                archive: {
                    state: 'WAIT'
                }
            };

            runTest(state);
        });

        it('content-archive-READY', () => {
            const state = {
                doc: {
                    pages: [],
                    title: 'my-archive.zip',
                    state: 'ARCHIVE'
                },
                archive: {
                    state: 'READY',
                    path: '',
                    listing: {},
                    selectedFile: {
                        path: ''
                    }
                }
            };

            runTest(state);
        });

        const ONE_PAGE_DOC_STORE_MOCK = {
            title: 'тюлень_олень_и_еноты.odt',
            state: 'READY',
            pages: [
                {
                    index: 1,
                    state: 'READY',
                    html: '<span>вжух</span>'
                }
            ],
            actions: {}
        };

        it('content-with-touch-advertisement (default)', () => {
            const state = {
                doc: ONE_PAGE_DOC_STORE_MOCK,
                cfg: {
                    isMounted: true,
                    ua: {
                        isMobile: true
                    },
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {})
                    }
                }
            };

            runTest(state);
        });

        it('content-with-touch-advertisement (experiment)', () => {
            const state = {
                doc: ONE_PAGE_DOC_STORE_MOCK,
                cfg: {
                    isMounted: true,
                    ua: {
                        isMobile: true
                    },
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {
                            dv_web_ad_custom_direct_exp: {
                                bottomTouchDirectId: 'custom-bottom-touch-direct-id'
                            }
                        })
                    }
                }
            };

            runTest(state);
        });

        it('content-with-advertisement (width > 1800, default)', () => {
            global.window.innerWidth = 1801;
            const state = {
                doc: ONE_PAGE_DOC_STORE_MOCK,
                cfg: {
                    isMounted: true,
                    ua: {},
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {})
                    }
                }
            };

            runTest(state);
        });

        it('content-with-advertisement (width > 1800, experiment)', () => {
            global.window.innerWidth = 1801;
            const state = {
                doc: ONE_PAGE_DOC_STORE_MOCK,
                cfg: {
                    isMounted: true,
                    ua: {},
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {
                            dv_web_ad_custom_direct_exp: {
                                bottomWideDesktopDirectId: 'custom-bottom-wide-desktop-direct-id'
                            }
                        })
                    }
                }
            };

            runTest(state);
        });

        it('content-with-advertisement (1400 < width < 1800, default)', () => {
            global.window.innerWidth = 1401;
            const state = {
                doc: ONE_PAGE_DOC_STORE_MOCK,
                cfg: {
                    isMounted: true,
                    ua: {},
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {})
                    }
                }
            };

            runTest(state);
        });

        it('content-with-advertisement (1400 < width < 1800, experiment)', () => {
            global.window.innerWidth = 1401;
            const state = {
                doc: ONE_PAGE_DOC_STORE_MOCK,
                cfg: {
                    isMounted: true,
                    ua: {},
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {
                            dv_web_ad_custom_direct_exp: {
                                bottomNormalDesktopDirectId: 'custom-bottom-normal-desktop-direct-id'
                            }
                        })
                    }
                }
            };

            runTest(state);
        });

        it('content-with-advertisement (width < 1400, default)', () => {
            global.window.innerWidth = 1399;
            const state = {
                doc: ONE_PAGE_DOC_STORE_MOCK,
                cfg: {
                    isMounted: true,
                    ua: {},
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {})
                    }
                }
            };

            runTest(state);
        });

        it('content-with-advertisement (width < 1400, experiment)', () => {
            global.window.innerWidth = 1399;
            const state = {
                doc: ONE_PAGE_DOC_STORE_MOCK,
                cfg: {
                    isMounted: true,
                    ua: {},
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {
                            dv_web_ad_custom_direct_exp: {
                                bottomNarrowDesktopDirectId: 'custom-bottom-narrow-desktop-direct-id'
                            }
                        })
                    }
                }
            };

            runTest(state);
        });

        it('error-with-advertisement (default)', () => {
            global.window.innerWidth = 1000;
            const state = {
                doc: {
                    state: 'FAIL',
                    pages: []
                },
                cfg: {
                    isMounted: true,
                    ua: {},
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {})
                    }
                }
            };

            runTest(state);
        });

        it('error-with-advertisement (experiment)', () => {
            global.window.innerWidth = 1000;
            const state = {
                doc: {
                    state: 'FAIL',
                    pages: []
                },
                cfg: {
                    isMounted: true,
                    ua: {},
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {
                            dv_web_ad_custom_direct_exp: {
                                bottomNarrowDesktopDirectId: 'custom-bottom-narrow-desktop-direct-id'
                            }
                        })
                    }
                }
            };

            runTest(state);
        });

        it('archive-with-mobile-advertisement (default)', () => {
            const state = {
                doc: {
                    state: 'ARCHIVE',
                    pages: [],
                    title: 'Мой архив.zip'
                },
                archive: {
                    state: 'READY',
                    path: '',
                    listing: {},
                    selectedFile: {
                        path: ''
                    }
                },
                cfg: {
                    isMounted: true,
                    ua: {
                        isMobile: true
                    },
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {})
                    }
                }
            };

            runTest(state);
        });

        it('archive-with-mobile-advertisement (experiment)', () => {
            const state = {
                doc: {
                    state: 'ARCHIVE',
                    pages: [],
                    title: 'Мой архив.zip'
                },
                archive: {
                    state: 'READY',
                    path: '',
                    listing: {},
                    selectedFile: {
                        path: ''
                    }
                },
                cfg: {
                    isMounted: true,
                    ua: {
                        isMobile: true
                    },
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {
                            dv_web_ad_custom_direct_exp: {
                                bottomTouchDirectId: 'custom-bottom-touch-direct-id'
                            }
                        })
                    }
                }
            };

            runTest(state);
        });
    });
});
