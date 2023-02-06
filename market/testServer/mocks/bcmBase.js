import {MockBackend} from './base/mockBackend';
import {MockClient} from './base/mockClient';

jest.mock('@yandex-market/mandrel/bcm/base', () => ({
    MarketHttpBackend: MockBackend,
    MarketClient: MockClient,
}));

jest.mock('@yandex-market/mandrel/bcm/base/MarketHttpBackend', () => ({
    MarketHttpBackend: MockBackend,
}));
