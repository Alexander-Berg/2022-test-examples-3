import React, { useEffect, useCallback, useState, useContext } from 'react';
import block from 'propmods';
import * as keyset from 'i18n/DraftControlPanel';
import i18n from '@yandex-int/i18n';
import { devicesTestingApi } from '../../../api';
import { useFetchingHandler } from '../../../hooks/useFetchingHandler';
import { useCurrentSkill } from '../../../hooks/useCurrentSkill';
import { useInterval } from '../../../hooks/useInterval';
import { Loading } from '../../Loading/Loading';
import { FormError } from '../../Form/FormError';
import {
    TestingDateStatus,
    DevicesTestingStatusMeta,
    initialDevicesTestingOptions,
    parseDevicesTestingOptions,
} from '../../../model/devicesTestingForm';
import { TestingDateSelectionForm } from '../TestingDateSelectionForm/TestingDateSelectionForm';
import { TestingDateSelectionChanger } from '../TestingDateSelectionChanger/TestingDateSelectionChanger';
import Caption from '../../Form/Caption';
import { formatDevicesTestingRange } from '../../../utils/time';
import { onlineDevicesTestingNoteText } from '../text';
import { Context as SnapshotContext } from '../../../context/snapshotContext';
import { completedModerationStateTexts } from '../../../locale/ru/draftStatus';

import './TestingDateSelectionSection.scss';

const b = block('TestingDateSelectionSection');
const t = i18n(keyset);

export const TestingDateSelectionSection: React.FC = () => {
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    const skill = useCurrentSkill()!;
    const { refreshSnapshot } = useContext(SnapshotContext);
    const [devicesTestingStatusMeta, setDevicesTestingStatusMeta] = useState<DevicesTestingStatusMeta>();

    const handler = useCallback(() => devicesTestingApi.getDevicesTestingRecordStatus(skill.id), [skill.id]);

    const [loadStatusHandler, state] = useFetchingHandler({ handler });

    const loadStatus = useCallback(async() => {
        setDevicesTestingStatusMeta(await loadStatusHandler());
    }, [loadStatusHandler]);

    const loadStatusUnhandled = useCallback(async() => {
        setDevicesTestingStatusMeta(await handler());
    }, [handler]);

    useEffect(() => {
        void loadStatus();
    }, []);
    useInterval(loadStatusUnhandled, 1000 * 60);

    return (
        <div {...b()}>
            {(() => {
                switch (state) {
                    case 'loading':
                        return <Loading size="m" />;
                    case 'error':
                        return <FormError>{t('status-error')}</FormError>;
                    case 'idle':
                        if (!devicesTestingStatusMeta) {
                            return null;
                        }

                        const data = devicesTestingStatusMeta;

                        switch (data.status) {
                            case TestingDateStatus.NOT_SPECIFIED:
                                return (
                                    <TestingDateSelectionForm
                                        initialSettings={initialDevicesTestingOptions}
                                        afterSave={loadStatusUnhandled}
                                    />
                                );
                            case TestingDateStatus.AWAITING:
                                return (
                                    <TestingDateSelectionChanger
                                        beforeButtonElement={
                                            <div {...b('text', { theme: 'default' })}>{t('meeting-wait-approval')}</div>
                                        }
                                        formSettings={parseDevicesTestingOptions(data.record)}
                                        afterButtonElement={<Caption>{onlineDevicesTestingNoteText}</Caption>}
                                        afterFormSave={loadStatusUnhandled}
                                    />
                                );
                            case TestingDateStatus.REJECTED:
                                return (
                                    <TestingDateSelectionChanger
                                        beforeButtonElement={
                                            <div {...b('text', { theme: 'error' })}>
                                                {t('meeting-not-agreed')}
                                            </div>
                                        }
                                        formSettings={parseDevicesTestingOptions(data.record)}
                                        afterFormSave={loadStatusUnhandled}
                                    />
                                );
                            case TestingDateStatus.APPROVED:
                                const formSettings = parseDevicesTestingOptions(data.record);

                                const approvedText = formSettings.selectedDateRange ?
                                    `${t('meeting-approved-on')}\xa0${formatDevicesTestingRange(
                                        formSettings.selectedDateRange,
                                        true,
                                    )}` :
                                    t('meeting-approved');

                                return (
                                    <TestingDateSelectionChanger
                                        beforeButtonElement={
                                            <div {...b('text', { theme: 'success' })}>{approvedText}</div>
                                        }
                                        formSettings={parseDevicesTestingOptions(data.record)}
                                        afterFormSave={refreshSnapshot}
                                    />
                                );
                            case TestingDateStatus.PASSED:
                                return (
                                    <div {...b('text', { theme: 'success' })}>
                                        {
                                            // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                            (completedModerationStateTexts as any)[skill.draft.channel][
                                                skill.draft.status
                                            ]
                                        }
                                    </div>
                                );

                            default:
                                return null;
                        }
                }
            })()}
        </div>
    );
};
