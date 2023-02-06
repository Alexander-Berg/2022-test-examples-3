/* eslint-disable */
import { ExecutionContext } from 'ava';
import { pick } from 'lodash';
import { DraftUserAgreementInstance } from '../../../../../db/tables/draftUserAgreement';
import { PublishedUserAgreementInstance } from '../../../../../db/tables/publishedUserAgreeement';

const serializedFields = ['id', 'name', 'order', 'skillId', 'url'];

export const assertSerializedUserAgreementEqualsModel = (
    model: DraftUserAgreementInstance | PublishedUserAgreementInstance,
    serialized: any,
    t: ExecutionContext,
) => {
    t.deepEqual({ ...pick(model, serializedFields) }, serialized);
};
