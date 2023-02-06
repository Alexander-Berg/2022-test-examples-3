// @flow

export default () => {
    if (typeof window === 'undefined') {
        return () => {};
    }

    const {IntersectionObserver} = window;

    delete window.IntersectionObserver;

    window.IntersectionObserver = function () {
        this.root = null;
        this.rootMargin = '';
        this.thresholds = [];
        this.disconnect = jest.fn(() => null);
        this.observe = jest.fn(() => null);
        this.takeRecords = jest.fn(() => null);
        this.unobserve = jest.fn(() => null);
    };

    return () => {
        window.IntersectionObserver = IntersectionObserver;
    };
};
