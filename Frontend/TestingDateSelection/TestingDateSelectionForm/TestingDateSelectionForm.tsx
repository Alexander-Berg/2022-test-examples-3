/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/no-non-null-assertion */
// eslint-disable-next-line @typescript-eslint/no-use-before-define
import React, { useState, useRef, useCallback } from 'react';
import block from 'propmods';
import { RadioButton, Button, TextInput, Popup, Icon, Select } from 'lego-on-react';
import * as keyset from 'i18n/DraftControlPanel';
import i18n from '@yandex-int/i18n';
import { useFormSettings } from '../../../hooks/useFormSettings';
import {
    DevicesTestingType,
    serializeDevicesTestingOptions,
    Range,
    DevicesTestingFormSettings,
} from '../../../model/devicesTestingForm';
import { useFetchingHandler } from '../../../hooks/useFetchingHandler';
import { devicesTestingApi } from '../../../api';
import { FormCheckbox } from '../../Form/Form';
import { useCurrentSkill } from '../../../hooks/useCurrentSkill';
import TextArea from '../../Form/TextArea';
import { generateTestingHours, formatDevicesTestingRange } from '../../../utils/time';
import { moment } from '../../../lib/moment';
import { Note } from '../../Note/Note';
import { onlineDevicesTestingNoteText } from '../text';
import { ActionButton } from '../../ActionButton/ActionButton';
import { getBaseTooltipReducer } from '../../../reducers';

import './TestingDateSelectionForm.scss';

const b = block('TestingDateSelectionForm');
const t = i18n(keyset);

interface Props {
    afterSave?: () => Promise<void>;
    onCancel?: () => void;
    initialSettings: DevicesTestingFormSettings;
}

interface TestingDateSelectProps {
    onSelect: (_range: Range) => void;
    range?: Range;
}

const generateInputName = (name: string) => {
    return `paskills-testing_date-${name}`;
};

const TestingDateSelect: React.FC<TestingDateSelectProps> = ({ onSelect, range }) => {
    const containerRef = useRef<HTMLDivElement>(null);

    const [popupIsVisible, setPopupIsVisible] = useState(false);

    const [testingDateRanges, testingDateOrder] = generateTestingHours(moment());

    return (
        <>
            <div {...b('date')} ref={containerRef}>
                <TextInput
                    name={generateInputName('date')}
                    theme="normal"
                    size="s"
                    iconRight={<Icon type="calendar" size="s" style={{ width: '28px' }} />}
                    placeholder={`* ${t('date-time')}`}
                    text={range ? formatDevicesTestingRange(range, true) : ''}
                    onFocus={() => setPopupIsVisible(true)}
                    autocomplete={false}
                />
            </div>
            <Popup
                autoclosable
                theme="normal"
                target="anchor"
                visible={popupIsVisible}
                anchor={() => containerRef.current}
                onOutsideClick={() => setPopupIsVisible(false)}
                directions={['bottom-right']}
            >
                <div {...b('date-options')}>
                    {testingDateOrder.map((formattedDate, i) => {
                        const ranges = testingDateRanges[formattedDate];

                        return (
                            <div {...b('date-options-group')} key={i}>
                                <div {...b('date-options-title')}>{formattedDate}</div>
                                {ranges.map((r, j) => {
                                    const onClick = () => {
                                        onSelect(r);
                                        setPopupIsVisible(false);
                                    };

                                    return (
                                        <div {...b('date-option')} key={j} onClick={onClick}>
                                            {formatDevicesTestingRange(r)}
                                        </div>
                                    );
                                })}
                            </div>
                        );
                    })}
                </div>
            </Popup>
        </>
    );
};

const LANG_SELECT_OPTIONS = [
    { val: 'ru', text: t('rus') },
    { val: 'en', text: t('eng') },
];

