import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createGainExpertise} from '@yandex-market/kadavr/mocks/PersAuthor/helpers';

import {MODEL_GRADE, SHOP_GRADE, MODEL_VIDEO} from '@self/root/src/entities/agitation/constants';

// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
// suites
import GainedExpertiseSuite from '@self/project/src/spec/hermione/test-suites/blocks/widgets/content/GainedExpertise';
// page-objects
import GainedExpertise from '@self/project/src/widgets/content/GainedExpertise/__pageObject';

const USER_PROFILE_CONFIG = profiles.ugctest3;
const DEFAULT_USER = createUser({
    id: USER_PROFILE_CONFIG.uid,
    uid: {
        value: USER_PROFILE_CONFIG.uid,
    },
    login: USER_PROFILE_CONFIG.login,
    display_name: {
        name: 'Willy Wonka',
        public_name: 'Willy W.',
        avatar: {
            default: '61207/462703116-1544492602',
            empty: false,
        },
    },
    dbfields: {
        'userinfo.firstname.uid': 'Willy',
        'userinfo.lastname.uid': 'Wonka',
    },
});

async function prepareExpertise(ctx, gradeType, gainedValue) {
    const gainExpertise = createGainExpertise(gradeType, gainedValue, USER_PROFILE_CONFIG.uid);
    await ctx.browser.setState('storage', {gainExpertise});
    await ctx.browser.yaProfile(USER_PROFILE_CONFIG.login, 'touch:index');
    await ctx.browser.yaOpenPage('market:my-tasks');
}

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница личного кабинета. Вкладка с заданиями пользователя.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('schema', {
                    users: [DEFAULT_USER],
                });
            },
        },
        {
            'Пользователю выдана новая экспертность за оценку модели.': prepareSuite(GainedExpertiseSuite, {
                pageObjects: {
                    gainedExpertise() {
                        return this.createPageObject(GainedExpertise);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await prepareExpertise(this, MODEL_GRADE, 3);
                    },
                },
                params: {
                    expectedBadgeText: '',
                },
            }),
        },
        {
            'Пользователю выдана новая экспертность за оценку магазина.': prepareSuite(GainedExpertiseSuite, {
                pageObjects: {
                    gainedExpertise() {
                        return this.createPageObject(GainedExpertise);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await prepareExpertise(this, SHOP_GRADE, 1);
                    },
                },
                params: {
                    expectedBadgeText: '',
                },
            }),
        },
        {
            'Пользователю выдана новая экспертность за добавленное видео.': prepareSuite(GainedExpertiseSuite, {
                pageObjects: {
                    gainedExpertise() {
                        return this.createPageObject(GainedExpertise);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await prepareExpertise(this, MODEL_VIDEO, 50);
                    },
                },
                params: {
                    expectedBadgeText: 'Вы достигли 2 уровня',
                },
            }),
        }
    ),
});
