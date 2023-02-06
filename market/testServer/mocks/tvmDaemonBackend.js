import {MockBackend} from './base/mockBackend';

jest.mock('@yandex-market/mandrel/bcm/tvm/TvmDaemonBackend', () => ({
    TvmDaemonBackend: MockBackend,
}));
