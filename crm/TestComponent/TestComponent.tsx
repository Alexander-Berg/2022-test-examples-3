import React, { FC } from 'react';
import { TestComponentProps } from './TestComponent.types';

export const TestComponent: FC<TestComponentProps> = ({ data }) => <span>{data}</span>;
