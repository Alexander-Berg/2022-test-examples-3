import {prepareSuite} from 'ginny';

import {NOT_VIEWED} from '@self/root/src/constants/a11y';

// pageObject
import ReferralProgramMenuItem from '@self/root/src/widgets/content/ReferralProgramMenuItem/__pageObject';
import SpecialMenuItem from '@self/root/src/components/SpecialMenuItem/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';

import userMenuSuite from './userMenuSuite';
import prepareState from './prepareState/';

const SUBTITLE = `И получайте 300 баллов Плюса\nза каждого друга\n${NOT_VIEWED}`;

export default {
    beforeEach() {
        this.setPageObjects({
            referralProgramMenuItem: () => this.createPageObject(SpecialMenuItem, {
                root: ReferralProgramMenuItem.root,
            }),
            menuItemLink: () => this.createPageObject(Link, {
                parent: this.referralProgramMenuItem,
            }),
        });
    },
    'Акция доступна,': {
        'Пользователь не достиг максимального количества баллов,': prepareSuite(userMenuSuite({
            shouldBeShown: true,
            subTitle: SUBTITLE,
        }), {
            meta: {
                id: 'marketfront-4810',
            },
            hooks: {
                async beforeEach() {
                    await prepareState.call(this, {
                        isReferralProgramActive: true,
                        isGotFullReward: false,
                    });
                },
            },
        }),
        'Пользователь достиг максимального количества баллов,': prepareSuite(userMenuSuite({
            shouldBeShown: true,
        }), {
            meta: {
                id: 'marketfront-4812',
            },
            hooks: {
                async beforeEach() {
                    await prepareState.call(this, {
                        isReferralProgramActive: true,
                        isGotFullReward: true,
                    });
                },
            },
        }),
    },
    'Акция не доступна.': prepareSuite(userMenuSuite({
        shouldBeShown: false,
    }), {
        meta: {
            id: 'marketfront-4996',
        },
        hooks: {
            async beforeEach() {
                await prepareState.call(this, {
                    isReferralProgramActive: false,
                });
            },
        },
    }),
};
