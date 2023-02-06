import * as React from 'react';
import { DecoratorFunction } from '@storybook/client-api';
import { StoryFnReactReturnType } from '@storybook/react/dist/ts3.9/client/preview/types';

import { CacheRequest, CacheRequestContext } from 'shared/hooks/useCacheRequestContext/useCacheRequestContext';

function onCacheChange(name: string, payload: Record<string, any>) {
    if (name) {
        // save cache here
        fetch(name, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(payload),
        });
    }
}

export const withRequestCache: DecoratorFunction<StoryFnReactReturnType> = (story, context) => {
    const { cacheData, cacheName = '' } = context.loaded || {};

    return (
        <CacheRequest.Provider value={new CacheRequestContext(cacheName, cacheData, onCacheChange)}>
            {story()}
        </CacheRequest.Provider>
    );
};
