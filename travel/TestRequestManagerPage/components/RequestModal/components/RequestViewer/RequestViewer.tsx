import {FC, memo, useMemo} from 'react';
import isEmpty from 'lodash/isEmpty';

import {ERequestType} from 'server/utilities/TestRequestManager/types/request';
import {ERequestSourceType} from 'server/utilities/TestRequestManager/types/requestSource';
import {IApiRequestInfo} from 'server/utilities/TestRequestManager/types/requestInfo';

import {getDisplayedMethod} from 'projects/testControlPanel/pages/TestRequestManagerPage/utilities/getDisplayedMethod';

import Flex from 'components/Flex/Flex';
import KeyValuePairsViewer from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestModal/components/KeyValuePairsViewer/KeyValuePairsViewer';
import JsonViewer from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestModal/components/JsonViewer/JsonViewer';
import ViewerSection from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestModal/components/ViewerSection/ViewerSection';

interface IRequestViewerProps {
    apiRequest: IApiRequestInfo;
}

const RequestViewer: FC<IRequestViewerProps> = props => {
    const {
        apiRequest: {
            request,
            request: {url, method, params, headers},
            source,
        },
    } = props;

    const generalInfo = useMemo(() => {
        return {
            URL: url,
            Метод: getDisplayedMethod(method),
        };
    }, [method, url]);

    const sourceInfo = useMemo(() => {
        return {
            'Источник запроса': source.type,
            Страница: source.pageUrl,
            ...(source.type === ERequestSourceType.BROWSER && {
                'Ручка на портале': source.requestUrl,
            }),
        };
    }, [source]);

    return (
        <Flex flexDirection="column" between={10}>
            <ViewerSection title="Основная информация">
                <KeyValuePairsViewer keyValuePairs={generalInfo} />
            </ViewerSection>

            <ViewerSection title="Источник">
                <KeyValuePairsViewer keyValuePairs={sourceInfo} />
            </ViewerSection>

            {!isEmpty(params) && (
                <ViewerSection title="Query-параметры">
                    <KeyValuePairsViewer keyValuePairs={params} sortKeys />
                </ViewerSection>
            )}

            {request.type === ERequestType.JSON && (
                <ViewerSection title="Тело запроса">
                    <JsonViewer json={request.data} />
                </ViewerSection>
            )}

            <ViewerSection title="Заголовки">
                <KeyValuePairsViewer keyValuePairs={headers} sortKeys />
            </ViewerSection>
        </Flex>
    );
};

export default memo(RequestViewer);
