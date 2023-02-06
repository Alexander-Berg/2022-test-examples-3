import GetRules from './get-rules';

describe('get_rules_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new GetRules();

        await action({ orgId: 100500 }, { service: () => service } as any);
        expect(service).toHaveBeenCalledWith('/v1/domain/rules/get', {}, {
            orgId: 100500,
        });
    });
});
