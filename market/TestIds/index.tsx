import React from 'react';

type Props = {
    testIds: number[];
    className?: string;
};

const TestIds = ({testIds, className}: Props): JSX.Element => (
    <div className={className}>
        <h3>Test IDs</h3>

        {testIds.map(testId => (
            <div key={testId}>{testId}</div>
        ))}
    </div>
);

export default TestIds;
