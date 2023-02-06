import * as React from 'react';

import { Report } from '../../Report/Report';
import { IReportModel } from '../../../models/report';
import { InfoLineContainer } from '../../../containers/InfoLineContainer/InfoLineContainer';
import { Row } from '../../Row/Row';
import { ReportTasksContainer } from '../../../containers/ReportTasksContainer/ReportTasksContainer';

export interface ITestResultProps {
    report?: IReportModel | null;
    taskIds?: string[];
}

export const LessonContentResolverTestResult: React.FC<ITestResultProps> = props => {
    const { report, taskIds } = props;

    if (!report) return null;

    return (
        <>
            <Row gapBottom="m">
                <InfoLineContainer />
            </Row>
            <Report data={report} variantId={-1} hideStat />
            {Array.isArray(taskIds) && (
                <ReportTasksContainer ids={taskIds} />
            )}
        </>
    );
};
