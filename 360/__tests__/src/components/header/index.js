import Header from 'components/header';
import React from 'react';
import { render } from 'enzyme';
import init from 'store';
import { Provider } from 'react-redux';

const runTest = (state) => {
    global.LANG = 'ru';
    const store = init(state);
    const component = render(
        <Provider store={store}>
            <Header />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

it('header-auth-all-actions', () => {
    const state = {
        url: {
            hostname: 'dv.ya.ru'
        },
        cfg: {
            passport: 'passport.ya.ru',
            ua: {},
            experiments: {
                flags: {
                    dv_history_exp: true
                }
            }
        },
        user: {
            auth: true,
            accounts: [{
                id: '4004594257',
                login: 'iegit20',
                name: 'iegit20',
                avatar: 'https://avatars.mdst.yandex.net/get-yapic/0/0-0/islands-middle'
            }]
        },
        doc: {
            title: 'тюлень_олень_и_еноты.odt',
            state: 'READY',
            actions: {
                save: {
                    allow: true
                },
                edit: {
                    allow: true,
                    url: '/some/edit/url'
                },
                share: {
                    allow: true
                },
                print: {
                    allow: true
                },
                download: {
                    allow: true
                }
            }
        }
    };
    runTest(state);
});

it('header-archive-all-actions', () => {
    const state = {
        url: {
            hostname: 'dv.ya.ru'
        },
        cfg: {
            passport: 'passport.ya.ru',
            ua: {},
            experiments: {
                flags: {
                    dv_history_exp: true
                }
            }
        },
        user: {
            auth: true,
            accounts: [{
                id: '4004594257',
                login: 'iegit20',
                name: 'iegit20',
                avatar: 'https://avatars.mdst.yandex.net/get-yapic/0/0-0/islands-middle'
            }]
        },
        doc: {
            title: 'трололо.zip',
            state: 'ARCHIVE',
            actions: {
                save: {
                    allow: true
                },
                share: {
                    allow: true
                },
                download: {
                    allow: true
                }
            }
        }
    };
    runTest(state);
});

it('header-no-auth-some-actions', () => {
    const state = {
        url: {
            hostname: 'dv.ya.ru'
        },
        cfg: {
            passport: 'passport.ya.ru',
            ua: {},
            experiments: {
                flags: {}
            }
        },
        user: {
            auth: false,
            accounts: []
        },
        doc: {
            title: 'вжух-вжух',
            state: 'READY',
            actions: {
                save: {
                    allow: true,
                    buttonUrl: 'save/url'
                },
                edit: {
                    allow: false
                },
                share: {
                    allow: false
                },
                print: {
                    allow: true
                },
                download: {
                    allow: true
                }
            }
        }
    };
    runTest(state);
});

it('header-auth-2-accounts-and-saved', () => {
    const state = {
        url: {
            hostname: 'dv.ya.ru'
        },
        cfg: {
            passport: 'passport.ya.ru',
            ua: {},
            experiments: {
                flags: {}
            }
        },
        user: {
            auth: true,
            accounts: [
                {
                    id: '4004594257',
                    login: 'iegit20',
                    name: 'iegit20',
                    avatar: 'https://avatars.mdst.yandex.net/get-yapic/0/0-0/islands-middle'
                },
                {
                    id: '4004594258',
                    login: 'kri0-gen',
                    name: 'kri0-gen',
                    avatar: 'https://avatars.mdst.yandex.net/get-yapic/1/0-0/islands-middle'
                }
            ]
        },
        doc: {
            title: 'вжух-вжух',
            state: 'READY',
            actions: {
                save: {
                    allow: true,
                    state: 'SAVED'
                },
                edit: {
                    allow: true
                },
                share: {
                    allow: true
                },
                print: {
                    allow: false
                },
                download: {
                    allow: false
                }
            }
        }
    };
    runTest(state);
});

it('header-no-auth-no-actions', () => {
    const state = {
        url: {
            hostname: 'dv.ya.ru'
        },
        cfg: {
            passport: 'passport.ya.ru',
            ua: {},
            experiments: {
                flags: {}
            }
        },
        user: {
            auth: false,
            accounts: []
        },
        doc: {
            title: 'вжух-вжух',
            state: 'READY',
            actions: {
                save: {
                    allow: false
                },
                edit: {
                    allow: false
                },
                share: {
                    allow: false
                },
                print: {
                    allow: false
                },
                download: {
                    allow: false
                }
            }
        }
    };
    runTest(state);
});

it('header-conversion-error', () => {
    const state = {
        url: {
            hostname: 'dv.ya.ru'
        },
        cfg: {
            passport: 'passport.ya.ru',
            ua: {},
            experiments: {
                flags: {}
            }
        },
        user: {
            auth: false,
            accounts: []
        },
        doc: {
            title: 'вжух-вжух',
            state: 'FAIL',
            actions: {
                save: {
                    allow: true
                },
                edit: {
                    allow: true
                },
                share: {
                    allow: true
                },
                print: {
                    allow: true
                },
                download: {
                    allow: true
                }
            }
        }
    };
    runTest(state);
});

it('header-conversion-error-file-is-downloadable', () => {
    const state = {
        url: {
            hostname: 'dv.ya.ru'
        },
        cfg: {
            passport: 'passport.ya.ru',
            ua: {},
            experiments: {
                flags: {}
            }
        },
        user: {
            auth: false,
            accounts: []
        },
        doc: {
            title: 'вжух-вжух',
            state: 'FAIL',
            actions: {

                save: {
                    allow: true
                },
                edit: {
                    allow: true
                },
                share: {
                    allow: true
                },
                print: {
                    allow: true
                },
                download: {
                    allow: true,
                    isDownloadable: true
                }
            }
        }
    };
    runTest(state);
});

it('header-version', () => {
    const state = {
        url: {
            hostname: 'dv.ya.ru'
        },
        cfg: {
            passport: 'passport.ya.ru',
            ua: {},
            experiments: {
                flags: {}
            }
        },
        user: {
            auth: true,
            accounts: [{
                id: '4007552028',
                login: 'follet',
                name: 'follet',
                avatar: 'https://avatars.mdst.yandex.net/get-yapic/0/0-0/islands-middle'
            }]
        },
        doc: {
            title: '02_versions_test.txt',
            state: 'READY',
            actions: {
                save: {
                    allow: false
                },
                edit: {
                    allow: false
                },
                share: {
                    allow: false
                },
                print: {
                    allow: false
                },
                download: {
                    allow: false
                }
            },
            versionDate: 1522137693000
        }
    };
    runTest(state);
});
