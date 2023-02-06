/* eslint-disable */
import * as fs from 'fs';
import * as path from 'path';
import { GeneratorOptions, RouteGenerator } from '../types';

const defaultHeaders = ['Host: xxx.tanks.example.com', 'User-Agent: tank', 'Connection: Close'];

const withUserTicket = (uid: string, headers: string[]) => {
    return [...headers, `x-ya-user-ticket: ${uid}`];
};

const withServiceTicket = (headers: string[]) => {
    return [...headers, 'x-ya-service-ticket: service-ticket'];
};

const withBody = (body: string, headers: string[]) => {
    return [...headers, `Content-Length: ${body.length}`, 'content-type: application/json'];
};

interface MakeAmmoUnitOptions {
    filePath?: string;
    userId?: string;
    body?: string | object;
    headers?: string[];
    ammoLabel: string;
    method: RequestMethod;
    route: string;
    serviceTicket?: boolean;
}

const stringifyBody = (body?: object | string) => {
    if (typeof body !== 'string') {
        return JSON.stringify(body);
    }

    return body;
};

export const makeAmmoUnit = (options: MakeAmmoUnitOptions) => {
    const { userId, body: rawBody, ammoLabel, method, route, headers, filePath, serviceTicket } = options;
    const body = stringifyBody(rawBody);

    let ammoHeaders = defaultHeaders.concat(headers || []);

    if (userId) {
        ammoHeaders = withUserTicket(userId, ammoHeaders);
    }

    if (serviceTicket) {
        ammoHeaders = withServiceTicket(ammoHeaders);
    }

    if (filePath) {
        return makeFileAmmo({ filePath, headers: ammoHeaders, ammoLabel, route });
    }

    if (body) {
        ammoHeaders = withBody(body, ammoHeaders);
    }

    const ammoBody = [`${method} ${route} HTTP/1.1`, ...ammoHeaders, '', ...(body ? [body, '', ''] : [''])].join(
        '\r\n',
    );

    const ammo = `${ammoBody.length} ${ammoLabel.replace(' ', '')}\n${ammoBody}\n`;

    return ammo;
};

interface MakeFileOptions {
    filePath: string;
    headers: string[];
    ammoLabel: string;
    route: string;
}

export const makeFileAmmo = (options: MakeFileOptions) => {
    const { ammoLabel, route, headers } = options;
    const EOL = '\r\n';

    const filePath = path.isAbsolute(options.filePath) ? options.filePath : path.join(__dirname, options.filePath);

    const boundary = '------------------------------d7c3d7c4089e';
    const bodyPart = `Content-Disposition: form-data; name="file"; filename="${path.basename(filePath)}"`;
    const bodyType = 'Content-Type: application/octet-stream';
    const body = fs.readFileSync(filePath, {
        flag: 'r',
        encoding: 'ascii',
    });
    let fmtBody = '--' + boundary + EOL;
    fmtBody += bodyPart + EOL;
    fmtBody += bodyType + EOL + EOL;
    fmtBody += body + EOL + EOL;
    fmtBody += '--' + boundary + '--';
    let req = 'POST ' + route + ' HTTP/1.1' + EOL;
    req += headers.join(EOL) + EOL;
    req += 'Accept: */*' + EOL;
    // req += 'Expect: 100-continue' + EOL;
    req += 'Content-Type: multipart/form-data; boundary=' + boundary + EOL;
    req += 'Content-Length: ' + Buffer.byteLength(fmtBody) + EOL;
    req += EOL + fmtBody + EOL + EOL;
    return `${Buffer.byteLength(req)} ${ammoLabel.replace(' ', '')}\n` + req;
};

export const defaultGenerator = ({ method, route, getSkillId, getUserId, ammoLabel }: GeneratorOptions) => {
    const ammoRoute = typeof route === 'string' ? route : route(getSkillId());

    return makeAmmoUnit({
        method,
        route: ammoRoute,
        userId: getUserId(),
        ammoLabel,
    });
};

// патроны для каталога
export const categoGenerator = ({ method, route, ammoLabel }: GeneratorOptions<string>) => {
    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
    });
};

export const getDialogGenerator = ({ method, route, ammoLabel, getSlug }: GeneratorOptions<RouteGenerator>) => {
    const ammoRoute = route(getSlug());

    return makeAmmoUnit({
        method,
        route: ammoRoute,
        ammoLabel,
    });
};

export const findDialogGenerator = ({ method, route, ammoLabel }: GeneratorOptions<string>) => {
    return makeAmmoUnit({
        method,
        route: route + `?q=${encodeURIComponent('города')}`,
        ammoLabel,
    });
};

export const getSuggestGenerator = ({ method, route, ammoLabel }: GeneratorOptions<string>) => {
    return makeAmmoUnit({
        method,
        route: route + `?q=${encodeURIComponent('города')}`,
        ammoLabel,
    });
};

export const getDialogsByCategoryGenerator = ({ method, route, ammoLabel }: GeneratorOptions<string>) => {
    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
    });
};

