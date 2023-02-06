import { Dictionary } from 'lodash';
import nock from 'nock';

import config from '@yandex-int/yandex-cfg';

import { Participant } from 'src/shared/types/participant';
import { Stats } from 'src/shared/types/stats';
import { Contest } from 'src/shared/types/contest';
import contestUrls from 'src/shared/urls/contest';

interface ParticipantsOptions {
    contestId: string;
    query?: Dictionary<string>;
    response?: Participant[];
}

interface RunsOptions {
    contestId: string;
    participantId: number;
    query?: Dictionary<string>;
    response?: Stats;
}

interface ContestOptions {
    contestId: string;
    response?: Contest;
}

export const nockParticipants = (options: ParticipantsOptions) => {
    const { contestId, query, response = {} } = options;

    const path = contestUrls.getParticipants(contestId);

    nock(`${config.contest.protocol}://${config.contest.hostname}`)
        .get(`${config.contest.pathname}/${path}`)
        .query(query || {})
        .matchHeader('Authorization', `OAuth ${config.contest.authToken}`)
        .times(Infinity)
        .reply(response ? 200 : 500, response);
};

export const nockRuns = (options: RunsOptions) => {
    const { contestId, participantId, query, response = {} } = options;

    const path = contestUrls.getRuns(contestId, participantId);

    nock(`${config.contest.protocol}://${config.contest.hostname}`)
        .get(`${config.contest.pathname}/${path}`)
        .query(query || {})
        .matchHeader('Authorization', `OAuth ${config.contest.authToken}`)
        .times(Infinity)
        .reply(response ? 200 : 500, response);
};

export const nockContest = (options: ContestOptions) => {
    const { contestId, response = {} } = options;

    const path = contestUrls.getContest(contestId);

    nock(`${config.contest.protocol}://${config.contest.hostname}`)
        .get(`${config.contest.pathname}/${path}`)
        .matchHeader('Authorization', `OAuth ${config.contest.authToken}`)
        .times(Infinity)
        .reply(response ? 200 : 500, response);
};
