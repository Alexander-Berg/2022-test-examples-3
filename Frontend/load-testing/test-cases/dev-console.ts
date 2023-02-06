/* eslint-disable */
import { RouteOptions } from '../types';
import { RoutesStructure } from '../configs/routes';
import {
    actionsWithdrawGenerator,
    confirmPhoneGenerator,
    decConsoleV1OrganizationsGenerator,
    devConsoleV1EmailSendGenerator,
    devConsoleV1ModerationSkillSkillIdValidateTokenGenerator,
    devConsoleV1SkillsDeleteGenerator,
    devConsoleV1SkillsGenerator,
    devConsoleV1SnapshotGenerator,
    devConsoleV1SubscriptionGenerator,
    getOperationsGenerator,
    getSkillReviewsForResponseGenerator,
    patchSkillFlagsGenerator,
    postCandidateGenerator,
    postMessageGenerator,
    postReleaseGenerator,
    skillsSkillIdDraftGenerator,
    stopSkillGenerator,
    uploadSkillLogoGenerator,
    deletePhoneGenerator,
} from '../ammo/utils';

export const devconsoleV1SkillsSkillIdDraft: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills/:skillId/draft': {
        testCases: [
            {
                name: '2',
                method: 'PATCH',
                RPS: 1,
                generator: skillsSkillIdDraftGenerator,
            },
        ],
    },
};

export const devConsoleV1SkillsSkillIdDraftActionsWithdraw: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills/:skillId/draft/actions/withdraw': {
        testCases: [
            {
                name: '2',
                method: 'POST',
                RPS: 1,
                generator: actionsWithdrawGenerator,
            },
        ],
    },
};

export const devconsoleV1SkillsSkillIdFlags: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills/:skillId/flags': {
        testCases: [
            {
                name: '2',
                method: 'PATCH',
                RPS: 1,
                generator: patchSkillFlagsGenerator,
            },
        ],
    },
};

export const devConsoleV1SkillsNotificationsPhone: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills/notifications/phone': {
        testCases: [
            {
                name: '1',
                method: 'PATCH',
                RPS: 1,
                generator: confirmPhoneGenerator,
            },
            {
                name: '2',
                method: 'DELETE',
                RPS: 1,
                generator: deletePhoneGenerator,
            },
        ],
    },
};

export const devConsoleV1SkillsCandidate: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills/candidate': {
        testCases: [
            {
                name: '2',
                method: 'POST',
                RPS: 1,
                generator: postCandidateGenerator,
            },
        ],
    },
};

export const devConsoleV1SkillsRelease: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills/release': {
        testCases: [
            {
                name: '2',
                method: 'POST',
                RPS: 1,
                generator: postReleaseGenerator,
            },
        ],
    },
};

export const devConsoleV1SkillsMessage: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills/message': {
        testCases: [
            {
                name: '2',
                method: 'POST',
                RPS: 1,
                generator: postMessageGenerator,
            },
        ],
    },
};

export const devConsoleV1SkillsOperations: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills/operations': {
        testCases: [
            {
                name: '2',
                method: 'GET',
                RPS: 34,
                generator: getOperationsGenerator,
            },
        ],
    },
};

export const devConsoleV1SkillReviews: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills/reviews': {
        testCases: [
            {
                name: '2',
                method: 'GET',
                RPS: 1,
                generator: getSkillReviewsForResponseGenerator,
            },
        ],
    },
};

export const devConsoleV1SkillsLogo: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills/logo': {
        testCases: [
            {
                name: 'Logo-upload',
                method: 'POST',
                RPS: 1,
                generator: uploadSkillLogoGenerator,
            },
        ],
    },
};

export const devConsoleV1SkillsActionsStop: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills/actions/stop': {
        testCases: [
            {
                name: '2',
                method: 'POST',
                RPS: 1,
                generator: stopSkillGenerator,
            },
        ],
    },
};

// ручка getskill

export const devConsoleV1Skills: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills': {
        testCases: [
            {
                name: '1',
                method: 'GET',
                RPS: 1,
                generator: devConsoleV1SkillsGenerator,
            },
        ],
    },
};

export const devConsoleV1SkillsSkillId: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/skills/:skillId': {
        testCases: [
            {
                name: '2',
                method: 'GET',
                RPS: 1,
                generator: devConsoleV1SkillsDeleteGenerator,
            },

            {
                name: '2',
                method: 'DELETE',
                RPS: 1,
                generator: devConsoleV1SkillsDeleteGenerator,
            },
        ],
    },
};

// Снапшот

export const devConsoleV1Snapshot: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/snapshot': {
        testCases: [
            {
                name: '2',
                method: 'GET',
                RPS: 11,
                generator: devConsoleV1SnapshotGenerator,
            },
        ],
    },
};

/// org
export const decConsoleV1Organizations: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/organizations': {
        testCases: [
            {
                name: '2',
                method: 'GET',
                RPS: 1,
                generator: decConsoleV1OrganizationsGenerator,
            },
        ],
    },
};

export const devConsoleV1Subscription: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/subscription': {
        testCases: [
            {
                name: '2',
                method: 'POST',
                RPS: 1,
                generator: devConsoleV1SubscriptionGenerator,
            },
            {
                name: '2',
                method: 'DELETE',
                RPS: 1,
                generator: devConsoleV1SubscriptionGenerator,
            },
        ],
    },
};

// export const devConsoleV1Email: Partial<RoutesStructure<RouteOptions>> = {
//     'dev-console/v1/email': {
//         testCases: [
//             {
//                 name: '2',
//                 method: 'POST',
//                 RPS: 1,
//                 generator: devConsoleV1EmailGenerator,
//             },
//         ],
//     },
// };

export const devConsoleV1EmailSend: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/email/send': {
        testCases: [
            {
                name: '2',
                method: 'POST',
                RPS: 1,
                generator: devConsoleV1EmailSendGenerator,
            },
        ],
    },
};

export const devConsoleV1ModerationSkillSkillIdValidateToken: Partial<RoutesStructure<RouteOptions>> = {
    'dev-console/v1/moderation/skills/:skillId/validate-token': {
        testCases: [
            {
                name: '2',
                method: 'POST',
                RPS: 1,
                generator: devConsoleV1ModerationSkillSkillIdValidateTokenGenerator,
            },
        ],
    },
};

// export const devConsoleV1OauthApps: Partial<RoutesStructure<RouteOptions>> = {
//     'dev-console/v1/oauth/apps': {
//         testCases: [
//             {
//                 name: '2',
//                 method: 'GET',
//                 RPS: 1,
//                 generator: devConsoleV1OauthAppsGenerator,
//             },
//             {
//                 name: '2',
//                 method: 'POST',
//                 RPS: 1,
//                 generator: devConsoleV1OauthAppsGenerator,
//             },
//         ],
//     },
// };

// export const devConsoleV1OauthAppsAppId: Partial<RoutesStructure<RouteOptions>> = {
//     'dev-console/v1/oauth/apps/:appId': {
//         testCases: [
//             {
//                 name: '2',
//                 method: 'GET',
//                 RPS: 1,
//                 generator: devConsoleV1OauthAppsAppIdGenerator,
//             },
//             {
//                 name: '2',
//                 method: 'DELETE',
//                 RPS: 1,
//                 generator: devConsoleV1OauthAppsAppIdGenerator,
//             },
//         ],
//     },
// };
