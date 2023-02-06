/* eslint-disable */
import test from 'ava';
import { validateBody } from '../../../../../routes/catalogue/v1/personal/news/utils';

test('validateBody: accepts one news source', t => {
    t.true(
        validateBody({
            selectedNewsSources: ['feed-id'],
        }),
    );
});

test('validateBody: accepts no news source', t => {
    t.true(
        validateBody({
            selectedNewsSources: [],
        }),
    );
});

test('validateBody: do not accepts several news source', t => {
    t.false(
        validateBody({
            selectedNewsSources: ['feed-id-1', 'feed-id-2'],
        }),
    );
});
