import {
    TGetPaymentTestContextTokenParams,
    EPaymentOutcome,
} from 'server/api/OrdersAPI/types/TGetPaymentTestContextTokenParams';
import IGetPaymentTestContextTokenResponse from 'server/api/OrdersAPI/types/IGetPaymentTestContextTokenResponse';
import {
    ECheckAvailabilityBeforeBookingOutcome,
    ECheckAvailabilityOnRedirOutcome,
    EConfirmationOutcome,
    EMqEventOutcome,
    ETokenizationOutcome,
} from 'server/api/AviaBookingApi/types/IAviaTestContextTokenApiParams';
import {
    ITestBookOfferTokenRequestParams,
    ITestBookOfferTokenResponse,
} from 'server/api/HotelsBookAPI/types/ITestBookOfferToken';
import {
    ERefundPricingOutcome,
    ERefundCheckoutOutcome,
    EInsurancePricingOutcome,
    ECreateReservationOutcome,
    EInsuranceCheckoutOutcome,
    EConfirmReservationOutcome,
    ITrainsTestContextTokenAnswer,
    ITrainsTestContextTokenParams,
    EInsuranceCheckoutConfirmOutcome,
} from 'server/api/TrainsBookingApi/types/ITrainsTestContextToken';
import {EFeatureFlagName} from 'types/IFeatureFlags';
import {ITrainsMockIm} from 'types/trains/common/testContext/ITrainsMockIm';
import {ICreateTrainServiceParams} from 'server/api/GenericOrderApi/types/common/ICreateTrainServiceParams';
import IAviaTestContextTokenServiceParams from 'server/services/TestContextService/types/IAviaTestContextTokenServiceParams';
import IAviaTestContextTokenServiceResponse from 'server/services/TestContextService/types/IAviaTestContextTokenServiceResponse';

import {IDependencies} from 'server/getContainerConfig';
import {OrdersAPI} from 'server/api/OrdersAPI/OrdersAPI';
import {HotelsBookAPI} from 'server/api/HotelsBookAPI/HotelsBookAPI';
import {BunkerService} from 'server/services/BunkerService/BunkerService';
import {TrainsBookingApi} from 'server/api/TrainsBookingApi/TrainsBookingApi';
import {AviaBookingTestContextApi} from 'server/api/AviaBookingTestContextApi/AviaBookingTestContextApi';

export class TestContextService {
    private readonly ordersApi: OrdersAPI;
    private readonly aviaBookingTestContextApi: AviaBookingTestContextApi;
    private readonly hotelsBookAPI: HotelsBookAPI;
    private readonly trainsBookingApi: TrainsBookingApi;

    private readonly bunkerService: BunkerService;

    private readonly canUseMockPayment: boolean;
    private readonly canUseTestContext: boolean;
    private readonly isCrowdTestProxy: boolean;

    constructor({
        ordersAPI,
        aviaBookingTestContextApi,
        hotelsBookAPI,
        trainsBookingApi,
        appConfig,
        isCrowdTestProxy,
        bunkerService,
    }: IDependencies) {
        this.ordersApi = ordersAPI;
        this.aviaBookingTestContextApi = aviaBookingTestContextApi;
        this.hotelsBookAPI = hotelsBookAPI;
        this.trainsBookingApi = trainsBookingApi;

        this.bunkerService = bunkerService;

        this.canUseMockPayment = appConfig.orders.canUseMockPayment;
        this.canUseTestContext = appConfig.orders.canUseTestContext;
        this.isCrowdTestProxy = isCrowdTestProxy;
    }

    getAviaTestContextToken(
        params: IAviaTestContextTokenServiceParams,
    ): Promise<IAviaTestContextTokenServiceResponse> {
        return this.aviaBookingTestContextApi.getTestContextToken(params);
    }

