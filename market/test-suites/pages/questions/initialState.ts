'use strict';

import _ from 'lodash';
import {normalize, schema} from 'normalizr';

import baseJson from 'spec/lib/page-mocks/questions.json';

const NUMBER_OF_GENERATED_ITEMS = 20; // еще 5 есть в baseJson, итого хватит на две страницы

const userIds = _.keys(baseJson.entities.user);

const getUserId = (id: number) => Number(userIds[(id - 1) % userIds.length]);

const createById = (id: number) => ({
    id,
    answers: [],
    model: {
        id: 1722193751,
        title: 'Wi-Fi роутер Cisco AIR-LAP1141N',
    },
    user: getUserId(id),
    created: '2018-04-11T10:07:25Z',
    votes: {
        likeCount: 0,
        dislikeCount: 0,
        userVote: 0,
    },
    text: 'what is this?',
});

const getModelQuestions = (count: number) => _.map(new Array(count), (value, index) => createById(index + 1));

const {result, entities} = normalize(getModelQuestions(NUMBER_OF_GENERATED_ITEMS), [
    new schema.Entity('productQuestion'),
]);

export default {
    entities: _.merge(entities, baseJson.entities),
    result: _.concat(result, baseJson.result),
};
