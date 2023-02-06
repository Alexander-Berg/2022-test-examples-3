import React from 'react';

import {
    prepareQaAttributes,
    IWithQaAttributes,
} from 'utilities/qaAttributes/qaAttributes';

import cx from './TestAnchors.scss';

interface ITestAnchorsProps extends IWithQaAttributes {}

const TestAnchors: React.FC<ITestAnchorsProps> = props => {
    return (
        <>
            <div
                className={cx('anchor', 'anchor_top')}
                {...prepareQaAttributes({parent: props, current: 'anchor-top'})}
            />
            <div
                className={cx('anchor', 'anchor_left')}
                {...prepareQaAttributes({
                    parent: props,
                    current: 'anchor-left',
                })}
            />
            <div
                className={cx('anchor', 'anchor_bottom')}
                {...prepareQaAttributes({
                    parent: props,
                    current: 'anchor-bottom',
                })}
            />
            <div
                className={cx('anchor', 'anchor_right')}
                {...prepareQaAttributes({
                    parent: props,
                    current: 'anchor-right',
                })}
            />
        </>
    );
};

export default TestAnchors;
