/* eslint-disable */
import '../../_helpers';

import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { DraftStatus } from '../../../../db/tables/draft';
import { getETag } from '../../../../services/moderation/eTag';
import { Channel } from '../../../../db/tables/settings';
import * as unistat from '../../../../services/unistat';
import { wipeDatabase } from '../../_helpers';
import { countOperaions, createSkillWithImageInDraft, serializeModeratorResponses } from './_helpers';
import {
    parseModeratorResponses,
    processModeratorResponse,
    processModeratorResponses,
} from '../../../../services/moderation/moderatorResponseProcessor';
import { requestReview, markDeleted } from '../../../../services/skill-lifecycle';

const test = anyTest as TestInterface<{
    incModerationStat: sinon.SinonSpy;
}>;

const positiveOutput = {
    category: 'ok',
};
const negativeOutput = {
    answer: 'wrong category',
    additional: {
        mistake: true,
    },
};

test.beforeEach(async t => {
    t.context.incModerationStat = sinon.spy(unistat, 'incModerationStat');

    await wipeDatabase();
});

test.afterEach.always(t => {
    t.context.incModerationStat.restore();
});

test('operation created after approve', async t => {
    const skill = await createSkillWithImageInDraft();
    const input = {
        id: skill.id,
        eTag: getETag(skill),
    };
    await processModeratorResponse(input, positiveOutput);

    const { reviewApproved, reviewCancelled } = await countOperaions(skill);
    t.is(reviewApproved, 1);
    t.is(reviewCancelled, undefined);
});

test('operation created after cancel', async t => {
    const skill = await createSkillWithImageInDraft();
    const input = {
        id: skill.id,
        eTag: getETag(skill),
    };
    await processModeratorResponse(input, negativeOutput);

    const { reviewApproved, reviewCancelled } = await countOperaions(skill);
    t.is(reviewApproved, undefined);
    t.is(reviewCancelled, 1);
});

test('only one operation created after two appoves', async t => {
    const skill = await createSkillWithImageInDraft();
    const input = {
        id: skill.id,
        eTag: getETag(skill),
    };
    await processModeratorResponse(input, positiveOutput);
    await processModeratorResponse(input, positiveOutput);

    const { reviewApproved, reviewCancelled } = await countOperaions(skill);
    t.is(reviewApproved, 1);
    t.is(reviewCancelled, undefined);
});

test('only one operation created after two cancels', async t => {
    const skill = await createSkillWithImageInDraft();
    await requestReview(skill, { user: skill.user });
    const input = {
        id: skill.id,
        eTag: getETag(skill),
    };
    await processModeratorResponse(input, negativeOutput);
    await processModeratorResponse(input, negativeOutput);

    const { reviewApproved, reviewCancelled } = await countOperaions(skill);
    t.is(reviewApproved, undefined);
    t.is(reviewCancelled, 1);
});

test('review not applied if eTag has changed', async t => {
    const skill = await createSkillWithImageInDraft();
    const input = {
        id: skill.id,
        eTag: getETag(skill),
    };

    skill.draft.name = 'new name';
    skill.draft.status = DraftStatus.ReviewRequested;
    await skill.draft.save();

    await processModeratorResponse(input, negativeOutput);
    await skill.draft.reload();

    t.deepEqual(skill.draft.status, DraftStatus.ReviewRequested);
    const { reviewApproved, reviewCancelled } = await countOperaions(skill);
    t.deepEqual(reviewApproved, undefined);
    t.deepEqual(reviewCancelled, undefined);
    t.true(t.context.incModerationStat.calledOnceWith('skillSkipped'));
});

test('organization chat review processed as chat', async t => {
    const chat = await createSkillWithImageInDraft();
    await chat.update({ channel: Channel.OrganizationChat });
    await chat.draft.update({ status: DraftStatus.ReviewRequested });
    const input = {
        id: chat.id,
        eTag: getETag(chat),
    };
    const output = {
        answer: 'Wrong category',
        additional: {
            mistake: true,
        },
    };

    await processModeratorResponse(input, output);
    await chat.draft.reload();
    t.deepEqual(chat.draft.status, DraftStatus.InDevelopment);
});

test('parse empty table', async t => {
    const rows = parseModeratorResponses('', 'myTable');
    t.deepEqual(rows, []);
});

test('parse 1 row', async t => {
    const data = [
        {
            input: {
                id: 'e51e0615-d460-47d8-955a-d49c1bc3b2d3',
                eTag: 'bb1553c4dfc3cfa9e21b2f10a4cd1bf1',
            },
            output: {
                category: 'true',
            },
        },
    ];
    const serialized = serializeModeratorResponses(data);
    const parsed = parseModeratorResponses(serialized, 'myTable');

    t.is(parsed.length, 1);
    t.deepEqual(parsed, data);
});

