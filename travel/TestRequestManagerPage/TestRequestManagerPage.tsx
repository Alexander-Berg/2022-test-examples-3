import {FC, memo} from 'react';

import RequestManager from 'projects/testControlPanel/pages/TestRequestManagerPage/components/RequestManager/RequestManager';

const TestRequestManagerPage: FC = () => {
    return <RequestManager filterByPage={false} />;
};

export default memo(TestRequestManagerPage);
