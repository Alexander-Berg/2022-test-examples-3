import {FC, memo} from 'react';

import {
    EResponseType,
    TResponse,
} from 'server/utilities/TestRequestManager/types/response';

import Text from 'components/Text/Text';
import JsonViewer from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestModal/components/JsonViewer/JsonViewer';

interface IResponseBodyProps {
    response: TResponse | null;
}

const ResponseBody: FC<IResponseBodyProps> = props => {
    const {response} = props;

    if (!response) {
        return <Text>Отсутствует</Text>;
    }

    if (response.type === EResponseType.TEXT) {
        return <Text>{response.data}</Text>;
    }

    if (response.type === EResponseType.JSON) {
        return <JsonViewer json={response.data} />;
    }

    return <Text>Не поддерживается</Text>;
};

export default memo(ResponseBody);
