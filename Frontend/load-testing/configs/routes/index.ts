/* eslint-disable */
const trimLeadingSlash = (str: string) => str.replace(/^\/+/, '');

const skillRouteConstructor = (prefix: string) => {
    const prefixedRouteBuilder = (route: string) => (skillId: string) =>
        `${prefix}/skills/${skillId}/${trimLeadingSlash(route)}`;

    return {
        'dev-console/v1/skills/:skillId/draft': prefixedRouteBuilder('draft'),
        'dev-console/v1/skills/:skillId/draft/actions/withdraw': prefixedRouteBuilder('draft/actions/withdraw'),
        'dev-console/v1/skills/:skillId/flags': prefixedRouteBuilder('flags'),
        'dev-console/v1/skills/notifications/options': prefixedRouteBuilder('notifications/options'),
        'dev-console/v1/skills/notifications/phone': prefixedRouteBuilder('notifications/phone'),
        'dev-console/v1/skills/notifications/phone/confirmation': prefixedRouteBuilder(
            'notifications/phone/confirmation',
        ),
        'dev-console/v1/skills/candidate': prefixedRouteBuilder('candidate'),
        'dev-console/v1/skills/release': prefixedRouteBuilder('release'),
        'dev-console/v1/skills/message': prefixedRouteBuilder('message'),
        'dev-console/v1/skills/operations': prefixedRouteBuilder('operations'),
        'dev-console/v1/skills/reviews': prefixedRouteBuilder('reviews'),
        'dev-console/v1/skills/logo': prefixedRouteBuilder('logo'),
        'dev-console/v1/skills/actions/stop': prefixedRouteBuilder('actions/stop'),
    };
};

const makeConsoleRoutes = () => {
    const prefix = '/api/dev-console/v1';
    const prefixed = (route: string) => `${prefix}/${trimLeadingSlash(route)}`;

    return {
        'dev-console/v1/skills': prefixed('skills'),
        'dev-console/v1/skills/:skillId': (skillId: string) => prefixed(`skills/${skillId}`),
        'dev-console/v1/snapshot': prefixed('snapshot'),
        'dev-console/v1/organizations': prefixed('organizations'),
        'dev-console/v1/organizations/:orgId': (orgId: string) => prefixed(`organizations/${orgId}`),
        'dev-console/v1/subscription': prefixed('subscription'),
        'dev-console/v1/email': prefixed('email'),
        'dev-console/v1/email/send': prefixed('email/send'),
        'dev-console/v1/moderation/skills/:skillId/validate-token': (skillId: string) =>
            prefixed(`moderation/skills/${skillId}/validate-token`),
        'dev-console/v1/oauth/apps': prefixed('oauth/apps'),
        'dev-console/v1/oauth/apps/:appId': (appId: string) => prefixed(`oauth/apps/${appId}`),
        ...skillRouteConstructor(prefix),
    };
};

const makeCatalogueRoutes = () => {
    const prefixed = (route: string) => `/api/catalogue/v1/${trimLeadingSlash(route)}`;

    return {
        'catalogue/v1/categories': prefixed('categories'),
        'catalogue/v1/dialogs/:slug': (slug: string) => prefixed(`dialogs/${slug}`),
        'catalogue/v1/dialogs/search': prefixed('dialogs/search'),
        'catalogue/v1/dialogs/suggest': prefixed('dialogs/suggest'),
        'catalogue/v1/dialogs2': prefixed('dialogs2'),
        'catalogue/v1/dialogs/:skillId/slug': (skillId: string) => prefixed(`dialogs/${skillId}/slug`),
        'catalogue/v1/dialogs/:skillId/account_linking': (skillId: string) =>
            prefixed(`dialogs/${skillId}/account_linking`),
        'catalogue/v1/collections': prefixed('collections'),
        'catalogue/v1/builtin-collections': prefixed('builtin-collections'),
        'catalogue/v1/reviewform': prefixed('reviewform'),
        'catalogue/v1/experiments/:yandexuid': (yandexuid: string) => prefixed(`experiments/${yandexuid}`),
        'catalogue/v1/compilations/:slug': (slug: string) => prefixed(`compilations/${slug}`),
        'catalogue/v1/pages/main': prefixed('pages/main'),
        'catalogue/v1/smart_home/get_native_skills': prefixed('smart_home/get_native_skills'),
    };
};

const makeExternalRoutesV1 = () => {
    return {
        'external/v1': '/api/external/v1',
    };
};

const makeExternalRoutesV2 = () => {
    const prefixed = (route: string) => `/api/external/v2/${trimLeadingSlash(route)}`;

    return {
        'external/v2/skills/:skillId': (skillId: string) => prefixed(`skills/${skillId}`),
        'external/v2/chats/bulk/get': prefixed('/chats/bulk/get'),
        'external/v2/skills/bulk/get': prefixed('/skills/bulk/get'),
        'external/v2/chats': prefixed('chats'),
        'external/v2/chats/:skillId': (skillId: string) => prefixed(`chats/${skillId}`),
        'external/v2/chats/:skillId/draft': (skillId: string) => prefixed(`/chats/${skillId}/draft`),
        'external/v2/chats/:skillId/draft/status': (skillId: string) => prefixed(`/chats/${skillId}/draft/status`),
        'external/v2/chats/:skillId/draft/actions/deploy': (skillId: string) =>
            prefixed(`/chats/${skillId}/draft/actions/deploy`),
    };
};

const makePublicRoutes = () => {
    const prefixed = (route: string) => `/api/public/v1/${trimLeadingSlash(route)}`;

    return {
        'public/v1/status': prefixed('status'),
        'public/v1/skills/:skillId/images': (skillId: string) => prefixed(`skills/${skillId}/images`),
        'public/v1/skills/:skillId/images/:groupId/:imageName': (skillId: string, groupId: string, imageName: string) =>
            prefixed(`/skills/${skillId}/images/${groupId}/${imageName}`),
        'public/v1/skills/:skillId/sounds': (skillId: string) => prefixed(`skills/${skillId}/sounds`),
        'public/v1/skills/:skillId/sounds/:soundId': (skillId: string, soundId: string) =>
            prefixed(`skills/${skillId}/sounds/${soundId}`),
    };
};

const routesConfig = {
    ...makeConsoleRoutes(),
    ...makeCatalogueRoutes(),
    ...makePublicRoutes(),
    ...makeExternalRoutesV1(),
    ...makeExternalRoutesV2(),
};

export default routesConfig;

export type RoutesStructure<V> = { [K in keyof typeof routesConfig]: V };
export type PartialRoutesRecord<V> = Partial<RoutesStructure<V>>;
