import IFlightInfo from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaResultVariant/types/IFlightInfo';

import TestDepartureAndArrivalInfo from 'components/TestDepartureAndArrivalInfo';

export default class TestFlights extends TestDepartureAndArrivalInfo {
    async getFlightInfo(): Promise<IFlightInfo> {
        const departure = await this.fromTimeBottomDescription.getText();
        const arrival = await this.toTimeBottomDescription.getText();

        return {
            departureTime: await this.fromTime.getText(),
            arrivalTime: await this.toTime.getText(),
            duration: await this.duration.getText(),
            departure: departure,
            departureIATA: departure,
            arrival: arrival,
            arrivalIATA: arrival,
        };
    }
}
