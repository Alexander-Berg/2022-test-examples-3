'use strict';

const { default: serviceConfig, clearCache } = require('./service-config');
const { defaultConfig } = require('../helpers/flags');

const check = (core) => serviceConfig.call(core, 'test');

beforeEach(() => clearCache());

test('no config, no service#defaultConfig', () => {
    const res = check({ services: {} });
    expect(res).toEqual({
        defaultOptions: {},
        methods: {}
    });
});

test('no config, service#defaultConfig', () => {
    const core = {
        services: {
            test: {
                [defaultConfig]() {
                    return {
                        url: 'url',
                        useServiceTvm: true,
                        methods: {
                            a: { a: 1 }
                        }
                    };
                }
            }
        }
    };
    const res = check(core);
    expect(res).toEqual({
        url: 'url',
        useServiceTvm: true,
        defaultOptions: {},
        methods: {
            a: { a: 1 }
        }
    });
});

test('config, no service#defaultConfig', () => {
    const core = {
        services: {},
        config: {
            services: {
                test: {
                    url: 'url',
                    useUserTvm: true
                }
            }
        }
    };
    const res = check(core);
    expect(res).toEqual({
        url: 'url',
        useUserTvm: true,
        defaultOptions: {},
        methods: {}
    });
});

test('config with methods', () => {
    const core = {
        services: {
            test: {
                [defaultConfig]() {
                    return {
                        url: 'def',
                        useServiceTvm: true,
                        methods: {
                            a: { a: 1 },
                        }
                    };
                }
            }
        },
        config: {
            services: {
                test: {
                    url: 'conf',
                    useServiceTvm: false,
                    methods: {
                        b: { x: 100 },
                    }
                }
            }
        }
    };
    const res = check(core);
    expect(res).toEqual({
        url: 'conf',
        useServiceTvm: false,
        defaultOptions: {},
        methods: {
            b: { x: 100 }
        }
    });
});

test('merge defaultOptions', () => {
    const core = {
        services: {
            test: {
                [defaultConfig]() {
                    return {
                        url: 'def',
                        useServiceTvm: true,
                        defaultOptions: { a: 1, b: 2 }
                    };
                }
            }
        },
        config: {
            services: {
                test: {
                    url: 'conf',
                    useServiceTvm: false,
                    methods: {
                        a: { a: 100 },
                        b: { b: 100 },
                        c: { c: 100 }
                    }
                }
            }
        }
    };
    const res = check(core);
    expect(res).toEqual({
        url: 'conf',
        useServiceTvm: false,
        defaultOptions: { a: 1, b: 2 },
        methods: {
            a: { a: 100, b: 2 },
            b: { a: 1, b: 100 },
            c: { a: 1, b: 2, c: 100 }
        }
    });
});

test('merge defaultOptions 2', () => {
    const core = {
        services: {
            test: {
                [defaultConfig]() {
                    return {
                        url: 'def',
                        useServiceTvm: true,
                        defaultOptions: { a: 1, b: 2 }
                    };
                }
            }
        },
        config: {
            services: {
                test: {
                    url: 'conf',
                    useServiceTvm: false,
                    defaultOptions: { b: 1, c: 3 },
                    methods: {
                        a: { a: 100 },
                        b: { b: 100 },
                        c: { c: 100 }
                    }
                }
            }
        }
    };
    const res = check(core);
    expect(res).toEqual({
        url: 'conf',
        useServiceTvm: false,
        defaultOptions: { a: 1, b: 1, c: 3 },
        methods: {
            a: { a: 100, b: 1, c: 3 },
            b: { a: 1, b: 100, c: 3 },
            c: { a: 1, b: 1, c: 100 }
        }
    });
});
