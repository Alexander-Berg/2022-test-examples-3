import Selectorman from '../../../../src/domain/models/selectorman/Selectorman';
import SelectormanId from '../../../../src/domain/models/selectorman/SelectormanId';

import SelectormanRepository from '../../../../src/infrastructure/repositories/SelectormanRepository';
import SelectormanSpecification from '../../../../src/domain/specifications/SelectormanSpecification';
import AppConfig from '../../../../src/AppConfig';

describe('selectorman repository', () => {
    beforeAll(async () => {
        await AppConfig.init();
    });

    test('should return correct object class', async () => {
        const objectClass = SelectormanRepository.getObjectClass();

        expect(objectClass).toBe(Selectorman);
    });

    test('should find selectorman by the identity', async () => {
        const selectormanId = new SelectormanId(AppConfig.TEST_SELECTORMAN_ID);

        const selectormanRepository = await SelectormanRepository.create();
        const [selectorman] = await selectormanRepository.find(selectormanId);

        expect(selectorman).toMatchObject({
            id: selectormanId,
            staff: AppConfig.TEST_SELECTORMAN_STAFF,
        });
    });

    describe('search by specification', () => {
        test('should find selectorman by the specification which defines the identity of the selectorman', async () => {
            const selectormanSpec = new SelectormanSpecification().withStaff(AppConfig.TEST_SELECTORMAN_STAFF);

            const selectormanRepository = await SelectormanRepository.create();
            const [actualSelectorman] = await selectormanRepository.search(selectormanSpec);

            expect(actualSelectorman).toMatchObject({
                staff: AppConfig.TEST_SELECTORMAN_STAFF,
            });
        });
    });
});
