import React, { FC } from 'react';
import { cn } from '@bem-react/classname';

import './Skeleton.css';

const cnSkeleton = cn('HermioneSkeleton');

export interface SkeletonProps {
    lines?: number;
}

export const Skeleton: FC<SkeletonProps> = ({ lines = 10 }) => {
    const content = [];

    for (let i = 0; i < lines; i += 1) {
        content.push(<div key={i} className={cnSkeleton('Text')} />);
    }

    return <div className={cnSkeleton()}>{content}</div>;
};
