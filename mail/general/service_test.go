package provider

import (
	"a.yandex-team.ru/mail/payments-sdk-backend/internal/interactions/trust"
	trustmock "a.yandex-team.ru/mail/payments-sdk-backend/internal/interactions/trust/mocks"
	yapaymock "a.yandex-team.ru/mail/payments-sdk-backend/internal/interactions/yapay/mocks"
	"a.yandex-team.ru/mail/payments-sdk-backend/internal/testutils"
	"a.yandex-team.ru/mail/payments-sdk-backend/internal/utils/ctxutil"
	"context"
	"github.com/golang/mock/gomock"
	"testing"
)

type serviceFixture struct {
	Ctrl             *gomock.Controller
	TrustMock        *trustmock.MockClient
	SandboxTrustMock *trustmock.MockClient
	YapayMock        *yapaymock.MockClient
	Config           *Config
	Service          *PaymentService
}

func newPaymentServiceFixture(t *testing.T) (context.Context, serviceFixture) {
	logger := testutils.NewLogger()
	ctrl := gomock.NewController(t)
	trustClient := trustmock.NewMockClient(ctrl)
	sandboxTrustClient := trustmock.NewMockClient(ctrl)
	yapayClient := yapaymock.NewMockClient(ctrl)
	cfg := &Config{}

	ctx := ctxutil.WithLogger(context.Background(), logger)
	service, _ := NewService(trustClient, sandboxTrustClient, yapayClient, logger, cfg, TestResources{})

	return ctx, serviceFixture{
		Ctrl:             ctrl,
		TrustMock:        trustClient,
		SandboxTrustMock: sandboxTrustClient,
		YapayMock:        yapayClient,
		Service:          service,
		Config:           cfg,
	}
}

type TestResources struct {
}

func (TestResources) GetBankNames() map[string]string {
	return map[string]string{
		"ALFA BANK": "AlfaBank",
		"SAVINGS BANK OF THE RUSSIAN FEDERATION (SBERBANK)": "SberBank",
	}
}

func paymentMethodsFixture() trust.PaymentMethods {
	return trust.PaymentMethods{
		Status: "success",
		EnabledPaymentMethods: []trust.EnabledPaymentMethods{
			{
				PaymentMethod:  "card",
				PaymentSystems: []string{"ApplePay", "GooglePay", "VISA", "MasterCard"},
				Currency:       "RUB",
				FirmID:         1,
			},
		},
		BoundPaymentMethods: []trust.BoundPaymentMethod{
			{
				PaymentSystem:               "MasterCard",
				CardBank:                    "RBS BANK (ROMANIA), S.A.",
				PaymentMethod:               "card",
				Account:                     "510000****0658",
				RegionID:                    225,
				Aliases:                     []string{"card-x514d801da3ef8609e1e19bb6"},
				CardLevel:                   "",
				RecommendedVerificationType: "standard2_3ds",
				CardID:                      "card-x514d801da3ef8609e1e19bb6",
				BindingSystems:              []string{"trust"},
				Holder:                      "Card Holder",
				BindingTS:                   "1595941218.522",
				CardCountry:                 "ROU",
				Expired:                     false,
				ID:                          "card-x514d801da3ef8609e1e19bb6",
				System:                      "MasterCard",
				IsSpasibo:                   0,
				LastPaid:                    0,
				LastServicePaid:             0,
				PayerInfo:                   nil,
			},
			{
				PaymentSystem:               "MasterCard",
				CardBank:                    "SAVINGS BANK OF THE RUSSIAN FEDERATION (SBERBANK)",
				PaymentMethod:               "card",
				Account:                     "546938****9762",
				RegionID:                    225,
				Aliases:                     []string{"card-x74507cf65a304489208ddbb5"},
				CardLevel:                   "",
				RecommendedVerificationType: "standard2",
				CardID:                      "card-x74507cf65a304489208ddbb5",
				BindingSystems:              []string{"trust"},
				Holder:                      "TEST TEST",
				BindingTS:                   "1595868573.006",
				CardCountry:                 "RUS",
				Expired:                     false,
				ID:                          "card-x74507cf65a304489208ddbb5",
				System:                      "MasterCard",
				IsSpasibo:                   0,
				LastPaid:                    0,
				LastServicePaid:             0,
				PayerInfo: &trust.PayerInfo{
					UID: "family_admin_uid",
					FamilyInfo: trust.FamilyInfo{
						FamilyID: "family_id",
						Expenses: 100,
						Limit:    1000,
						Frame:    "month",
						Currency: "RUB",
					},
				},
				PartnerInfo: &trust.PartnerInfo{
					IsYabankCard:      false,
					IsYabankCardOwner: true,
					IsFakeYabankCard:  true,
				},
			},
		},
	}
}
