import { LoaderFunction } from '@storybook/addons/dist/ts3.9/types';

export const withRequestCacheLoader: LoaderFunction = async (context) => {
    const { fileName, cacheName } = context.parameters;

    const parts = fileName.split('/');
    parts.shift(); // .
    parts.shift(); // src
    parts.pop(); // *.story.tsx
    parts.push('__fixtures__', parts[parts.length - 1] + (cacheName ? '-' + cacheName : '') + '.json');
    parts.unshift('__cache__');

    const fixtureCacheName = parts.join('/');

    try {
        const res = await fetch(fixtureCacheName, {
            method: 'GET',
        });

        if (res.ok) {
            const cacheData = await res.json();

            // normal cache
            return {
                cacheName: fixtureCacheName,
                cacheData,
            };
        }
    } catch (error) {
        // noop
    }

    // empty cache
    return {
        cacheName: fixtureCacheName,
        cacheData: {},
    };
};
