import {FC, memo} from 'react';

import {TJsonEntity} from 'server/utilities/TestRequestManager/types/json';

import cx from './JsonViewer.scss';

interface IJsonViewerProps {
    json: TJsonEntity;
}

const JsonViewer: FC<IJsonViewerProps> = props => {
    const {json} = props;

    return <div className={cx('root')}>{JSON.stringify(json, null, 4)}</div>;
};

export default memo(JsonViewer);
