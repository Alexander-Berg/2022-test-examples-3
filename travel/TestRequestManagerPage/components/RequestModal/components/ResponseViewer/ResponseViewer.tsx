import {FC, memo, useMemo} from 'react';

import {IApiRequestInfo} from 'server/utilities/TestRequestManager/types/requestInfo';

import ViewerSection from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestModal/components/ViewerSection/ViewerSection';
import KeyValuePairsViewer from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestModal/components/KeyValuePairsViewer/KeyValuePairsViewer';
import Flex from 'components/Flex/Flex';
import RequestDuration from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestDuration/RequestDuration';
import ResponseBody from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestModal/components/ResponseViewer/components/ResponseBody/ResponseBody';

interface IResponseViewerProps {
    apiRequest: IApiRequestInfo;
}

const ResponseViewer: FC<IResponseViewerProps> = props => {
    const {
        apiRequest: {response, startTime, endTime, localStartTime},
    } = props;

    const generalInfo = useMemo(() => {
        return {
            Статус: response?.status ?? '-',
            Длительность: (
                <RequestDuration
                    startTime={startTime}
                    endTime={endTime}
                    localStartTime={localStartTime}
                />
            ),
        };
    }, [endTime, localStartTime, response?.status, startTime]);

    return (
        <Flex flexDirection="column" between={10}>
            <ViewerSection title="Основная информация">
                <KeyValuePairsViewer keyValuePairs={generalInfo} />
            </ViewerSection>

            <ViewerSection title="Тело ответа">
                <ResponseBody response={response} />
            </ViewerSection>

            <ViewerSection title="Заголовки">
                <KeyValuePairsViewer
                    keyValuePairs={response?.headers ?? {}}
                    sortKeys
                />
            </ViewerSection>
        </Flex>
    );
};

export default memo(ResponseViewer);
