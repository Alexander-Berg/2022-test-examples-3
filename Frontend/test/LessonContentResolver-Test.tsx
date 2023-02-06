import * as React from 'react';
import { useEffect } from 'react';
import { Row } from '../../Row/Row';
import { TaskContainer } from '../../../containers/TaskContainer/TaskContainer';
import { restoreTestTabValues } from '../../../actions/lessonPage';
import { cnLessonContent } from '../LessonContentResolver';

export interface IResolverTestProps {
    taskIds?: string[];
    restoreTestValues: typeof restoreTestTabValues.request;
}

export const LessonContentResolverTest: React.FC<IResolverTestProps> = props => {
    const { taskIds, restoreTestValues } = props;

    useEffect(() => void restoreTestValues(), [restoreTestValues]);

    if (!taskIds || !taskIds.length) return null;

    return (
        <>
            {taskIds.map((id, index, self) => {
                return (
                    <Row
                        className={cnLessonContent('TestRow')}
                        gapBottom={index === self.length - 1 ? 'none' : 'l'}
                        key={id}
                    >
                        <TaskContainer id={id} />
                    </Row>
                );
            })}
        </>
    );
};
