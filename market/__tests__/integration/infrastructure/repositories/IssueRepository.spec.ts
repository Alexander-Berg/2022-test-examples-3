/* eslint-disable max-len */

import Issue from '../../../../src/domain/models/issue/Issue';
import IssueId from '../../../../src/domain/models/issue/IssueId';

import IssueRepository from '../../../../src/infrastructure/repositories/IssueRepository';
import IssueSpecification from '../../../../src/domain/specifications/IssueSpecification';
import AppConfig from '../../../../src/AppConfig';

describe('issue tracker repository', () => {
    let issue: Issue;

    // Creates new issue that will be used in further tests
    beforeAll(async done => {
        await AppConfig.init();
        try {
            const issueRepository = await IssueRepository.create();
            const [issueId, url] = await issueRepository.nextIdentities();
            const key = url.split('/')[url.split('/').length - 1];

            issue = new Issue({
                self: `https://st-api.test.yandex-team.ru/v2/issues/${key}`,
                url,
                id: issueId,
                summary: 'This issue is created for testing purpose',
                description: 'This issue is created for testing purpose.',
            });
        } catch (err) {
            done.fail(err);
        }

        done();
    }, 10000);


    test('should return correct object class', async () => {
        const objectClass = IssueRepository.getObjectClass();

        expect(objectClass).toBe(Issue);
    });

    test('should return new unique identity', async () => {
        const issueRepository = await IssueRepository.create();
        const issueId = await issueRepository.nextIdentity();

        expect(issueId).toBeInstanceOf(IssueId);
    }, 10000);

    describe('add and then find the issue in the repository', () => {
        test('should add the issue to the repository', async () => {
            const { id: expectedIssueId } = issue;

            const issueRepository = await IssueRepository.create();
            const [actualIssueId] = await issueRepository.add(issue);

            expect(actualIssueId.externalId).toEqual(expectedIssueId.externalId);
        }, 10000);

        test('should find issue by the identity in the repository', async () => {
            const { id: issueId } = issue;

            const issueRepository = await IssueRepository.create();
            const [actualIssue] = await issueRepository.find(issueId);

            expect(actualIssue).toBeDefined();

            if (!actualIssue) {
                return;
            }

            expect(actualIssue.url).toEqual(issue.url);
            expect(actualIssue.self).toEqual(issue.self);
            expect(actualIssue.id.externalId).toEqual(issue.id.externalId)
        }, 10000);

        describe('search by the specification', () => {
            test('should find issue by the specification which defines the id of the issue', async () => {
                const { id: issueId } = issue;

                const issueSpec = new IssueSpecification().withId(issueId);

                const issueRepository = await IssueRepository.create();
                const [actualIssue] = await issueRepository.search(issueSpec);

                expect(actualIssue).toBeDefined();

                if (!actualIssue) {
                    return;
                }

                expect(actualIssue.url).toEqual(issue.url);
                expect(actualIssue.self).toEqual(issue.self);
                expect(actualIssue.id.externalId).toEqual(issue.id.externalId)
            }, 10000);

            test.skip('should find issue by the specification which defines url, assignee, summary and description of the issue', async () => {
                const { url, assignee: { staff }, summary, description } = issue;

                const issueSpec = new IssueSpecification()
                    .withUrl(url)
                    .withAssignee(staff)
                    .withSummary(summary)
                    .withDescription(description);

                const issueRepository = await IssueRepository.create();
                const [actualIssue] = await issueRepository.search(issueSpec);

                expect(actualIssue).toEqual(issue);
            }, 10000);
        });
    });

    afterAll(async done => {
        try {
            const { id: issueId } = issue;

            const issueRepository = await IssueRepository.create();
            await issueRepository.remove(issueId);
        } catch (err) {
            done.fail(err);
        }

        done();
    }, 10000);
});
