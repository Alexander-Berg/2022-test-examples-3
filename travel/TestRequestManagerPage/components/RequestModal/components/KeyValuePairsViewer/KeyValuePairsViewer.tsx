import {FC, ReactNode, memo, useMemo} from 'react';

import Text from 'components/Text/Text';
import Flex from 'components/Flex/Flex';

import cx from './KeyValuePairsViewer.scss';

interface IKeyValuePairsProps {
    keyValuePairs: Record<string, ReactNode>;
    sortKeys?: boolean;
}

const KeyValuePairsViewer: FC<IKeyValuePairsProps> = props => {
    const {keyValuePairs, sortKeys = false} = props;

    const sortedKeys = useMemo(() => {
        const keys = Object.keys(keyValuePairs);

        return sortKeys ? keys.sort() : keys;
    }, [keyValuePairs, sortKeys]);

    return (
        <Flex className={cx('root')} flexDirection="column" between={1}>
            {sortedKeys.map(key => {
                return (
                    <div key={key}>
                        <Text weight="bold">{key}:</Text>{' '}
                        <Text>{keyValuePairs[key]}</Text>
                    </div>
                );
            })}
        </Flex>
    );
};

export default memo(KeyValuePairsViewer);
