import ServiceProvider from '../../../src/core/base-implementations/ServiceProvider';
import Service from '../../../src/core/Service';


describe('service provider', () => {
    interface IStubService extends Service {}

    class StubService implements IStubService {
        public static create(): IStubService {
            return new StubService();
        }
    }

    class StubImplService extends StubService {
        public static create(): IStubService {
            return new StubImplService();
        }
    }

    test('should throw an error if repository was not found', () => {
        const serviceProvider = new ServiceProvider();

        expect(() => serviceProvider.service(StubService)).toThrow();
    });

    test('should return correct service (1)', async () => {
        const serviceProvider = new ServiceProvider().addService(StubService);

        const actualService = await serviceProvider.service(StubService);

        expect(actualService).toBeInstanceOf(StubService);
    });

    test('should return correct service (2)', async () => {
        const serviceProvider = new ServiceProvider().addService(StubImplService);

        const actualService = await serviceProvider.service(StubService);

        expect(actualService).toBeInstanceOf(StubService);
        expect(actualService).toBeInstanceOf(StubImplService);
    });
});
