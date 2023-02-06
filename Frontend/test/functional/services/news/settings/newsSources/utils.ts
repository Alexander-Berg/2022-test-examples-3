/* eslint-disable */
import test from 'ava';
import { makeNewsSourceId } from '../../../../../../services/news/settings/newsSources/utils';

test('makeNewsSourceId', t => {
    t.is(makeNewsSourceId('slug', 'rubric'), 'slug:rubric');
    t.is(makeNewsSourceId('slug'), 'slug');
});
