import type { ChangedPackagesNormalizedAttributes } from './typings/attributes';
import { buildAttributes, normalizeAttributes } from '.';

describe('buildAttributes', () => {
    it('should prepare all fields', () => {
        const params: ChangedPackagesNormalizedAttributes = {
            ttl: 14,

            repositoryName: 'frontend',
            repositoryPath: 'search-interfaces/frontend',
            headRef: 'review/1980271',
            headCommit: 'b3070357bcc6ba48e352105d15780416f9e7c366',
            baseCommit: '6162c8c32645c58deb5938d8bd08491eae8ae77d',
            branch: 'users/ifutinma/FORMS-7656',
            prNumber: 1980271,

            changedPackages: ['@yandex-int/ydo', '@yandex-int/health'],
        };

        const attrs = buildAttributes(params);

        expect(attrs).toEqual({
            type: 'changed-packages',
            ttl: '14',

            repo: 'frontend',
            repo_full_name: 'search-interfaces/frontend',
            head_ref: 'review/1980271',
            head_commit: 'b3070357bcc6ba48e352105d15780416f9e7c366',
            base_commit: '6162c8c32645c58deb5938d8bd08491eae8ae77d',
            branch: 'users/ifutinma/FORMS-7656',
            pr_number: '1980271',

            changed_packages: '@yandex-int/ydo,@yandex-int/health',
        });
    });

    it('should not fail if branch or pr_number is empty', () => {
        const params: ChangedPackagesNormalizedAttributes = {
            repositoryName: 'frontend',
            repositoryPath: 'search-interfaces/frontend',
            headRef: 'review/1980271',
            headCommit: 'b3070357bcc6ba48e352105d15780416f9e7c366',
            baseCommit: '6162c8c32645c58deb5938d8bd08491eae8ae77d',

            changedPackages: ['@yandex-int/ydo', '@yandex-int/health'],
        };

        const attrs = buildAttributes(params);

        expect(attrs).not.toHaveProperty('branch');
        expect(attrs).not.toHaveProperty('pr_number');
    });

    it('should set default ttl', () => {
        const params: ChangedPackagesNormalizedAttributes = {
            repositoryName: 'frontend',
            repositoryPath: 'search-interfaces/frontend',
            headRef: 'review/1980271',
            headCommit: 'b3070357bcc6ba48e352105d15780416f9e7c366',
            baseCommit: '6162c8c32645c58deb5938d8bd08491eae8ae77d',
            branch: 'users/ifutinma/FORMS-7656',
            prNumber: 1980271,

            changedPackages: ['@yandex-int/ydo', '@yandex-int/health'],
        };

        const attrs = buildAttributes(params);

        expect(attrs.ttl).toBe('30');
    });

    it('should work with one changed package', () => {
        const params: ChangedPackagesNormalizedAttributes = {
            repositoryName: 'frontend',
            repositoryPath: 'search-interfaces/frontend',
            headRef: 'review/1980271',
            headCommit: 'b3070357bcc6ba48e352105d15780416f9e7c366',
            baseCommit: '6162c8c32645c58deb5938d8bd08491eae8ae77d',
            changedPackages: ['@yandex-int/ydo'],
        };

        const attrs = buildAttributes(params);

        expect(attrs.changed_packages).toEqual('@yandex-int/ydo');
    });

    it('should work with no changed packages', () => {
        const params: ChangedPackagesNormalizedAttributes = {
            repositoryName: 'frontend',
            repositoryPath: 'search-interfaces/frontend',
            headRef: 'review/1980271',
            headCommit: 'b3070357bcc6ba48e352105d15780416f9e7c366',
            baseCommit: '6162c8c32645c58deb5938d8bd08491eae8ae77d',
            changedPackages: [],
        };

        const attrs = buildAttributes(params);

        expect(attrs.changed_packages).toEqual('');
    });
});

describe('normalizeAttributes', () => {
    const checkNormalization = (params: ChangedPackagesNormalizedAttributes) => {
        const actual = normalizeAttributes(buildAttributes(params));

        expect(actual).toMatchObject(params);
        expect(actual).toHaveProperty('ttl');
    };

    it('should parse all generated attributes', () => {
        checkNormalization({
            ttl: 14,

            repositoryName: 'frontend',
            repositoryPath: 'search-interfaces/frontend',
            headRef: 'review/1980271',
            headCommit: 'b3070357bcc6ba48e352105d15780416f9e7c366',
            baseCommit: '6162c8c32645c58deb5938d8bd08491eae8ae77d',
            branch: 'users/ifutinma/FORMS-7656',
            prNumber: 1980271,

            changedPackages: ['@yandex-int/ydo', '@yandex-int/health'],
        });
    });

    it('should work with one changed package', () => {
        checkNormalization({
            repositoryName: 'frontend',
            repositoryPath: 'search-interfaces/frontend',
            headRef: 'review/1980271',
            headCommit: 'b3070357bcc6ba48e352105d15780416f9e7c366',
            baseCommit: '6162c8c32645c58deb5938d8bd08491eae8ae77d',

            changedPackages: ['@yandex-int/ydo'],
        });
    });

    it('should work with no changed packages', () => {
        checkNormalization({
            repositoryName: 'frontend',
            repositoryPath: 'search-interfaces/frontend',
            headRef: 'review/1980271',
            headCommit: 'b3070357bcc6ba48e352105d15780416f9e7c366',
            baseCommit: '6162c8c32645c58deb5938d8bd08491eae8ae77d',

            changedPackages: [],
        });
    });
});
