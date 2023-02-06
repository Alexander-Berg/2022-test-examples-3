import stout from '@yandex-market/stout';

let config = stout.get('config');

if (!config) {
    config = {};
    stout.set('config', config);
}

jest.mock('heapdump', () => ({}), {virtual: true});
