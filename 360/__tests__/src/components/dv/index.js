import _ from 'lodash';
import DV from 'components/dv';

import React from 'react';
import { render } from 'enzyme';

jest.mock('../../../../src/lib/helpers/fullscreen');
jest.mock('@ps-int/ufo-rocks/lib/components/overdraft-content', () =>
    require('../../../../__mocks__/overdraft-content-mock')
);

const mockStore = (props) => ({
    getState: () => {
        return _.merge({
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
            url: {
                hostname: 'dv.ya.ru',
                query: {}
            },
            cfg: {
                passport: 'passport.ya.ru',
                isMounted: true,
                ua: {
                    OSFamily: 'MacOS',
                    isMobile: false
                },
                experiments: {
                    flags: {
                        dv_history_exp: true
                    }
                }
            },
            user: {
                auth: false,
                accounts: [],
                features: {}
            },
            doc: {
                pages: [],
                state: 'NO_URL', // содержимое тестируем в Content, здесь берём самую простую страничку
                actions: {
                    edit: {
                        state: ''
                    },
                    save: {
                        state: ''
                    },
                    share: {},
                    print: {},
                    download: {}
                }
            },
            archive: {
                state: '',
                fileActions: {
                    save: {
                        state: ''
                    }
                },
                selectedFile: {}
            },
            notifications: { current: null, items: [], state: 'CLOSED' }
        }, props || {});
    },
    subscribe: () => {},
    dispatch: () => {}
});

const runTest = (store) => {
    global.LANG = 'ru';
    const component = render(<DV store={store}/>);
    expect(component).toMatchSnapshot();
};

[
    [
        '',
        {}
    ],
    [
        'with custom direct exp on desktop',
        {
            dv_web_ad_custom_direct_exp: { topDesktopDirectId: 'topDesktopDirectId' }
        }
    ]
].forEach(([title, flags]) => {
    describe(title, () => {
        it('dv', () => {
            const store = mockStore();
            runTest(store);
        });

        it('dv-corp', () => {
            const store = mockStore({
                cfg: {
                    experiments: { flags },
                    isMounted: true,
                    isCorp: true
                }
            });
            runTest(store);
        });

        it('dv-embed', () => {
            const store = mockStore({
                cfg: {
                    experiments: { flags },
                    isMounted: true,
                    embed: 'dv'
                }
            });
            runTest(store);
        });

        it('tutor-embed', () => {
            const store = mockStore({
                cfg: {
                    experiments: { flags },
                    isMounted: true,
                    embed: 'tutor'
                }
            });
            runTest(store);
        });

        it('dv-with-edit-promo', () => {
            const store = mockStore({
                cfg: {
                    experiments: { flags }
                },
                doc: {
                    protocol: 'ya-browser:',
                    state: 'READY',
                    title: '',
                    actions: {
                        edit: {
                            allow: true,
                            url: 'edit-url'
                        }
                    }
                }
            });
            runTest(store);
        });

        it('dv-with-advertisement (default)', () => {
            const store = mockStore({
                cfg: {
                    experiments: { flags },
                    isMounted: true
                }
            });
            runTest(store);
        });

        it('dv-with-advertisement (experiment)', () => {
            const store = mockStore({
                cfg: {
                    isMounted: true,
                    experiments: {
                        flags: _.merge(_.cloneDeep(flags), {
                            dv_web_ad_custom_direct_exp: {
                                topDesktopDirectId: 'custom-top-desktop-direct-id'
                            }
                        })
                    }
                }
            });
            runTest(store);
        });

        it('dv-with-touch-advertisement (default)', () => {
            const store = mockStore({
                cfg: {
                    isMounted: true,
                    ua: {
                        isMobile: true
                    }
                }
            });
            runTest(store);
        });

        it('archive-with-advertisement', () => {
            const store = mockStore({
                doc: {
                    state: 'ARCHIVE'
                },
                cfg: {
                    experiments: { flags },
                    isMounted: true
                }
            });
            runTest(store);
        });

        it('mobile-dv', () => {
            const store = mockStore({
                cfg: {
                    experiments: { flags },
                    isMounted: true,
                    ua: {
                        isMobile: true
                    }
                }
            });
            runTest(store);
        });

        it('dv with fullscreen-slider', () => {
            const store = mockStore({
                cfg: {
                    experiments: { flags },
                    isMounted: true,
                    ua: {}
                },
                doc: {
                    title: 'вжух-вжух.pdf',
                    state: 'READY',
                    contentFamily: 'presentation',
                    pages: [
                        {
                            index: 1,
                            state: 'WAIT'
                        }, {
                            index: 2,
                            state: 'READY'
                        }, {
                            index: 3
                        }
                    ]
                }
            });

            global.ALLOW_FULLSCREEN = true;
            runTest(store);
            global.ALLOW_FULLSCREEN = false;
        });
    });
});

describe('mobile', () => {
    it('disk_dv_in_docs_touch_exp', () => {
        const store = mockStore({
            doc: {
                title: 'wow.pdf',
                pages: [],
                state: 'READY',
                iframe: true,
                contentType: 'application/pdf',
                url: '/some/iframe/url'
            },
            cfg: {
                isMounted: true,
                ua: { isMobile: true },
                embed: 'docs',
                experiments: {
                    flags: { disk_dv_in_docs_touch_exp: true }
                }
            }
        });
        runTest(store);
    });
});
