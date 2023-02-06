/* eslint-disable max-len */

import SelectormanSpecification from '../../../../src/domain/specifications/SelectormanSpecification';
import PgSelectormanGateway from '../../../../src/infrastructure/gateways/PgSelectormanGateway';
import SelectormanId from '../../../../src/domain/models/selectorman/SelectormanId';
import AppConfig from '../../../../src/AppConfig';

describe.skip('selectorman gateway', () => {
    beforeAll(async () => {
        await AppConfig.init();
    });

    test('should find selectorman by the identity', async () => {
        const selectormanId = SelectormanId.fromString(AppConfig.TEST_SELECTORMAN_ID);

        const [selectorman] = await (await PgSelectormanGateway.create()).find(selectormanId);

        expect(selectorman).toMatchObject({
            id: selectormanId,
            staff: 'robot-sovetnik',
        });
    });

    describe('search by specification', () => {
        test('should find selectorman by the specification which defines the identity of the selectorman', async () => {
            const selectormanId = SelectormanId.fromString(AppConfig.TEST_SELECTORMAN_ID);

            const selectormanSpec = new SelectormanSpecification().withId(selectormanId);

            const [actualSelectorman] = await (await PgSelectormanGateway.create()).search(selectormanSpec);

            expect(actualSelectorman).toMatchObject({
                id: selectormanId,
                staff: 'robot-sovetnik',
            });
        });
    });
});
