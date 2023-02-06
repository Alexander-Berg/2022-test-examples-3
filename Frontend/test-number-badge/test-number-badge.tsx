import React, { FC } from 'react';

import block from 'client/utils/cn';

import { Props } from 'client/components/contest-submissions/test-number-badge/types';

import 'client/components/contest-submissions/test-number-badge/test-number-badge.css';

const b = block('test-number-badge');

const TestNumberBadge: FC<Props> = ({ children, className }) => {
    return <div className={b({}, [className])}>{children}</div>;
};

export default TestNumberBadge;
