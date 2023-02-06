/* eslint-disable */
import test, { ExecutionContext } from 'ava';
import {
    sanitizeSkillAccess,
    getHideInStoreBySkillAccess,
    getSkillAccessByHideInStore,
    fixMismatchedSkillAccess,
    getPrivacyFromRawSources,
    fixMismatchedHideInStore,
} from '../../../services/skillPrivacy';
import { SkillAccess, Channel } from '../../../db/tables/settings';

test('sanitizeSkillAccess', t => {
    t.is(sanitizeSkillAccess('asdfasdf'), null);
    t.is(sanitizeSkillAccess(1), null);
    t.is(sanitizeSkillAccess(true), null);
    t.is(sanitizeSkillAccess({}), null);
    t.is(sanitizeSkillAccess([]), null);

    t.is(sanitizeSkillAccess('public'), SkillAccess.Public);
    t.is(sanitizeSkillAccess('private'), SkillAccess.Private);
    t.is(sanitizeSkillAccess('hidden'), SkillAccess.Hidden);
});

test('getHideInStoreBySkillAccess', t => {
    t.is(getHideInStoreBySkillAccess(SkillAccess.Hidden), true);
    t.is(getHideInStoreBySkillAccess(SkillAccess.Private), true);
    t.is(getHideInStoreBySkillAccess(SkillAccess.Public), false);
});

test('getSkillAccessByHideInStore', t => {
    t.is(getSkillAccessByHideInStore(true, Channel.AliceSkill), SkillAccess.Hidden);
    t.is(getSkillAccessByHideInStore(true, Channel.SmartHome), SkillAccess.Private);
    t.is(getSkillAccessByHideInStore(true, Channel.Thereminvox), SkillAccess.Private);
    t.is(getSkillAccessByHideInStore(true, Channel.OrganizationChat), SkillAccess.Hidden);
    t.is(getSkillAccessByHideInStore(false, Channel.AliceSkill), SkillAccess.Public);
    t.is(getSkillAccessByHideInStore(false, Channel.SmartHome), SkillAccess.Public);
    t.is(getSkillAccessByHideInStore(false, Channel.Thereminvox), SkillAccess.Public);
    t.is(getSkillAccessByHideInStore(false, Channel.OrganizationChat), SkillAccess.Public);
});

test('fixMismatchedSkillAccess', t => {
    t.is(fixMismatchedSkillAccess(SkillAccess.Hidden, Channel.AliceSkill), SkillAccess.Hidden);
    t.is(fixMismatchedSkillAccess(SkillAccess.Public, Channel.AliceSkill), SkillAccess.Public);
    t.is(fixMismatchedSkillAccess(SkillAccess.Private, Channel.AliceSkill), SkillAccess.Private);

    t.is(fixMismatchedSkillAccess(SkillAccess.Hidden, Channel.SmartHome), SkillAccess.Private);
    t.is(fixMismatchedSkillAccess(SkillAccess.Public, Channel.SmartHome), SkillAccess.Public);
    t.is(fixMismatchedSkillAccess(SkillAccess.Private, Channel.SmartHome), SkillAccess.Private);

    t.is(fixMismatchedSkillAccess(SkillAccess.Hidden, Channel.Thereminvox), SkillAccess.Private);
    t.is(fixMismatchedSkillAccess(SkillAccess.Public, Channel.Thereminvox), SkillAccess.Private);
    t.is(fixMismatchedSkillAccess(SkillAccess.Private, Channel.Thereminvox), SkillAccess.Private);

    t.is(fixMismatchedSkillAccess(SkillAccess.Hidden, Channel.OrganizationChat), SkillAccess.Hidden);
    t.is(fixMismatchedSkillAccess(SkillAccess.Public, Channel.OrganizationChat), SkillAccess.Public);
    t.is(fixMismatchedSkillAccess(SkillAccess.Private, Channel.OrganizationChat), SkillAccess.Hidden);
});

