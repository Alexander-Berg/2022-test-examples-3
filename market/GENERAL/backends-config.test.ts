import {backendsConfig} from './backends-config';

test('store and read', function () {
    const aaaConfig = {host: 'aaa'};
    backendsConfig.setup({a: aaaConfig});

    expect(backendsConfig.getBackend('a')).toBe(aaaConfig);
});

test('extend', function () {
    const aaaConfig = {host: 'aaa'};
    const bbbConfig = {host: 'bbb'};

    backendsConfig.setup({a: aaaConfig});
    backendsConfig.setup({b: bbbConfig});

    expect(backendsConfig.getBackend('a')).toBe(aaaConfig);
    expect(backendsConfig.getBackend('b')).toBe(bbbConfig);
});
