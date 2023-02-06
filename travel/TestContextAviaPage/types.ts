import IAviaTestContextTokenServiceParams from 'server/services/TestContextService/types/IAviaTestContextTokenServiceParams';

export interface ITestAviaContextForm
    extends IAviaTestContextTokenServiceParams {
    skipPayment: boolean;
}
