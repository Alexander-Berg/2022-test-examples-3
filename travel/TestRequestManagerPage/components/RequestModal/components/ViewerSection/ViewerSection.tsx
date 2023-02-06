import {FC, ReactNode, memo} from 'react';

import Heading from 'components/Heading/Heading';
import Flex from 'components/Flex/Flex';

interface ISectionProps {
    title: string;
    children: ReactNode;
}

const ViewerSection: FC<ISectionProps> = props => {
    const {title, children} = props;

    return (
        <Flex flexDirection="column" between={2}>
            <Heading level={2}>{title}</Heading>

            <div>{children}</div>
        </Flex>
    );
};

export default memo(ViewerSection);
