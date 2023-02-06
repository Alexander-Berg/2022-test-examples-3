import { storiesOf } from '@storybook/react';
import React from 'react';

import Tests from '../tests';

const stubAction = () => ({});

storiesOf('Edit', module)
    .add('plain', () => (
        <Tests
            exams={[
                { id: 1, slug: 'direct', title: 'Сертификация специалистов по Яндекс.Директу' },
                { id: 44, slug: 'direct-pro', title: 'Сертификация с прокторингом' },
                { id: 38, slug: 'direct-en', title: 'Yandex.Direct certification' }
            ]}
            api=""
            lockedExams={{
                direct: {
                    login: 'dotokoto',
                    lockDate: new Date()
                },
                metrika: {
                    login: 'anyok',
                    lockDate: new Date()
                }
            }}
            showTooltip={stubAction}
            hideTooltip={stubAction}
            visibleLockName={null}
            />
    ))
    .add('popup opened', () => (
        <Tests
            exams={[
                { id: 1, slug: 'direct', title: 'Сертификация специалистов по Яндекс.Директу' },
                { id: 44, slug: 'direct-pro', title: 'Сертификация с прокторингом' },
                { id: 38, slug: 'direct-en', title: 'Yandex.Direct certification' }
            ]}
            api=""
            lockedExams={{
                direct: {
                    login: 'dotokoto',
                    lockDate: new Date()
                },
                metrika: {
                    login: 'anyok',
                    lockDate: new Date()
                }
            }}
            showTooltip={stubAction}
            hideTooltip={stubAction}
            visibleLockName="direct"
            />
    ));