    async getAviaTestContextTokenIfNeeded(
        contextToken: string | undefined,
    ): Promise<string | undefined> {
        if (contextToken) {
            return contextToken;
        }

        if (!this.isCrowdTestProxy) {
            return;
        }

        const {token} = await this.getAviaTestContextToken({
            checkAvailabilityOnRedirOutcome:
                ECheckAvailabilityOnRedirOutcome.SUCCESS,
            checkAvailabilityBeforeBookingOutcome:
                ECheckAvailabilityBeforeBookingOutcome.SUCCESS,
            tokenizationOutcome: ETokenizationOutcome.SUCCESS,
            confirmationOutcome: EConfirmationOutcome.SUCCESS,
            mqEventOutcome: EMqEventOutcome.SUCCESS,
        });

        return token;
    }

    getHotelsTestContextToken(
        params: ITestBookOfferTokenRequestParams,
    ): Promise<ITestBookOfferTokenResponse> {
        return this.hotelsBookAPI.getTestBookOfferToken(params);
    }

    getTrainsTestContextToken(
        params: ITrainsTestContextTokenParams,
    ): Promise<ITrainsTestContextTokenAnswer> {
        return this.trainsBookingApi.testContextToken(params);
    }

    getTrainsSuccessFlowTestContextToken(
        additionalParams?: ITrainsTestContextTokenParams,
    ): Promise<ITrainsTestContextTokenAnswer> {
        return this.getTrainsTestContextToken({
            insurancePricingOutcome: EInsurancePricingOutcome.SUCCESS,
            insuranceCheckoutOutcome: EInsuranceCheckoutOutcome.SUCCESS,
            insuranceCheckoutConfirmOutcome:
                EInsuranceCheckoutConfirmOutcome.SUCCESS,
            refundPricingOutcome: ERefundPricingOutcome.SUCCESS,
            refundCheckoutOutcome: ERefundCheckoutOutcome.SUCCESS,
            createReservationOutcome: ECreateReservationOutcome.SUCCESS,
            confirmReservationOutcome: EConfirmReservationOutcome.SUCCESS,
            ...additionalParams,
        });
    }

    getPaymentTestContextToken(
        params: TGetPaymentTestContextTokenParams,
    ): Promise<IGetPaymentTestContextTokenResponse> {
        return this.ordersApi.getPaymentTestContextToken(params);
    }

    async getPaymentTestContextTokenIfNeeded(
        paymentToken: string | undefined,
    ): Promise<string | undefined> {
        if (!this.canUseMockPayment) {
            return;
        }

        if (paymentToken) {
            return paymentToken;
        }

        if (!this.isCrowdTestProxy) {
            return;
        }

        const {token} = await this.getPaymentTestContextToken({
            paymentOutcome: EPaymentOutcome.PO_SUCCESS,
        });

        return token;
    }

    async getStartPaymentTestContextTokenIfNeeded(
        paymentToken: string | undefined,
    ): Promise<string | undefined> {
        if (!this.canUseMockPayment) {
            return;
        }

        return paymentToken;
    }

    async checkTrainsUseMockImTestContext(): Promise<boolean> {
        if (this.isCrowdTestProxy && this.canUseTestContext) {
            const featureFlags = await this.bunkerService.getFeatureFlags();

            return (
                featureFlags?.[
                    EFeatureFlagName.TRAINS_ASSESSORS_IM_TEST_CONTEXT
                ]?.enabled ?? false
            );
        }

        return false;
    }

    async addTestContextTokenToTrainServicesIfNeed(
        trainServices: ICreateTrainServiceParams[],
    ): Promise<ICreateTrainServiceParams[]> {
        const canUseMockImAutoTestContext =
            await this.checkTrainsUseMockImTestContext();

        if (!trainServices?.length) {
            return trainServices;
        }

        if (canUseMockImAutoTestContext) {
            const {test_context_token: trainTestContextToken} =
                await this.getTrainsSuccessFlowTestContextToken();

            return trainServices.map(trainService => ({
                ...trainService,
                trainTestContextToken,
            }));
        }

        return trainServices;
    }

    async getTrainsMockImTestContextParamsIfNeed(): Promise<
        ITrainsMockIm | undefined
    > {
        const canUseMockImAutoTestContext =
            await this.checkTrainsUseMockImTestContext();

        if (canUseMockImAutoTestContext) {
            return {
                mockImAuto: true,
            };
        }

        return {};
    }
}
