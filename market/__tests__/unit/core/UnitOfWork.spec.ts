import UnitOfWork from '../../../src/infrastructure/base-implementations/UnitOfWork';
import AggregateRoot from '../../../src/core/base-implementations/AggregateRoot';
import ServiceProvider from '../../../src/core/base-implementations/ServiceProvider';
import Service from '../../../src/core/Service';
import Specification from '../../../src/core/Specification';
import Class from '../../../src/core/Class';
import Id from '../../../src/core/Id';
import RepositoryProvider from '../../../src/core/base-implementations/RepositoryProvider';
import Repository from '../../../src/core/Repository';
import RepositoryDecorator from '../../../src/core/base-implementations/RepositoryDecorator';
import Session from '../../../src/infrastructure/base-implementations/Session';
import NotImplementedException from '../../../src/utils/errors/NotImplementedException';

describe('unit of work', () => {
    class StubId implements Id {
        public value: string;

        public constructor() {
            this.value = Math.random().toString();
        }

        public toString(): string {
            return this.value;
        }

        public equals(id: Id): boolean {
            return this.value === id.value;
        }
    }

    class Stub implements AggregateRoot<StubId> {
        public id: StubId;

        public constructor() {
            this.id = new StubId();
        }
    }

    class StubSpecification implements Specification<StubId, Stub> {
        public limit = 50;

        // eslint-disable-next-line no-unused-vars,class-methods-use-this
        public isSatisfiedBy(_stub: Stub): boolean {
            return true;
        }
    }

    class StubRepository implements Repository<StubId, Stub, StubSpecification> {
        private readonly identityMap: Map<StubId, Stub>;

        public constructor() {
            this.identityMap = new Map();
        }

        public static create(): Promise<Repository<StubId, Stub, StubSpecification>> {
            return Promise.resolve(new StubRepository());
        }

        public static getObjectClass(): Class<Stub> {
            return Stub;
        }

        // eslint-disable-next-line no-unused-vars,class-methods-use-this
        public nextIdentity(): Promise<StubId> {
            throw new NotImplementedException();
        }

        public add(...stubs: Stub[]): Promise<StubId[]> {
            const ids = stubs.map(stub => {
                const { id } = stub;

                this.identityMap.set(id, stub);

                return id;
            });

            return Promise.resolve(ids);
        }

        public find(...ids: StubId[]): Promise<(Stub | void)[]> {
            const stubs = ids.map(id => this.identityMap.get(id) || undefined);

            return Promise.resolve(stubs);
        }

        // eslint-disable-next-line no-unused-vars,class-methods-use-this
        public search(_spec: StubSpecification): Promise<Stub[]> {
            throw new NotImplementedException();
        }

        public remove(...ids: StubId[]): Promise<StubId[]> {
            ids.forEach(id => this.identityMap.delete(id));

            return Promise.resolve(ids);
        }

        // eslint-disable-next-line class-methods-use-this
        public async revertAdd(..._entities: Stub[]): Promise<Stub['id'][]> {
            return [];
        }

        // eslint-disable-next-line class-methods-use-this
        public async revertRemove(..._entities: Stub[]): Promise<Stub['id'][]> {
            return [];
        }
    }

    interface IStubService extends Service {}

    class StubService implements IStubService {
        public static create(): IStubService {
            return new StubService();
        }
    }

    describe('get required repository', () => {
        test('should throw an error if required repository was not found', async () => {
            const serviceProvider = new ServiceProvider();
            const repositoryProvider = new RepositoryProvider();

            const unitOfWork = await UnitOfWork.create(serviceProvider, repositoryProvider, await Session.create());

            await expect(unitOfWork.repository(Stub)).rejects.toThrow();
        });

        test('should return required instance of repository decorator instead of repository', async () => {
            const serviceProvider = new ServiceProvider();

            // prettier-ignore
            const repositoryProvider = new RepositoryProvider()
                .addRepository(StubRepository);

            const unitOfWork = await UnitOfWork.create(serviceProvider, repositoryProvider, await Session.create());

            const actualRepository = await unitOfWork.repository(Stub);

            expect(actualRepository).toBeInstanceOf(RepositoryDecorator);
        });
    });

    describe('get required service', () => {
        test('should throw an error if required service was not found', async () => {
            const serviceProvider = new ServiceProvider();
            const repositoryProvider = new RepositoryProvider();

            const unitOfWork = await UnitOfWork.create(serviceProvider, repositoryProvider, await Session.create());

            expect(() => unitOfWork.service(StubService)).toBeTruthy();
        });

        test('should return correct instance of  required service', async () => {
            // prettier-ignore
            const serviceProvider = new ServiceProvider()
                .addService(StubService);

            const repositoryProvider = new RepositoryProvider();

            const unitOfWork = await UnitOfWork.create(serviceProvider, repositoryProvider, await Session.create());

            const actualService = await unitOfWork.service(StubService);

            expect(actualService).toBeInstanceOf(StubService);
        });
    });

    describe('identity map', () => {
        test.skip('add entity', async () => {
            const serviceProvider = new ServiceProvider();

            // prettier-ignore
            const repositoryProvider = new RepositoryProvider()
                .addRepository(StubRepository);

            const unitOfWork = await UnitOfWork.create(serviceProvider, repositoryProvider, await Session.create());

            const stubRepository = await unitOfWork.repository(Stub);

            const spyOnAdd = jest.spyOn(stubRepository, 'add');

            const stub = new Stub();

            await stubRepository.add(stub, stub);

            expect(spyOnAdd).not.toHaveBeenCalled();

            await unitOfWork.commit();

            expect(spyOnAdd).toHaveBeenCalled();

            spyOnAdd.mockRestore();
        });

        test.skip('find entity', async () => {
            const serviceProvider = new ServiceProvider();

            // prettier-ignore
            const repositoryProvider = new RepositoryProvider()
                .addRepository(StubRepository);

            const unitOfWork = await UnitOfWork.create(serviceProvider, repositoryProvider, await Session.create());

            const stubRepository = await unitOfWork.repository(Stub);

            const spyOnFind = jest.spyOn(stubRepository, 'find');

            const stub = new Stub();
            const { id: stubId } = stub;

            await stubRepository.add(stub);
            await stubRepository.find(stubId);

            expect(spyOnFind).not.toHaveBeenCalled();

            spyOnFind.mockRestore();
        });

        test.skip('remove entity', async () => {
            const serviceProvider = new ServiceProvider();

            // prettier-ignore
            const repositoryProvider = new RepositoryProvider()
                .addRepository(StubRepository);

            const unitOfWork = await UnitOfWork.create(serviceProvider, repositoryProvider, await Session.create());

            const stubRepository = await unitOfWork.repository(Stub);

            const spyOnRemove = jest.spyOn(stubRepository, 'remove');

            const stub = new Stub();
            const { id: stubId } = stub;

            await stubRepository.add(stub);
            await stubRepository.remove(stubId);

            expect(spyOnRemove).not.toHaveBeenCalled();

            await unitOfWork.commit();

            expect(spyOnRemove).toHaveBeenCalled();

            spyOnRemove.mockRestore();
        });
    });
});
