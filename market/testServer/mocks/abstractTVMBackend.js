import {MockBackend} from './base/mockBackend';

jest.mock('@yandex-market/mandrel/bcm/abstract/AbstractTVMBackend', () => ({
    AbstractTVMBackend: MockBackend,
}));
