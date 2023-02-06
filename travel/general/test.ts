import {Request, Response} from '@yandex-data-ui/core/build/types';

import {headerMocks} from '../blocks/header/__mocks__';
import {footerMocks} from '../blocks/footer/__mocks__';
import {usefulMocks} from '../blocks/useful/__mocks__';
import {carouselMocks} from '../blocks/carousel/__mocks__';
import {greetingMocks} from '../blocks/greeting/__mocks__';
import {weatherMocks} from '../blocks/weather/__mocks__';
import {preTripMocks} from '../blocks/preTrip/__mocks__';
import {hotelsOrderMocks} from '../blocks/hotelsOrder/__mocks__';
import {trainsOrderMocks} from '../blocks/trainsOrder/__mocks__';
import {disclaimersMocks} from '../blocks/disclaimers/__mocks__';
import baseController from './baseController';

export default (req: Request, res: Response): void => {
    const type = req.query.type as undefined | string | string[];
    const blocks = [
        ...headerMocks,
        ...greetingMocks,
        ...hotelsOrderMocks,
        ...trainsOrderMocks,
        ...usefulMocks,
        ...carouselMocks,
        ...weatherMocks,
        ...preTripMocks,
        ...footerMocks,
        ...disclaimersMocks,
    ].filter((item) => {
        if (!type) {
            return true;
        }

        if (Array.isArray(type)) {
            return type.includes(item.type);
        }

        return type === item.type;
    });

    baseController(req, res, blocks);
};
