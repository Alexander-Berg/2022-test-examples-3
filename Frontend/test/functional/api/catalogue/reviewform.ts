/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { callApi, respondsWithResult } from './_helpers';
import { BunkerData } from '../../../../types/BunkerData';
import * as bunker from '../../../../services/bunker';
import { ReviewFormInfo } from '../../../../types';

const test = anyTest as TestInterface;

test('get reviewform', async t => {
    const reviewforminfos: Record<number, ReviewFormInfo> = {
        '1': {
            question: 'Что не понравилось?',
            placeholder: 'Напишите, что вам не понравилось...',
            quickAnswers: ['Не отвечает', 'Ответил не то', 'Не понятно, что делает навык'],
        },
        '2': {
            question: 'Что не понравилось?',
            placeholder: 'Напишите, что вам не понравилось...',
            quickAnswers: ['Не отвечает', 'Ответил не то', 'Не понятно, что делает навык'],
        },
        '3': {
            question: 'Что не понравилось?',
            placeholder: 'Напишите, что вам понравилось...',
            quickAnswers: ['Не отвечает', 'Ответил не то', 'Не понятно, что делает навык'],
        },
        '4': {
            question: 'Что понравилось?',
            placeholder: 'Напишите, что вам понравилось...',
            quickAnswers: ['Быстро отвечает', 'Было весело', 'Просто и удобно'],
        },
        '5': {
            question: 'Что понравилось?',
            placeholder: 'Напишите, что вам понравилось...',
            quickAnswers: ['Быстро отвечает', 'Было весело', 'Просто и удобно', 'Очень круто'],
        },
    };

    const fetchFromBunker = sinon
        .stub(bunker, 'getBunkerData')
        .returns(Promise.resolve({ reviewforminfos } as BunkerData));

    const res = await callApi('get', '/reviewform/');
    respondsWithResult(
        {
            result: reviewforminfos,
        },
        res,
        t,
    );

    fetchFromBunker.restore();
});
