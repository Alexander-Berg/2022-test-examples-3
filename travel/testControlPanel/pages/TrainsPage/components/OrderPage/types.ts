import {ITrainsTestContextTokenParams} from 'server/api/TrainsBookingApi/types/ITrainsTestContextToken';

export interface ITestTrainsContextForm extends ITrainsTestContextTokenParams {
    setOnlyForSecondTrain: boolean;
}
