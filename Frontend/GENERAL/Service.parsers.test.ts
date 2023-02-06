import { prepareOebsFlags, parseOebsFields } from '~/src/features/Service/redux/Service.parsers';
import { getOebsAgreementMock, getRawOebsAgreementMock } from '~/test/jest/mocks/data/oebs-agreement';

const oebsFlags = {
    useForHr: true,
    useForProcurement: false,
    useForRevenue: true,
    useForHardware: true,
    useForGroup: true,
};

const rawOebsFlags = {
    use_for_hr: true,
    use_for_procurement: false,
    use_for_revenue: true,
    use_for_hardware: true,
    use_for_group_only: true,
};

describe('Service parsers', () => {
    it('should convert fields to backend format in prepareOebsFlags', () => {
        expect(prepareOebsFlags(oebsFlags)).toStrictEqual(rawOebsFlags);
    });

    it('should omit flags with non-boolean values', () => {
        const actual = prepareOebsFlags({
            useForHr: true,
            useForHardware: false,
            // @ts-expect-error
            useForGroup: null,
            // @ts-expect-error
            useForProcurement: 0,
            // @ts-expect-error
            useForRevenue: 1,
        });

        expect(actual.use_for_hr).toBe(true);
        expect(actual.use_for_hardware).toBe(false);
        expect(Object.keys(actual)).not.toContain('use_for_group_only');
        expect(Object.keys(actual)).not.toContain('use_for_procurement');
        expect(Object.keys(actual)).not.toContain('use_for_revenue');
    });

    it('should convert fields from backend format in parseOebsFields', () => {
        expect(parseOebsFields({
            ...rawOebsFlags,
            oebs_agreement: getRawOebsAgreementMock(1),
        })).toStrictEqual({
            ...oebsFlags,
            oebsAgreement: getOebsAgreementMock(1),
        });
    });
});
