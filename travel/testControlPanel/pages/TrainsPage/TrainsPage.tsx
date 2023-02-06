import {memo, FC} from 'react';
import {Route, Switch} from 'react-router-dom';

import {URLs} from 'constants/urls';

import Box from 'components/Box/Box';
import Flex from 'components/Flex/Flex';
import OrderPage from 'projects/testControlPanel/pages/TrainsPage/components/OrderPage/OrderPage';
import TrainDetailsPage from 'projects/testControlPanel/pages/TrainsPage/components/TrainDetailsPage/TrainDetailsPage';
import SearchPage from 'projects/testControlPanel/pages/TrainsPage/components/SearchPage/SearchPage';
import NavLink from 'projects/testControlPanel/components/NavLink/NavLink';

import cx from './TrainsPage.scss';

const TrainsPage: FC = () => {
    return (
        <Box between={5}>
            <Flex between={3} inline>
                <NavLink
                    className={cx('link')}
                    to={URLs.testControlPanelTrainsSearch}
                    exact
                >
                    Поиск
                </NavLink>

                <NavLink
                    className={cx('link')}
                    to={URLs.testControlPanelTrainsTrainDetails}
                    exact
                >
                    Выбор мест
                </NavLink>

                <NavLink
                    className={cx('link')}
                    to={URLs.testControlPanelTrainsTestContext}
                    exact
                >
                    Заказ
                </NavLink>
            </Flex>

            <Switch>
                <Route path={URLs.testControlPanelTrainsSearch} exact>
                    <SearchPage />
                </Route>

                <Route path={URLs.testControlPanelTrainsTrainDetails} exact>
                    <TrainDetailsPage />
                </Route>

                <Route path={URLs.testControlPanelTrainsTestContext} exact>
                    <OrderPage />
                </Route>
            </Switch>
        </Box>
    );
};

export default memo(TrainsPage);
