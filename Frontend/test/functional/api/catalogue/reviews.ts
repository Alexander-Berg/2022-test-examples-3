/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { markDeleted } from '../../../../services/skill-lifecycle';
import { callApi, createStoreSkill } from './_helpers';
import { createUser, wipeDatabase } from '../../_helpers';
import { getUserTicket, testUser } from '../_helpers';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
});

test('Get review by unauthenticated user', async t => {
    const res = await callApi('get', '/personal/reviews/550e8400-e29b-41d4-a716-446655440000/my');
    t.is(res.status, 403);
});

test('Add review by unauthenticated user', async t => {
    const res = await callApi('post', '/personal/reviews/550e8400-e29b-41d4-a716-446655440000');
    t.is(res.status, 403);
});

test('Add review to removed or nonexistent skill', async t => {
    await createUser({ id: testUser.uid });

    const review = {
        rating: 1,
        reviewText: 'ok',
        quickAnswers: ['Не отвечает'],
    };

    let res = await callApi('post', '/personal/reviews/550e8400-e29b-41d4-a716-446655440000', {
        userTicket: t.context.userTicket,
    }).send(review);

    t.is(res.status, 404);

    const skill = await createStoreSkill();

    res = await callApi('post', `/personal/reviews/${skill.id}`, {
        userTicket: t.context.userTicket,
    }).send(review);

    t.is(res.status, 201);

    await markDeleted(skill);

    res = await callApi('post', `/personal/reviews/${skill.id}`, {
        userTicket: t.context.userTicket,
    }).send(review);

    t.is(res.status, 404);
});

test('Get nonexistent review by authenticated user', async t => {
    await createUser({ id: testUser.uid });
    const { id } = await createStoreSkill();

    const getReviewRes = await callApi('get', `/personal/reviews/${id}/my`, {
        userTicket: t.context.userTicket,
    });

    t.is(getReviewRes.body.result, null);
});

test('Get existent review of deleted skill', async t => {
    await createUser({ id: testUser.uid });
    const skill = await createStoreSkill();

    const res = await callApi('post', `/personal/reviews/${skill.id}`, {
        userTicket: t.context.userTicket,
    }).send({
        rating: 1,
        reviewText: 'ok',
        quickAnswers: ['Не отвечает'],
    });

    t.is(res.status, 201);

    await markDeleted(skill);

    const getReviewRes = await callApi('get', `/personal/reviews/${skill.id}/my`, {
        userTicket: t.context.userTicket,
    });

    t.is(getReviewRes.status, 200);
    t.is(getReviewRes.body.result, null);
});

test('Add review', async t => {
    await createUser({ id: testUser.uid });
    const { id } = await createStoreSkill();

    const review = {
        rating: 1,
        reviewText: 'ok',
        quickAnswers: ['Не отвечает'],
    };

    const res = await callApi('post', `/personal/reviews/${id}`, {
        userTicket: t.context.userTicket,
    }).send(review);

    t.is(res.status, 201);

    const getReviewRes = await callApi('get', `/personal/reviews/${id}/my`, {
        userTicket: t.context.userTicket,
    });

    t.is(getReviewRes.status, 200);
    t.deepEqual(getReviewRes.body.result, review);
});

test('Add invalid review', async t => {
    await createUser({ id: testUser.uid });
    const { id } = await createStoreSkill();

    const reviews = [
        {
            rating: '',
            reviewText: 'ok',
            quickAnswers: [],
        },
        {
            rating: null,
            reviewText: 'ok',
            quickAnswers: [],
        },
        {
            rating: 4,
            reviewText: 4,
        },
        {
            rating: 5,
            reviewText: 'ok',
            quickAnswers: [3, 6],
        },
        {},
    ];

    for (const review of reviews) {
        const res = await callApi('post', `/personal/reviews/${id}`, {
            userTicket: t.context.userTicket,
        }).send(review);

        t.is(res.status, 400);
    }
});

test('Update existing review', async t => {
    await createUser({ id: testUser.uid });
    const { id } = await createStoreSkill();

    const review = {
        rating: 1,
        reviewText: 'ok',
        quickAnswers: ['Не отвечает'],
    };

    await callApi('post', `/personal/reviews/${id}`, {
        userTicket: t.context.userTicket,
    }).send(review);

    const updatedReview = {
        ...review,
        rating: 3,
    };

    await callApi('post', `/personal/reviews/${id}`, {
        userTicket: t.context.userTicket,
    }).send(updatedReview);

    const res = await callApi('get', `/personal/reviews/${id}/my`, {
        userTicket: t.context.userTicket,
    });

    t.is(res.status, 200);
    t.deepEqual(res.body.result, updatedReview);
});
