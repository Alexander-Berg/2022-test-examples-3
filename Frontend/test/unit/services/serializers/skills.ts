/* eslint-disable */
import test from 'ava';
import { getIsRecommended } from '../../../../serializers/skills';
import { SkillWithLogo } from '../../../../entities/skill';
import { SkillAccess } from '../../../../db/tables/settings';

test('getIsRecommended() returns false for private skills', t => {
    const skill = {
        isRecommended: true,
        automaticIsRecommended: true,
        hideInStore: true,
        skillAccess: SkillAccess.Hidden,
    } as SkillWithLogo;
    t.false(getIsRecommended(skill));
});

test('getIsRecommended() returns true for public skills with all flags equal to true', t => {
    const skill = {
        isRecommended: true,
        automaticIsRecommended: true,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    } as SkillWithLogo;
    t.true(getIsRecommended(skill));
});

test('getIsRecommended() returns true for public skills if automaticIsRecommended is null', t => {
    const skill = {
        isRecommended: true,
        automaticIsRecommended: null,
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    } as SkillWithLogo;
    t.true(getIsRecommended(skill));
});

test('getIsRecommended() returns false for public skills if automaticIsRecommended is false', t => {
    const skill = {
        isRecommended: true,
        automaticIsRecommended: false,
        hideInStore: false,
    } as SkillWithLogo;
    t.false(getIsRecommended(skill));
});

test('getIsRecommended() returns false for public skills if isRecommended is false', t => {
    const skill = {
        isRecommended: false,
        automaticIsRecommended: true,
        hideInStore: false,
    } as SkillWithLogo;
    t.false(getIsRecommended(skill));
});

test('getIsRecommended() returns false is isRecommended is null', t => {
    const skill = {
        isRecommended: null,
        automaticIsRecommended: true,
        hideInStore: false,
    } as SkillWithLogo;
    t.false(getIsRecommended(skill));
});
