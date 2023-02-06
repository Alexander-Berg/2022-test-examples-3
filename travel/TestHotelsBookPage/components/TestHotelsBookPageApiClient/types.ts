interface IOfferToken {
    offerName: string;
    pansionType: string;
    hasFreeCancellation: boolean;
    price: number;
    token: string;
    bookingPageUrl: number;
}

export interface ITestBookOfferTokenResponse {
    data: {
        offerTokens: IOfferToken[];
    };
}
