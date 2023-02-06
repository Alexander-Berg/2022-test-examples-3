import block from 'propmods';
import React from 'react';
import * as keyset from 'i18n/TestPage';
import i18n from '@yandex-int/i18n';
import { AliceSkill } from '../../model/skill';
import Switch from '../Switch/Switch';
import { TestChat } from '../TestChat/TestChat';
import { PageHeadline } from '../Typo/Typo';
import { getSettingsEntity } from '../../utils';

import './TestPage.scss';

const b = block('TestPage');
const t = i18n(keyset);

export interface Props {
    isDraft: boolean;
    skill: AliceSkill;
    onTypeChange: (isDraft: boolean) => void;
}

export default class TestPage extends React.Component<Props> {
    public handleTypeChange = (version: string) => {
        if (this.props.onTypeChange) {
            this.props.onTypeChange(version === 'draft');
        }
    };

    public render() {
        const { isDraft, skill } = this.props;
        const testEntity = getSettingsEntity(skill, isDraft);

        return (
            <div {...b(this.props)}>
                <PageHeadline
                    title={t('header-test-page')}
                    subtitle={
                        <Switch
                            items={[
                                { value: 'draft', text: t('menu-draft') },
                                {
                                    value: 'production',
                                    text: t('menu-published'),
                                    title: skill.onAir ? void 0 : t('error-no-published'),
                                    disabled: !skill.onAir,
                                },
                            ]}
                            value={isDraft ? 'draft' : 'production'}
                            onSwitch={this.handleTypeChange}
                        />
                    }
                />

                {testEntity.backendSettings.uri || testEntity.backendSettings.functionId ? (
                    <>
                        <div {...b('intro')}>{t('alert-check-functional')}</div>
                        <div {...b('chat')}>
                            <TestChat skillId={skill.id} isDraft={isDraft} key={isDraft ? 'DRAFT_CHAT' : 'CHAT'} />
                        </div>
                    </>
                ) : (
                    <div>{t('error-need-valid-webhook')}</div>
                )}
            </div>
        );
    }
}
