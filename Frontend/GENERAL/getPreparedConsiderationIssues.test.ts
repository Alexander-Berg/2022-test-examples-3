import { CONSIDERATION_ISSUE_LEVEL } from '~/enums/ConsiderationIssueLevel';

import { IConsiderationIssue } from '~/interfaces/IConsideration';

import { getPreparedConsiderationIssues } from '.';

describe('CandidateCard', function() {
    describe('getPreparedConsiderationIssues', function() {
        it('should sort and group consideration issues', function() {
            const source: IConsiderationIssue[] = [
                {
                    id: 1,
                    type: 'type1',
                    level: CONSIDERATION_ISSUE_LEVEL.INFO,
                    params: {},
                },
                {
                    id: 2,
                    type: 'type1',
                    level: CONSIDERATION_ISSUE_LEVEL.DANGER,
                    params: {},
                },
                {
                    id: 3,
                    type: 'type1',
                    level: CONSIDERATION_ISSUE_LEVEL.WARNING,
                    params: {},
                },
                {
                    id: 4,
                    type: 'type2',
                    level: CONSIDERATION_ISSUE_LEVEL.DANGER,
                    params: {},
                },
                {
                    id: 5,
                    type: 'type3',
                    level: CONSIDERATION_ISSUE_LEVEL.WARNING,
                    params: {},
                },
            ];

            const result = {
                type1: {
                    id: 2,
                    type: 'type1',
                    level: CONSIDERATION_ISSUE_LEVEL.DANGER,
                    params: {},
                },
                type2: {
                    id: 4,
                    type: 'type2',
                    level: CONSIDERATION_ISSUE_LEVEL.DANGER,
                    params: {},
                },
                type3: {
                    id: 5,
                    type: 'type3',
                    level: CONSIDERATION_ISSUE_LEVEL.WARNING,
                    params: {},
                },
            };

            expect(getPreparedConsiderationIssues(source)).toEqual(result);
        });
    });
});