export const getSkillSlugByIdGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillId,
}: GeneratorOptions<RouteGenerator>) => {
    const ammoRoute = route(getSkillId());

    return makeAmmoUnit({
        method,
        route: ammoRoute,
        ammoLabel,
    });
};

export const getSkillForAccountLinkingGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillWithOauthAppMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId } = getSkillWithOauthAppMeta();
    const ammoRoute = route(skillId);

    return makeAmmoUnit({
        method,
        route: ammoRoute,
        ammoLabel,
    });
};

export const getCollectionsGenerator = ({ method, route, ammoLabel }: GeneratorOptions<string>) => {
    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
    });
};

export const getBuiltinCollectionsGenerator = ({ method, route, ammoLabel }: GeneratorOptions<string>) => {
    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
    });
};

export const getReviewFormGenerator = ({ method, route, ammoLabel }: GeneratorOptions<string>) => {
    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
    });
};

export const getCompilationGenerator = ({ method, route, ammoLabel, getSlug }: GeneratorOptions<RouteGenerator>) => {
    const ammoRoute = route(getSlug());

    return makeAmmoUnit({
        method,
        route: ammoRoute,
        ammoLabel,
    });
};

export const getExperimentsGenerator = ({ method, route, ammoLabel }: GeneratorOptions<RouteGenerator>) => {
    const yandexuid = (Math.random() * 1e9).toFixed() + String(Date.now()).substr(0, 10);
    const ammoRoute = route(yandexuid);

    return makeAmmoUnit({
        method,
        route: ammoRoute,
        ammoLabel,
    });
};

export const getMainPageDataGenerator = ({ method, route, ammoLabel }: GeneratorOptions<string>) => {
    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
    });
};

export const getSmartHomeCategoryGenerator = ({ method, route, ammoLabel, getUserId }: GeneratorOptions<string>) => {
    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
        userId: getUserId(),
    });
};

// патроны для dev-console

export const skillsSkillIdDraftGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const body = {
        name: 'Pryjok so sholy',
        voice: 'good_oksana',
        activationPhrases: ['Pryjok so sholy'],
        hideInStore: true,
        noteForModerator: 'Note',
        yaCloudGrant: true,
        backendSettings: { uri: 'https://example.com' },
        publishingSettings: {
            category: 'music_audio',
            developerName: 'lol',
            explicitContent: false,
            structuredExamples: [],
            description: 'Desc',
            email: 'sanya.rogov35@yandex.ru',
        },
        requiredInterfaces: ['screen'],
        exactSurfaces: [],
        surfaceWhitelist: [],
        surfaceBlacklist: [],
        oauthAppId: null,
    };
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
        body: JSON.stringify(body),
    });
};

export const actionsWithdrawGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
    });
};

export const patchSkillFlagsGenerator = ({
    method,
    route,
    ammoLabel,
    getAdminId,
    getSkillId,
}: GeneratorOptions<RouteGenerator>) => {
    const body1 = {};

    return makeAmmoUnit({
        method,
        route: route(getSkillId()),
        ammoLabel,
        userId: getAdminId(),
        body: JSON.stringify(body1),
    });
};

export const deletePhoneGenerator = ({ method, route, ammoLabel, getSkillMeta }: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
    });
};

export const confirmPhoneGenerator = ({ method, route, ammoLabel, getSkillMeta }: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    const body = {
        confirmationId: '4234',
        code: '123456',
    };

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
        body: JSON.stringify(body),
    });
};

export const uploadSkillLogoGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
        filePath: '../../../testResources/testlogo.jpg',
    });
};

export const postMessageGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMetaWithWebhook,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMetaWithWebhook();

    const body1 = {
        isDraft: true,
        sessionId: '3fb570ae-65912d4d-a507054a-30141d2c',
        sessionSeq: 3,
        surface: 'mobile',
        text: 'ffwef',
    };

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
        body: JSON.stringify(body1),
    });
};

export const getOperationsGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
    });
};

export const postReleaseGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMetaReviewApproved,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMetaReviewApproved();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
    });
};

export const postCandidateGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMetaInDevelopment,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMetaInDevelopment();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
    });
};

export const stopSkillGenerator = ({ method, route, ammoLabel, getSkillMeta }: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
    });
};

export const getSkillReviewsForResponseGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
    });
};

export const devConsoleV1SkillsGenerator = ({ method, route, ammoLabel, getSkillMeta }: GeneratorOptions<string>) => {
    const { userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
        userId,
    });
};

export const devConsoleV1SkillsSmartHomeGenerator = ({
    method,
    route,
    ammoLabel,
    getUserId,
}: GeneratorOptions<string>) => {
    const body1 = {
        Channel: 'SmartHome',
    };

    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
        userId: getUserId(),
        body: JSON.stringify(body1),
    });
};

