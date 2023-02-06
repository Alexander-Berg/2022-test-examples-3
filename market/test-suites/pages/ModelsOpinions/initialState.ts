'use strict';

import _ from 'lodash';
import {normalize, schema} from 'normalizr';

import buildUrl from 'spec/lib/helpers/buildUrl';
import baseModelOpinionsJson from 'spec/lib/page-mocks/modelOpinions.json';

const NUMBER_OF_GENERATED_MODEL_OPINIONS = 20; // еще 5 есть в baseModelOpinionsJson, итого хватит на две страницы

const userIds = _.keys(baseModelOpinionsJson.entities.user);
// @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
const getUserId = id => Number(userIds[(id - 1) % userIds.length]);

// @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
const createModelOpinionById = id => ({
    id,
    anonymous: false,
    model: {
        id: 1722193751,
        title: 'Смартфон Samsung Galaxy S8',
    },
    canReply: true,
    cpa: false,
    user: getUserId(id),
    created: '2018-04-01T10:07:25Z',
    review: {
        pro: 'улкдокоипцдклеп',
        contra: 'дуцлптжуклетп',
        comment: 'жущцкшпзкжщуре пж',
    },
    recommended: false,
    averageGrade: 5,
    photos: [
        {
            url: buildUrl('external:opinion-picture:full', {pictureId: '364668/img_id35591694493424954'}),
        },
    ],
    comments: [
        {
            id: 100001939,
            text: 'HELL FROM AUTOTEST!',
            user: 634321247,
            canEdit: true,
            updateTime: '2018-06-14T14:18:40Z',
        },
    ],
});

// @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
const getModelOpinions = count => _.map(new Array(count), (value, index) => createModelOpinionById(index + 1));

const {result, entities} = normalize(getModelOpinions(NUMBER_OF_GENERATED_MODEL_OPINIONS), [
    new schema.Entity('modelOpinion'),
]);

export default {
    entities: _.merge(entities, baseModelOpinionsJson.entities),
    result: _.concat(result, baseModelOpinionsJson.result),
};