export const TestingDateSelectionForm: React.FC<Props> = ({ afterSave, onCancel, initialSettings }) => {
    const skill = useCurrentSkill()!;
    const [isChanged, setIsChanges] = useState(false);
    const { settings, onChange } = useFormSettings({
        defaultSettings: initialSettings,
        registerChanges: setIsChanges,
    });

    const [isDevicesTransferConfirmed, setIsDevicesTransferConfirmed] = useState(false);

    const isFormSettingsSet = settings.login && settings.password && settings.selectedDateRange && settings.skypeLogin;

    const isSavingAllowed =
        ((settings.type === 'online' && isFormSettingsSet) ||
            (settings.type === 'offline' && isDevicesTransferConfirmed)) &&
        isChanged;

    const [onSave, savingState] = useFetchingHandler({
        handler: async() => {
            const res = await devicesTestingApi.saveDevicesTestingOptions(
                skill.id,
                serializeDevicesTestingOptions(settings),
            );
            await afterSave?.();

            return res;
        },
    });

    const isSaving = savingState === 'loading';

    const onLangChange = useCallback(([lang]: string[]) => onChange('lang')(lang), [onChange]);

    return (
        <div {...b()}>
            <div {...b('type')}>
                <RadioButton
                    name="type"
                    theme="normal"
                    size="s"
                    view="default"
                    value={settings.type}
                    onChange={e => onChange('type')(e.target.value as DevicesTestingType)}
                >
                    <RadioButton.Radio value="online">{t('meeting-skype')}</RadioButton.Radio>
                    <RadioButton.Radio value="offline">{t('meeting-office')}</RadioButton.Radio>
                </RadioButton>
            </div>
            {settings.type === 'online' ? (
                <div {...b('fields')}>
                    <div {...b('field')}>
                        <TestingDateSelect
                            range={settings.selectedDateRange}
                            onSelect={onChange('selectedDateRange')}
                        />
                    </div>
                    <div {...b('field')}>
                        <TextInput
                            name={generateInputName('provider_login')}
                            theme="normal"
                            size="s"
                            text={settings.login}
                            onChange={onChange('login')}
                            autocomplete={false}
                            placeholder={`* ${t('meeting-provider-login')}`}
                        />
                    </div>
                    <div {...b('field')}>
                        {/* Обертка в виде формы нужна, чтобы предотвратить автоподстановку доменных паролей*/}
                        <form onSubmit={e => e.preventDefault()} autoComplete="off">
                            <TextInput
                                name={generateInputName('provider_password')}
                                theme="normal"
                                size="s"
                                text={settings.password}
                                type="password"
                                onChange={onChange('password')}
                                autocomplete={false}
                                placeholder={`* ${t('meeting-provider-password')}`}
                            />
                        </form>
                    </div>
                    <div {...b('field')}>
                        <TextInput
                            name={generateInputName('skype_login')}
                            theme="normal"
                            size="s"
                            text={settings.skypeLogin}
                            onChange={onChange('skypeLogin')}
                            placeholder={`* ${t('meeting-skype-login')}`}
                        />
                    </div>
                    <div {...b('field')}>
                        <TextArea
                            name={generateInputName('comment')}
                            text={settings.comment!}
                            rows={2}
                            onChange={onChange('comment')}
                            autosized={false}
                            placeholder={t('comment')}
                        />
                    </div>
                    <div {...b('field', { lang: true })}>
                        <Select
                            theme="normal"
                            size="s"
                            name={generateInputName('lang')}
                            val={settings.lang}
                            onChange={onLangChange as any}
                            type="radio"
                            placeholder={t('lang')}
                            items={LANG_SELECT_OPTIONS}
                        />
                        <span {...b('clarification')}>
                            {t('meeting-choose-lang')}
                        </span>
                    </div>
                    <div {...b('field')}>
                        <Note type="info" hideLabel>
                            <div {...b('note')}>{onlineDevicesTestingNoteText}</div>
                        </Note>
                    </div>
                </div>
            ) : (
                <div {...b('checkbox')}>
                    <FormCheckbox
                        checked={isDevicesTransferConfirmed}
                        onChange={() => setIsDevicesTransferConfirmed(!isDevicesTransferConfirmed)}
                        multiline
                        centered={false}
                    >
                        {t('meeting-devices-sent')}
                    </FormCheckbox>
                </div>
            )}
            <div {...b('buttons')}>
                <div {...b('button')}>
                    <ActionButton
                        tooltipReducer={getBaseTooltipReducer({ successLabel: '' })}
                        action={async() => {
                            await onSave();
                            return 'clear';
                        }}
                        defaultTooltipState={{ tooltipVisible: false }}
                        disabled={!isSavingAllowed}
                    >
                        {t('save')}
                    </ActionButton>
                </div>
                {onCancel && (
                    <div {...b('button')}>
                        <Button theme="normal" size="s" onClick={onCancel} disabled={isSaving}>
                            {t('cancel')}
                        </Button>
                    </div>
                )}
            </div>
        </div>
    );
};
