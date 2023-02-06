import {FC, memo, useCallback, useMemo, useRef, useState} from 'react';

import {IApiRequestInfo} from 'server/utilities/TestRequestManager/types/requestInfo';

import {deviceMods} from 'utilities/stylesUtils';
import {useDeviceType} from 'utilities/hooks/useDeviceType';
import useImmutableCallback from 'utilities/hooks/useImmutableCallback';
import {useUniversalLayoutEffect} from 'utilities/hooks/useUniversalLayoutEffect';

import Tabs, {ITabsProps} from 'components/Tabs/Tabs';
import RequestViewer from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestModal/components/RequestViewer/RequestViewer';
import ResponseViewer from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestModal/components/ResponseViewer/ResponseViewer';
import Flex from 'components/Flex/Flex';

import cx from './RequestModal.scss';

interface IRequestViewerProps {
    apiRequest: IApiRequestInfo;
}

enum ERequestViewerTab {
    REQUEST = 'REQUEST',
    RESPONSE = 'RESPONSE',
}

interface IRequestViewerTab {
    type: ERequestViewerTab;
    name: string;
}

const TABS: IRequestViewerTab[] = [
    {
        type: ERequestViewerTab.REQUEST,
        name: 'Запрос',
    },
    {
        type: ERequestViewerTab.RESPONSE,
        name: 'Ответ',
    },
];
const DEFAULT_TAB = ERequestViewerTab.REQUEST;

const getTabId = (tab: IRequestViewerTab): ERequestViewerTab => tab.type;

const RequestModal: FC<IRequestViewerProps> = props => {
    const {apiRequest} = props;

    const [selectedTab, setSelectedTab] =
        useState<ERequestViewerTab>(DEFAULT_TAB);
    const deviceType = useDeviceType();
    const contentRef = useRef<HTMLDivElement | null>(null);

    const content = useMemo(() => {
        if (selectedTab === ERequestViewerTab.REQUEST) {
            return <RequestViewer apiRequest={apiRequest} />;
        }

        if (selectedTab === ERequestViewerTab.RESPONSE) {
            return <ResponseViewer apiRequest={apiRequest} />;
        }

        return null;
    }, [apiRequest, selectedTab]);

    const handleChangeTab = useImmutableCallback(
        (_tab: IRequestViewerTab, tabId: ERequestViewerTab) => {
            setSelectedTab(tabId);
        },
    );

    const scrollToTop = useImmutableCallback(() => {
        if (contentRef.current) {
            contentRef.current.scrollTop = 0;
        }
    });

    const renderTab: ITabsProps<
        IRequestViewerTab,
        ERequestViewerTab
    >['renderTab'] = useCallback(({key, tab, tabProps}) => {
        return (
            <Tabs.Tab key={key} {...tabProps}>
                {tab.name}
            </Tabs.Tab>
        );
    }, []);

    useUniversalLayoutEffect(() => {
        scrollToTop();
    }, [scrollToTop, selectedTab]);

    useUniversalLayoutEffect(() => {
        setSelectedTab(DEFAULT_TAB);
        scrollToTop();
    }, [apiRequest.id, scrollToTop]);

    return (
        <Flex
            className={cx('root', deviceMods('root', deviceType))}
            flexDirection="column"
            between={4}
        >
            <Tabs
                activeTabId={selectedTab}
                tabs={TABS}
                renderTab={renderTab}
                getTabId={getTabId}
                onChange={handleChangeTab}
            />

            <div ref={contentRef} className={cx('content')}>
                {content}
            </div>
        </Flex>
    );
};

export default memo(RequestModal);