export const devConsoleV1SkillsOrganizationGenerator = ({
    method,
    route,
    ammoLabel,
    getUserId,
}: GeneratorOptions<string>) => {
    const body1 = {
        Channel: 'organizationChat',
    };

    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
        userId: getUserId(),
        body: JSON.stringify(body1),
    });
};

export const devConsoleV1SkillsAliceGenerator = ({ method, route, ammoLabel, getUserId }: GeneratorOptions<string>) => {
    const body1 = {
        Channel: 'aliceSkill',
    };

    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
        userId: getUserId(),
        body: JSON.stringify(body1),
    });
};

export const devConsoleV1SkillsDeleteGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
    });
};

// /// Снапшот
export const devConsoleV1SnapshotGenerator = ({ method, route, ammoLabel, getUserId }: GeneratorOptions<string>) => {
    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
        userId: getUserId(),
    });
};

export const devConsoleV1SubscriptionGenerator = ({
    method,
    route,
    ammoLabel,
    getUserId,
}: GeneratorOptions<string>) => {
    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
        userId: getUserId(),
    });
};

export const devConsoleV1EmailSendGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<string>) => {
    const { skillId, userId } = getSkillMeta();

    const body1 = {
        email: 'nasdspb@yandex.ru',
        skillId,
        userId,
    };

    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
        userId,
        body: JSON.stringify(body1),
    });
};

export const devConsoleV1ModerationSkillSkillIdValidateTokenGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
    });
};

// export const devConsoleV1OauthAppsGenerator = ({
//      method,
//      route,
//      ammoLabel,
//      getSkillMeta,
// }: GeneratorOptions<RouteGenerator>) => {
//     const { skillId, userId } = getSkillMeta();
//
//     const body1 = {
//         id
//         name: required('name'),
//         clientId: required('clientId'),
//         clientSecret: id ? optional('clientSecret') : required('clientSecret'),
//         authorizationUrl: requiredUrl('authorizationUrl'),
//         tokenUrl: requiredUrl('tokenUrl'),
//         refreshTokenUrl: optional('refreshTokenUrl'),
//         scope: optional('scope'),
//         yandexClientId: optional('yandexClientId'),
//         userId,
//     };
//
//     return makeAmmoUnit({
//         method,
//         route: route(skillId),
//         ammoLabel,
//         userId,
//         body: JSON.stringify(body1),
//     });
// };

export const devConsoleV1OauthAppsAppIdGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
    });
};

export const decConsoleV1OrganizationsGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
        serviceTicket: true,
    });
};

// патроны для external
export const exterGetSkillGenerator = ({ method, route, ammoLabel, getSkillId }: GeneratorOptions<RouteGenerator>) => {
    return makeAmmoUnit({
        method,
        route: route(getSkillId()),
        ammoLabel,
        serviceTicket: true,
    });
};

export const externalV2SkillsBulkGetGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillId,
}: GeneratorOptions<string>) => {
    const body1 = {
        skillIds: new Array(10).fill(undefined).map(() => getSkillId()),
    };

    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
        body: JSON.stringify(body1),
        serviceTicket: true,
    });
};

export const externalV2ChatsBulkGetGenerator = ({ method, route, ammoLabel, getUserId }: GeneratorOptions<string>) => {
    const body = {
        userIds: new Array(10).fill(undefined).map(() => getUserId()),
    };

    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
        body,
        serviceTicket: true,
    });
};

export const externalV2ChatsGenerator = ({ method, route, ammoLabel, getUserId }: GeneratorOptions<string>) => {
    const userId = getUserId();
    const body = {
        settings: {
            organizationId: '1234',
        },
        logoUrl: 'https://via.placeholder.com/1',
        uid: userId,
    };

    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
        serviceTicket: true,
        body,
    });
};

export const externalV2ChatsSkillIdGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillId,
}: GeneratorOptions<RouteGenerator>) => {
    return makeAmmoUnit({
        method,
        route: route(getSkillId()),
        ammoLabel,
        serviceTicket: true,
    });
};

export const getChatDraftStatusGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillId,
}: GeneratorOptions<RouteGenerator>) => {
    return makeAmmoUnit({
        method,
        route: route(getSkillId()),
        ammoLabel,
        serviceTicket: true,
    });
};

// патроны для public
export const getStatusGenerator = ({ method, route, ammoLabel, getUserId }: GeneratorOptions<string>) => {
    return makeAmmoUnit({
        method,
        route,
        ammoLabel,
        userId: getUserId(),
    });
};

export const getSkillImagesGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
    });
};

export const getSkillImagesUploadGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
        filePath: '../../../testResources/testimage.jpg',
    });
};

export const getSkillSoundGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
    });
};

export const uploadSkillSoundGenerator = ({
    method,
    route,
    ammoLabel,
    getSkillMeta,
}: GeneratorOptions<RouteGenerator>) => {
    const { skillId, userId } = getSkillMeta();

    return makeAmmoUnit({
        method,
        route: route(skillId),
        ammoLabel,
        userId,
        filePath: '../../../testResources/testsound.mp3',
    });
};
