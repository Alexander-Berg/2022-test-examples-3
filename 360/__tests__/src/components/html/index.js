import HTML from 'components/doc/html';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { Provider } from 'react-redux';

jest.mock('../../../../src/lib/helpers/fullscreen');

const runTest = (state, backend) => {
    global.LANG = 'ru';

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
        }
    }, state), backend);

    const component = render(
        <Provider store={store}>
            <HTML />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

[
    [
        '',
        {}
    ],
    [
        'with custom direct on desktop',
        {
            dv_web_ad_custom_direct_exp: { topDesktopDirectId: 'topDesktopDirectId' }
        }
    ]
].forEach(([title, flags]) => {
    describe(title, () => {
        const cfg = {
            ua: {},
            experiments: { flags }
        };
        const user = {
            features: {}
        };

        it('html-3-pages-wo-size-no-spins', () => {
            const state = {
                cfg,
                user,
                url: {
                    query: {
                        page: 2
                    }
                },
                doc: {
                    actions: {},
                    title: 'вжух-вжух.xlsx',
                    contentFamily: 'spreadsheet',
                    withoutSize: true,
                    pages: [
                        {
                            index: 1,
                            state: 'FAIL'
                        }, {
                            index: 2,
                            state: 'READY',
                            html: '<div>yo</div>'
                        }, {
                            index: 3,
                            state: 'FAIL'
                        }
                    ]
                }
            };

            runTest(state);
        });

        it('html-3-pages-wo-size-with-spins', () => {
            const state = {
                cfg,
                user,
                url: {
                    query: {
                        page: 2
                    }
                },
                doc: {
                    actions: {},
                    title: 'тра-та-та',
                    contentFamily: 'spreadsheet',
                    withoutSize: true,
                    pages: [
                        {
                            index: 1,
                            state: 'WAIT'
                        }, {
                            index: 2,
                            state: 'READY',
                            html: '<div>yo</div>'
                        }, {
                            index: 3,
                            state: ''
                        }
                    ]
                }
            };

            jest.mock('lib/backend');
            const backendMock = require('lib/backend').default;
            runTest(state, backendMock);
        });

        it('html-4-pages-with-size', () => {
            const state = {
                cfg,
                user,
                url: {
                    query: {
                        page: 3
                    }
                },
                doc: {
                    actions: {},
                    title: 'вжух-вжух.pdf',
                    contentFamily: 'pdf',
                    withoutSize: false,
                    scale: 0.9,
                    pages: [
                        {
                            index: 1,
                            state: 'WAIT',
                            width: 400,
                            height: 300
                        }, {
                            index: 2,
                            state: 'FAIL',
                            width: 200,
                            height: 380
                        }, {
                            index: 3,
                            state: 'READY',
                            html: '<div>this is magic</div>',
                            width: 500,
                            height: 900
                        }, {
                            index: 4,
                            width: 600,
                            height: 800
                        }
                    ]
                }
            };

            runTest(state);
        });

        it('html with show only one page', () => {
            const state = {
                user,
                url: {
                    query: {
                        page: 2
                    }
                },
                cfg,
                doc: {
                    actions: {},
                    title: 'вжух-вжух.pdf',
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
            };

            global.ALLOW_FULLSCREEN = true;
            runTest(state);
            global.ALLOW_FULLSCREEN = false;
        });

        const getState = (protocol) => ({
            cfg,
            user,
            doc: {
                actions: {},
                protocol,
                serpUrl: (/s:$/.test(protocol) ? 'https' : 'http') + '://ya.ru/docs/doc-from-serp.pdf',
                serpHost: 'ya.ru',
                serpLastAccess: 1488292801428,
                title: 'doc-from-serp.pdf',
                pages: []
            }
        });
        it('html for doc from http', () => {
            runTest(getState('http:'));
        });

        it('html for doc from https', () => {
            runTest(getState('https:'));
        });

        it('html for doc from ya-serp', () => {
            runTest(getState('ya-serp:'));
        });

        it('html for doc from ya-serps', () => {
            runTest(getState('ya-serps:'));
        });
    });
});
