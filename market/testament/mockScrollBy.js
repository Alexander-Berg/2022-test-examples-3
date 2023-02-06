// @flow

export default () => {
    if (typeof window === 'undefined') {
        return () => {
        };
    }

    const {scrollBy} = window;
    delete window.scrollBy;
    window.scrollBy = jest.fn();

    return () => {
        window.scrollBy = scrollBy;
    };
};
