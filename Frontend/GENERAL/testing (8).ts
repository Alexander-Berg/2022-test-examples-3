export const testingPolicies = () => {
    const hosts = [
        'https://frontend-internal.s3.mds.yandex.net',
        'https://frontend-test.s3.mds.yandex.net',
        '*.hamster.yandex.ru:*',
    ];

    return {
        'script-src': hosts,
        'style-src': hosts,
        'font-src': hosts,
        'connect-src': hosts,
        'img-src': hosts,
        'manifest-src': hosts,
        'media-src': hosts,
        'worker-src': hosts,
    };
};