test('fixMismatchedHideInStore', t => {
    t.is(fixMismatchedHideInStore(true, Channel.AliceSkill), true);
    t.is(fixMismatchedHideInStore(false, Channel.AliceSkill), false);

    t.is(fixMismatchedHideInStore(true, Channel.SmartHome), true);
    t.is(fixMismatchedHideInStore(false, Channel.SmartHome), false);

    t.is(fixMismatchedHideInStore(true, Channel.Thereminvox), true);
    t.is(fixMismatchedHideInStore(false, Channel.Thereminvox), true);

    t.is(fixMismatchedHideInStore(true, Channel.OrganizationChat), true);
    t.is(fixMismatchedHideInStore(false, Channel.OrganizationChat), false);
});

const testGetPrivacyFromRawSources = (
    t: ExecutionContext,
    [actualSkillAccess, actualHideInStore, actualChannel]: [any, any, Channel],

    [expectedSkillAccess, expectedHideInStore]: [SkillAccess, boolean],
) => {
    t.deepEqual(getPrivacyFromRawSources(actualSkillAccess, actualHideInStore, actualChannel), {
        hideInStore: expectedHideInStore,
        skillAccess: expectedSkillAccess,
    });
};

test('getPrivacyFromRawSources', t => {
    testGetPrivacyFromRawSources(t, ['public', true, Channel.AliceSkill], [SkillAccess.Public, false]);
    testGetPrivacyFromRawSources(t, ['private', true, Channel.AliceSkill], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['hidden', true, Channel.AliceSkill], [SkillAccess.Hidden, true]);
    testGetPrivacyFromRawSources(t, ['public', false, Channel.AliceSkill], [SkillAccess.Public, false]);
    testGetPrivacyFromRawSources(t, ['private', false, Channel.AliceSkill], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['hidden', false, Channel.AliceSkill], [SkillAccess.Hidden, true]);

    testGetPrivacyFromRawSources(t, ['public', true, Channel.SmartHome], [SkillAccess.Public, false]);
    testGetPrivacyFromRawSources(t, ['private', true, Channel.SmartHome], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['hidden', true, Channel.SmartHome], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['public', false, Channel.SmartHome], [SkillAccess.Public, false]);
    testGetPrivacyFromRawSources(t, ['private', false, Channel.SmartHome], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['hidden', false, Channel.SmartHome], [SkillAccess.Private, true]);

    testGetPrivacyFromRawSources(t, ['public', true, Channel.Thereminvox], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['private', true, Channel.Thereminvox], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['hidden', true, Channel.Thereminvox], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['public', false, Channel.Thereminvox], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['private', false, Channel.Thereminvox], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['hidden', false, Channel.Thereminvox], [SkillAccess.Private, true]);

    testGetPrivacyFromRawSources(t, ['public', true, Channel.OrganizationChat], [SkillAccess.Public, false]);
    testGetPrivacyFromRawSources(t, ['private', true, Channel.OrganizationChat], [SkillAccess.Hidden, true]);
    testGetPrivacyFromRawSources(t, ['hidden', true, Channel.OrganizationChat], [SkillAccess.Hidden, true]);
    testGetPrivacyFromRawSources(t, ['public', false, Channel.OrganizationChat], [SkillAccess.Public, false]);
    testGetPrivacyFromRawSources(t, ['private', false, Channel.OrganizationChat], [SkillAccess.Hidden, true]);
    testGetPrivacyFromRawSources(t, ['hidden', false, Channel.OrganizationChat], [SkillAccess.Hidden, true]);

    testGetPrivacyFromRawSources(t, ['invalid', true, Channel.AliceSkill], [SkillAccess.Hidden, true]);
    testGetPrivacyFromRawSources(t, ['invalid', true, Channel.SmartHome], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['invalid', true, Channel.Thereminvox], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['invalid', true, Channel.OrganizationChat], [SkillAccess.Hidden, true]);

    testGetPrivacyFromRawSources(t, ['invalid', false, Channel.AliceSkill], [SkillAccess.Public, false]);
    testGetPrivacyFromRawSources(t, ['invalid', false, Channel.SmartHome], [SkillAccess.Public, false]);
    testGetPrivacyFromRawSources(t, ['invalid', false, Channel.Thereminvox], [SkillAccess.Private, true]);
    testGetPrivacyFromRawSources(t, ['invalid', false, Channel.OrganizationChat], [SkillAccess.Public, false]);
});
