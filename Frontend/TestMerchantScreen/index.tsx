import React from 'react';

import PageProvider from './PageProvider';
import Component from './Component';

const TestMerchantComponent: React.FC = () => {
    return (
        <PageProvider>
            <Component />
        </PageProvider>
    );
};

export default TestMerchantComponent;
