// @flow

export default () => {
    if (typeof window === 'undefined') {
        return () => {
        };
    }

    const {scrollTo} = window;
    delete window.scrollTo;
    window.scrollTo = jest.fn();

    return () => {
        window.scrollTo = scrollTo;
    };
};
