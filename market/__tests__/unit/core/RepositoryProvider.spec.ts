/* eslint-disable class-methods-use-this,  */

import AggregateRoot from '../../../src/core/base-implementations/AggregateRoot';
import Specification from '../../../src/core/Specification';
import Class from '../../../src/core/Class';
import Id from '../../../src/core/Id';
import RepositoryProvider from '../../../src/core/base-implementations/RepositoryProvider';
import Repository from '../../../src/core/Repository';
import NotImplementedException from '../../../src/utils/errors/NotImplementedException';

describe('repository provider', () => {
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

        public isSatisfiedBy(_stub: Stub): boolean {
            return true;
        }
    }

    class StubRepository implements Repository<StubId, Stub, StubSpecification> {
        public static create() {
            return new StubRepository();
        }

        public static getObjectClass(): Class<Stub> {
            return Stub;
        }

        public nextIdentity(): Promise<StubId> {
            throw new NotImplementedException();
        }

        public add(..._stubs: Stub[]): Promise<StubId[]> {
            throw new NotImplementedException();
        }

        public find(..._ids: StubId[]): Promise<(Stub | void)[]> {
            throw new NotImplementedException();
        }

        public search(_spec: StubSpecification): Promise<Stub[]> {
            throw new NotImplementedException();
        }

        public remove(..._ids: StubId[]): Promise<StubId[]> {
            throw new NotImplementedException();
        }

        public async revertAdd(..._entities: Stub[]): Promise<Stub['id'][]> {
            return [];
        }

        public async revertRemove(..._entities: Stub[]): Promise<Stub['id'][]> {
            return [];
        }
    }

    test('should throw an error if repository was not found', () => {
        const repositoryProvider = new RepositoryProvider();

        expect(() => repositoryProvider.repository(Stub)).toThrow();
    });

    test('should return correct repository (1)', () => {
        const repositoryProvider = new RepositoryProvider().addRepository(StubRepository);

        const actualRepository = repositoryProvider.repository(Stub);

        expect(actualRepository).toBeInstanceOf(StubRepository);
    });

    test('should return correct repository (2)', () => {
        const repositoryProvider = new RepositoryProvider().addRepository(StubRepository);

        const actualRepository = repositoryProvider.repository(Stub);

        expect(actualRepository).toBeInstanceOf(StubRepository);
    });
});
