/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { getServiceTicket, getUserTicket, testUser } from '../../_helpers';
import { SkillInstance } from '../../../../../db/tables/skill';
import { UserInstance } from '../../../../../db/tables/user';
import { callApi } from './_helpers';
import { UserReview } from '../../../../../db';

interface TestContext {
    serviceTicket: string;
    userTicket: string;
    skill: SkillInstance;
    user: UserInstance;
}

const test = anyTest as TestInterface<TestContext>;

test.before(async t => {
    const serviceTicket = await getServiceTicket();
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { serviceTicket, userTicket });
});

test.beforeEach(async t => {
    await wipeDatabase();
    t.context.user = await createUser({ id: testUser.uid });
    t.context.skill = await createSkill({
        onAir: true,
        userId: t.context.user.id,
    });
});

test.afterEach.always(t => {
    sinon.restore();
});

test('post review with no text', async t => {
    const response = await callApi('post', `/skills/${t.context.skill.id}/review`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    }).send({
        rating: 5,
        quickAnswers: [],
        reviewText: '',
    });
    t.is(response.status, 201);
    const reviewCount = await UserReview.count({
        where: {
            skillId: t.context.skill.id,
        },
    });
    t.is(reviewCount, 1);
    const review = (await UserReview.findOne({
        where: {
            userId: t.context.user.id,
            skillId: t.context.skill.id,
        },
    }))!;
    t.deepEqual(review.quickAnswers, []);
    t.is(review.reviewText, '');
});

test('post review with quick answer', async t => {
    const response = await callApi('post', `/skills/${t.context.skill.id}/review`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    }).send({
        rating: 5,
        quickAnswers: ['Было весело'],
        reviewText: '',
    });
    t.is(response.status, 201);
    const reviewCount = await UserReview.count({
        where: {
            skillId: t.context.skill.id,
        },
    });
    t.is(reviewCount, 1);
    const review = (await UserReview.findOne({
        where: {
            userId: t.context.user.id,
            skillId: t.context.skill.id,
        },
    }))!;
    t.deepEqual(review.quickAnswers, ['Было весело']);
    t.is(review.reviewText, '');
});

test('post review with custom text', async t => {
    const response = await callApi('post', `/skills/${t.context.skill.id}/review`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    }).send({
        rating: 5,
        quickAnswers: [],
        reviewText: 'крутой навык',
    });
    t.is(response.status, 201);
    const reviewCount = await UserReview.count({
        where: {
            skillId: t.context.skill.id,
        },
    });
    t.is(reviewCount, 1);
    const review = (await UserReview.findOne({
        where: {
            userId: t.context.user.id,
            skillId: t.context.skill.id,
        },
    }))!;
    t.deepEqual(review.quickAnswers, []);
    t.is(review.reviewText, 'крутой навык');
});

test('post review with rating less than 1', async t => {
    const response = await callApi('post', `/skills/${t.context.skill.id}/review`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    }).send({
        rating: 0,
        quickAnswers: [],
        reviewText: '',
    });
    t.is(response.status, 400);
});

test('post review with rating more than 5', async t => {
    const response = await callApi('post', `/skills/${t.context.skill.id}/review`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    }).send({
        rating: 6,
        quickAnswers: [],
        reviewText: '',
    });
    t.is(response.status, 400);
});

test('post review with no rating', async t => {
    const response = await callApi('post', `/skills/${t.context.skill.id}/review`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    }).send({
        quickAnswers: [],
        reviewText: '',
    });
    t.is(response.status, 400);
});

test('post review with invalid quick answer', async t => {
    const response = await callApi('post', `/skills/${t.context.skill.id}/review`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    }).send({
        rating: 5,
        quickAnswers: ['random phrase'],
        reviewText: '',
    });
    t.is(response.status, 400);
});
