import { Dictionary } from 'lodash';

import { INTERVIEW_RESOLUTION } from '~/enums/InterviewResolution';
import { INTERVIEW_STATUS } from '~/enums/InterviewStatus';
import { INTERVIEW_TYPE } from '~/enums/InterviewType';

import { ICardInterview } from '../../interfaces/IInterview';

import { getGroupedSortedInterviews } from './CardInterviewsInfo.util';

describe('CardInterviewsInfo.util', function() {
    describe('getGroupedSortedInterviews', function() {
        it('should sort and group interviews', function() {
            const source: ICardInterview[] = [
                {
                    id: 2,
                    type: INTERVIEW_TYPE.REGULAR,
                    grade: 6,
                    resolution: '',
                    state: INTERVIEW_STATUS.ESTIMATED,
                },
                {
                    id: 1,
                    type: INTERVIEW_TYPE.SCREENING,
                    resolution: INTERVIEW_RESOLUTION.HIRE,
                    grade: null,
                    state: INTERVIEW_STATUS.ASSIGNED,
                },
                {
                    id: 3,
                    type: INTERVIEW_TYPE.AA,
                    grade: 6,
                    resolution: '',
                    state: INTERVIEW_STATUS.ESTIMATED,
                },
                {
                    id: 10,
                    type: INTERVIEW_TYPE.HR_SCREENING,
                    grade: null,
                    resolution: INTERVIEW_RESOLUTION.HIRE,
                    state: INTERVIEW_STATUS.ESTIMATED,
                },
                {
                    id: 4,
                    type: INTERVIEW_TYPE.REGULAR,
                    grade: 3,
                    resolution: '',
                    state: INTERVIEW_STATUS.ESTIMATED,
                },
                {
                    id: 5,
                    type: INTERVIEW_TYPE.SCREENING,
                    grade: null,
                    resolution: INTERVIEW_RESOLUTION.NOHIRE,
                    state: INTERVIEW_STATUS.ESTIMATED,
                },
                {
                    id: 6,
                    type: INTERVIEW_TYPE.AA,
                    grade: 0,
                    resolution: '',
                    state: INTERVIEW_STATUS.ESTIMATED,
                },
                {
                    id: 8,
                    type: INTERVIEW_TYPE.FINAL,
                    grade: null,
                    resolution: INTERVIEW_RESOLUTION.HIRE,
                    state: INTERVIEW_STATUS.ESTIMATED,
                },
                {
                    id: 7,
                    type: INTERVIEW_TYPE.REGULAR,
                    grade: 2,
                    resolution: '',
                    state: INTERVIEW_STATUS.ESTIMATED,
                },
                {
                    id: 9,
                    type: INTERVIEW_TYPE.FINAL,
                    grade: null,
                    resolution: INTERVIEW_RESOLUTION.NOHIRE,
                    state: INTERVIEW_STATUS.ESTIMATED,
                },
            ];

            const getById = id => source.find(item => item.id === id);

            const result: Dictionary<ICardInterview[]> = {
                hr_screening: [10].map(getById),
                screening: [1, 5].map(getById),
                regular: [7, 4, 2].map(getById),
                aa: [6, 3].map(getById),
                final: [8, 9].map(getById),
            };

            expect(getGroupedSortedInterviews(source)).toEqual(result);
        });
    });
});
