// @flow

export default () => {
    if (typeof window === 'undefined') {
        return () => {};
    }

    const {open, close} = window;

    window.open = jest.fn();
    window.close = jest.fn();

    // eslint-disable-next-line no-return-assign
    return () => {
        window.open = open;
        window.close = close;
    };
};
