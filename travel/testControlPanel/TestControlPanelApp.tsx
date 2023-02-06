import React from 'react';
import {Route, Switch, Redirect} from 'react-router-dom';

import {EProjectName} from 'constants/common';
import {URLs} from 'constants/urls';

import {EFooterProject} from 'components/Footer/types';

import LayoutDefault from 'components/Layouts/LayoutDefault/LayoutDefault';
import Container from 'components/Layouts/Container/Container';
import Box from 'components/Box/Box';
import NotFoundContent from 'components/NotFound/NotFoundContent/NotFoundContent';
import Flex from 'components/Flex/Flex';
import NavLink from 'projects/testControlPanel/components/NavLink/NavLink';

import TestContextAviaPage from 'projects/testControlPanel/pages/TestContextAviaPage/TestContextAviaPage';
import TestContextPaymentPage from 'projects/testControlPanel/pages/TestContextPaymentPage/TestContextPaymentPage';
import ExperimentsPage from 'projects/testControlPanel/pages/ExperimentsPage/ExperimentsPage';
import TrainsPage from 'projects/testControlPanel/pages/TrainsPage/TrainsPage';
import Test3DSPage from 'projects/testControlPanel/pages/Test3DSPage/Test3DSPage';
import MarkupViewer from 'projects/journal/pages/MarkupViewer/MarkupViewer';
import BusesPage from 'projects/testControlPanel/pages/BusesPage/BusesPage';

import TestContextHotelsPage from './pages/TestContextHotelsPage/TestContextHotelsPage';
import TestTripsPage from './pages/TestTripsPage/TestTripsPage';
import TestTravelAppPage from './pages/TestTravelAppPage/TestTravelAppPage';
import TestRequestManagerPage from './pages/TestRequestManagerPage/TestRequestManagerPage';

const TestControlPanelApp: React.FC = () => {
    return (
        <LayoutDefault
            isFixedNavigation
            showSearchForm
            showNavigation
            hasSideSheetNavigation
            project={EProjectName.TEST_CONTROL_PANEL}
            footerType={EFooterProject.AVIA}
            filters={undefined}
        >
            <Container>
                <Flex inline between={3} above={8}>
                    <NavLink to={URLs.testControlPanelExperiments} exact>
                        Экспы
                    </NavLink>
                    <NavLink to={URLs.testControlPanelPaymentTestContext} exact>
                        ТК Платеж
                    </NavLink>
                    <NavLink to={URLs.testControlPanelAviaTestContext} exact>
                        ТК Авиа
                    </NavLink>
                    <NavLink to={URLs.testControlPanelHotelsTestContext} exact>
                        ТК Отели
                    </NavLink>
                    <NavLink to={URLs.testControlPanelTrains}>Поезда</NavLink>
                    <NavLink to={URLs.testControlPanelBusesTestContext}>
                        Автобусы
                    </NavLink>
                    <NavLink to={URLs.testControlPanel3DS}>3DS</NavLink>
                    <NavLink
                        to={URLs.testControlPanelContentMarkupViewer}
                        exact
                    >
                        CMS block preview
                    </NavLink>
                    <NavLink to={URLs.testControlPanelTrips} exact>
                        Поездки
                    </NavLink>
                    <NavLink to={URLs.testControlPanelTravelApp} exact>
                        Travel App
                    </NavLink>
                    <NavLink to={URLs.testControlPanelRequestManager} exact>
                        Запросы
                    </NavLink>
                </Flex>

                <Box y={8}>
                    <Switch>
                        <Route exact path={URLs.testControlPanelRoot}>
                            <Redirect to={URLs.testControlPanelExperiments} />
                        </Route>
                        <Route
                            path={URLs.testControlPanelAviaLegacyTestContext}
                        >
                            <Redirect
                                to={URLs.testControlPanelAviaTestContext}
                            />
                        </Route>
                        <Route
                            exact
                            path={URLs.testControlPanelExperiments}
                            component={ExperimentsPage}
                        />
                        <Route
                            exact
                            path={URLs.testControlPanelPaymentTestContext}
                            component={TestContextPaymentPage}
                        />
                        <Route
                            exact
                            path={URLs.testControlPanelAviaTestContext}
                            component={TestContextAviaPage}
                        />

                        <Route
                            exact
                            path={URLs.testControlPanelHotelsTestContext}
                            component={TestContextHotelsPage}
                        />

                        <Route
                            path={URLs.testControlPanelTrains}
                            component={TrainsPage}
                        />

                        <Route
                            path={URLs.testControlPanelBusesTestContext}
                            component={BusesPage}
                        />

                        <Route
                            path={URLs.testControlPanel3DS}
                            component={Test3DSPage}
                        />

                        <Route
                            exact
                            path={URLs.testControlPanelContentMarkupViewer}
                            component={MarkupViewer}
                        />

                        <Route
                            exact
                            path={URLs.testControlPanelTrips}
                            component={TestTripsPage}
                        />

                        <Route
                            exact
                            path={URLs.testControlPanelTravelApp}
                            component={TestTravelAppPage}
                        />

                        <Route
                            exact
                            path={URLs.testControlPanelRequestManager}
                            component={TestRequestManagerPage}
                        />

                        <Route component={NotFoundContent} />
                    </Switch>
                </Box>
            </Container>
        </LayoutDefault>
    );
};

export default TestControlPanelApp;
