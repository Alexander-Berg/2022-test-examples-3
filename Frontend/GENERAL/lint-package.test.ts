import { lintPackage } from './lint-package';
import { LintStatus, DepStatus, TRestrictions } from './types';

jest.mock('@yandex-int/frontend.ci.utils', () => ({
    execLerna: jest.fn()
        .mockReturnValueOnce('[{"name":"p4","version":"2.0.0","private":false,"location":""}]')
        .mockReturnValueOnce('[{"name":"p4","version":"2.0.0","private":false,"location":""}]'),
}));

const restrictions: TRestrictions = {
    p1: {
        actualVersion: '1.1.0',
        outdatedVersions: [],
        restricted: false,
    },
    p2: {
        actualVersion: '2.0.0',
        outdatedVersions: ['1.6.4', '1.6.5'],
        restricted: false,
    },
    p3: {
        actualVersion: '3.0.0',
        outdatedVersions: ['2.9.8', '3.0.0', '2.9.9'],
        restricted: false,
    },
    p4: {
        actualVersion: 'upstream',
        outdatedVersions: [],
        restricted: false,
    },
    p5: {
        actualVersion: 'upstream',
        outdatedVersions: [],
        restricted: false,
    },
    forbiddenDep: {
        actualVersion: '1.0.0',
        outdatedVersions: [],
        recommendations: ['recommendedDep'],
        restricted: true,
    },
};

describe('Lint package', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should notify OK on approved version', () => {
        expect(
            lintPackage({ p1: { version: '1.1.0', status: DepStatus.Changed } }, restrictions)[0],
        ).toStrictEqual({
            name: 'p1',
            status: LintStatus.Ok,
            current: '1.1.0',
            target: '1.1.0',
            recommendations: undefined,
        });
    });

    it('should warn on legacy (allowed) version', () => {
        expect(
            lintPackage({ p2: { version: '1.6.5', status: DepStatus.Changed } }, restrictions)[0],
        ).toStrictEqual({
            name: 'p2',
            status: LintStatus.OutdatedVersion,
            current: '1.6.5',
            target: '2.0.0',
            recommendations: undefined,
        });

        expect(
            lintPackage({ p2: { version: '1.6.4', status: DepStatus.Changed } }, restrictions)[0],
        ).toStrictEqual({
            name: 'p2',
            status: LintStatus.OutdatedVersion,
            current: '1.6.4',
            target: '2.0.0',
            recommendations: undefined,
        });
    });

    it('should swear on invalid version', () => {
        expect(
            lintPackage({ p1: { version: '1.1.1', status: DepStatus.Changed } }, restrictions)[0],
        ).toStrictEqual({
            name: 'p1',
            status: LintStatus.RestrictedVersion,
            current: '1.1.1',
            target: '1.1.0',
            recommendations: undefined,
        });

        expect(
            lintPackage({ p2: { version: '1.6.6', status: DepStatus.Changed } }, restrictions)[0],
        ).toStrictEqual({
            name: 'p2',
            status: LintStatus.RestrictedVersion,
            current: '1.6.6',
            target: '2.0.0',
            recommendations: undefined,
        });
    });

    it('should prefer approved on ambiguous config', () => {
        expect(
            lintPackage({ p3: { version: '3.0.0', status: DepStatus.Changed } }, restrictions)[0],
        ).toStrictEqual({
            name: 'p3',
            status: LintStatus.Ok,
            current: '3.0.0',
            target: '3.0.0',
            recommendations: undefined,
        });
    });

    it('should notify OK on valid upstream version', () => {
        expect(
            lintPackage({ p4: { version: '2.0.0', status: DepStatus.Changed } }, restrictions)[0],
        ).toStrictEqual({
            name: 'p4',
            status: LintStatus.Ok,
            current: '2.0.0',
            target: '2.0.0',
            recommendations: undefined,
        });
    });

    it('should swear on invalid upstream version', () => {
        expect(
            lintPackage({ p4: { version: '1.9.0', status: DepStatus.Changed } }, restrictions)[0],
        ).toStrictEqual({
            name: 'p4',
            status: LintStatus.RestrictedVersion,
            current: '1.9.0',
            target: '2.0.0',
            recommendations: undefined,
        });
    });

    it('should swear on added new forbidden dependency', () => {
        expect(
            lintPackage({ forbiddenDep: { version: '1.0.0', status: DepStatus.New } }, restrictions)[0],
        )
            .toStrictEqual({
                name: 'forbiddenDep',
                status: LintStatus.RestrictedDep,
                current: '1.0.0',
                target: '1.0.0',
                recommendations: ['recommendedDep'],
            });
    });

    it('should notify OK on changed vestion of forbidden dependency', () => {
        expect(
            lintPackage({ forbiddenDep: { version: '1.0.0', status: DepStatus.Changed } }, restrictions)[0],
        ).toStrictEqual({
            name: 'forbiddenDep',
            status: LintStatus.Ok,
            current: '1.0.0',
            target: '1.0.0',
            recommendations: ['recommendedDep'],
        });
    });
});