test('parse multiple rows', async t => {
    const data = [
        {
            input: {
                id: 'e51e0615-d460-47d8-955a-d49c1bc3b2d3',
                eTag: 'ab1553c4dfc3cfa9e21b2f10a4cd1bf1',
            },
            output: {
                category: 'true',
            },
        },
        {
            input: {
                id: '0c2fcf64-e361-493a-9876-a5ad3647e58d',
                eTag: 'bb1553c4dfc3cfa9e21b2f10a4cd1bf1',
            },
            output: {
                category: 'wrong category',
            },
        },
        {
            input: {
                id: 'e507b812-3b65-4a46-b508-1d1093266ba7',
                eTag: 'cb1553c4dfc3cfa9e21b2f10a4cd1bf1',
            },
            output: {
                category: 'invalid category',
            },
        },
    ];
    const serialized = serializeModeratorResponses(data);
    const parsed = parseModeratorResponses(serialized, 'myTable');

    t.is(parsed.length, 3);
    t.deepEqual(parsed, data);
});

test('apply moderator responses', async t => {
    const skill = await createSkillWithImageInDraft();
    const data = [
        {
            input: {
                id: skill.id,
                eTag: getETag(skill),
            },
            output: positiveOutput,
        },
    ];

    const processed = await processModeratorResponses(data);
    await skill.draft.reload();

    t.is(processed, 1);
    t.deepEqual(skill.draft.status, DraftStatus.ReviewApproved);

    t.true(t.context.incModerationStat.calledOnce);
    t.deepEqual(t.context.incModerationStat.getCall(0).args, ['skillApproved']);
});

test("invalid skill id doens't affect other skills", async t => {
    const skill = await createSkillWithImageInDraft();
    const data = [
        {
            input: {
                id: skill.id,
                eTag: getETag(skill),
            },
            output: positiveOutput,
        },
        {
            input: {
                id: 'e507b812-3b65-4a46-b508-1d1093266ba7',
                eTag: 'cb1553c4dfc3cfa9e21b2f10a4cd1bf1',
            },
            output: positiveOutput,
        },
    ];

    const processed = await processModeratorResponses(data);
    await skill.draft.reload();

    t.is(processed, 1);
    t.deepEqual(skill.draft.status, DraftStatus.ReviewApproved);

    t.deepEqual(t.context.incModerationStat.callCount, 2);
    t.deepEqual(t.context.incModerationStat.getCall(0).args, ['skillApproved']);
    t.deepEqual(t.context.incModerationStat.getCall(1).args, ['skillSkipped']);
});

test('invalid alice skill moderator responses are not applied', async t => {
    const skill = await createSkillWithImageInDraft();
    await skill.draft.update({
        status: DraftStatus.ReviewRequested,
    });
    const data = [
        {
            input: {
                id: skill.id,
                eTag: getETag(skill),
            },
            output: {
                unknownKey: 'true',
            },
        },
    ];
    const processed = await processModeratorResponses(data as any);
    await skill.draft.reload();
    t.is(processed, 1);
    await skill.draft.reload();
    t.deepEqual(skill.draft.status, DraftStatus.ReviewRequested);

    t.true(t.context.incModerationStat.calledOnce);
    t.deepEqual(t.context.incModerationStat.getCall(0).args, ['validationError']);
});

test('deleted skill is marked as skillSkipped, not as unknownError unistat metric', async t => {
    const skill = await createSkillWithImageInDraft();
    await skill.draft.update({
        status: DraftStatus.ReviewRequested,
    });
    await markDeleted(skill);

    const data = [
        {
            input: {
                id: skill.id,
                eTag: getETag(skill),
            },
            output: positiveOutput,
        },
    ];
    await processModeratorResponses(data as any);
    t.true(t.context.incModerationStat.calledOnceWith('skillSkipped'));
});

test('insignificant mistakes are ignored', async t => {
    const skill = await createSkillWithImageInDraft();
    const data = [
        {
            input: {
                id: skill.id,
                eTag: getETag(skill),
            },
            output: {
                ...negativeOutput,
                additional: {
                    w_was_it_worth_it: true,
                },
            },
        },
    ];

    await processModeratorResponses(data);
    await skill.draft.reload();

    t.deepEqual(skill.draft.status, DraftStatus.ReviewApproved);
});

test('mixed mistakes are applied', async t => {
    const skill = await createSkillWithImageInDraft();
    const data = [
        {
            input: {
                id: skill.id,
                eTag: getETag(skill),
            },
            output: {
                ...negativeOutput,
                additional: {
                    significant_mistake: true,
                    w_was_it_worth_it: true,
                },
            },
        },
    ];

    await processModeratorResponses(data);
    await skill.draft.reload();

    t.deepEqual(skill.draft.status, DraftStatus.InDevelopment);
});
