import React from 'react';

import Component from './Component';
import PageProvider from './PageProvider';

const EventManagerScreen: React.FC = () => {
    return (
        <PageProvider>
            <Component />
        </PageProvider>
    );
};

export default EventManagerScreen;
