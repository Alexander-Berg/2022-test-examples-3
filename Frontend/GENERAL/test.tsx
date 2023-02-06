import React from 'react';

import { HorizontalPaddingMix } from 'blocks/horizontal-wrapper';
import { ListItem } from 'blocks/list-item';
import { NavBar } from 'blocks/navbar';
import { PageLayout, PageLayoutHeader } from 'blocks/page-layout';
import { Stream } from 'blocks/stream';
import { ToggleSwitch } from 'blocks/toggle-switch';
import { TextInput } from 'ui-kit/text-input';

import type { WithHistoryControllerProps } from 'hoc/with-history-controller';
import { useHistoryController } from 'hooks/global-history-controller';
import { createUrl } from 'utils/createUrl';
import { parseStringParamFromQuery } from 'utils/parse-string-param-from-query';

export const StreamTest: React.FunctionComponent<WithHistoryControllerProps> = ({ location: { search, pathname } }: WithHistoryControllerProps) => {
    const { historyAdapter } = useHistoryController();
    const parsedStreamUrl = parseStringParamFromQuery(search, 'stream-url');
    const parsedHls = parseStringParamFromQuery(search, 'stream-hls') === 'true';
    const [streamUrl, setStreamUrl] = React.useState(parsedStreamUrl);
    const [hls, setHls] = React.useState(parsedHls);

    const replaceLocation = React.useCallback((patchedQueryParams) => {
        const defaultQueryParams = {
            'stream-url': streamUrl,
            'stream-hls': hls ? 'true' : undefined,
        };
        historyAdapter.replace(createUrl(`${pathname}`, { ...defaultQueryParams, ...patchedQueryParams }));
    }, [pathname, historyAdapter, streamUrl, hls]);

    const handleStreamUrlChange = React.useCallback(
        (url: string) => {
            replaceLocation({ 'stream-url': url });
            // подмена на лету ломает проигрыватель яндекса
            location.reload();
            setStreamUrl(url);
        },
        [setStreamUrl, replaceLocation]
    );
    const handleStreamHlsChange = React.useCallback(
        (value: boolean) => {
            replaceLocation({ 'stream-hls': value ? 'true' : undefined });
            setHls(value);
        },
        [setHls, replaceLocation]
    );

    return (
        <PageLayout>
            <PageLayoutHeader>
                <NavBar />
            </PageLayoutHeader>
            <div className={HorizontalPaddingMix.both}>
                <TextInput value={streamUrl ?? ''} onChange={handleStreamUrlChange} />
                <ListItem name="Источник hls" control={<ToggleSwitch active={hls} onChange={handleStreamHlsChange} />} />
            </div>
            {streamUrl && <Stream type="device" source={streamUrl} hls={hls} />}
        </PageLayout>
    );
};
