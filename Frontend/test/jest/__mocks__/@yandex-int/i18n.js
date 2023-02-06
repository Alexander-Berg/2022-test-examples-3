import React from 'react';

export const i18n = () => key => `i18n:${key}`;
export const i18nRaw = () => (i18nKey, values) => (
    <React.Fragment>
        {`i18nRaw:${i18nKey}`}
        {
            values && Object.entries(values)
                .sort(([a], [b]) => a.localeCompare(b))
                .map(([key, value]) => (
                    <React.Fragment key={key}>
                        {value}
                    </React.Fragment>
                ))
        }
    </React.Fragment>
);
export default i18n;
