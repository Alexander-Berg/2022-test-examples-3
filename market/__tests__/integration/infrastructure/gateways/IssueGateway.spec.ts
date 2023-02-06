/* eslint-disable max-len */


import IssueGateway from '../../../../src/infrastructure/gateways/IssueGateway';
import Issue from '../../../../src/domain/models/issue/Issue';
import IssueSpecification from '../../../../src/domain/specifications/IssueSpecification';
import Selectorman from '../../../../src/domain/models/selectorman/Selectorman';
import IssueId from '../../../../src/domain/models/issue/IssueId';
import DomainInfoId from '../../../../src/domain/models/domain-info/DomainInfoId';

describe('issue gateways', () => {
    let issue: Issue;

    // Creates new issue that will be used in further tests
    beforeAll(async done => {
        // await AppConfig.init();
        try {
            const issueGateway = new IssueGateway();

            const [issueId, url] = await issueGateway.url();

            const key = url.split('/')[url.split('/').length - 1];

            issue = new Issue({
                id: new IssueId(issueId.externalId, issueId.externalId),
                assignee: Selectorman.default(DomainInfoId.defaultStaff),
                followers: ['robot-sovetnik'],
                summary: 'This issue is created for testing purpose',
                description: 'This issue is created for testing purpose.',
                numberOfFixed: 10,
                externalId: issueId.externalId,
                self: `https://st-api.test.yandex-team.ru/v2/issues/${key}`,
                url,
            });

            await issueGateway.insert(issue);
        } catch (err) {
            done.fail(err);
        }

        done();
    }, 10000);

    afterAll(async done => {
        try {
            const { id: issueId } = issue;

            await new IssueGateway().delete(issueId);
        } catch (err) {
            done.fail(err);
        }

        done();
    }, 10000);

    describe('search by specification', () => {

        test('should find issue by the identity', async () => {
            const { id: issueId } = issue;

            const [actualIssue] = await new IssueGateway().find(issueId);
            expect(actualIssue).toBeDefined();

            if (!actualIssue) {
                return;
            }

            expect(actualIssue.url).toEqual(issue.url);
            expect(actualIssue.self).toEqual(issue.self);
            expect(actualIssue.id).toEqual(issueId);
        });

        test('should find issue by the specification which defines the id of the issue', async () => {
            const { id: issueId } = issue;

            const issueSpec = new IssueSpecification().withId(issueId);

            const [actualIssue] = await new IssueGateway().search(issueSpec);

            expect(actualIssue).toBeDefined();

            if (!actualIssue) {
                return;
            }

            expect(actualIssue.url).toEqual(issue.url);
            expect(actualIssue.self).toEqual(issue.self);
            expect(actualIssue.id).toEqual(issueId);
        }, 10000);

        test.skip('should find issue by the specification which defines url, assignee, summary and description of the issue', async () => {
            const { url, assignee: { staff }, summary, description } = issue;

            const issueSpec = new IssueSpecification()
                .withUrl(url)
                .withAssignee(staff)
                .withSummary(summary)
                .withDescription(description);

            const [actualIssue] = await new IssueGateway().search(issueSpec);

            expect(actualIssue).toEqual(issue);
        }, 10000);
    });
});
