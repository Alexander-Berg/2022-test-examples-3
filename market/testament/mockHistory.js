// @flow

import {splitTestingUrlByPath} from './url';

const defaultMock = {
    ...(global.window ? window.history : {}),
    back: jest.fn(),
};

type Props = {
    length: number,
};

export default (props: Props) => {
    if (typeof window === 'undefined') {
        return () => {};
    }

    const {history} = window;
    delete window.history;
    delete global.history;

    const historyStateImpl = (_, __, url) => {
        window.location.href = url;

        const [pathname, search] = splitTestingUrlByPath(url);
        window.location.pathname = pathname;
        window.location.search = `_${search}`;
    };

    window.history = {
        ...defaultMock,
        length: props.length,
        replaceState: historyStateImpl,
        pushState: historyStateImpl,
    };


    return () => {
        global.history = history;
        window.history = history;
    };
};
