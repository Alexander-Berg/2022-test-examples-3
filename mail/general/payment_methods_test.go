package provider

import (
	"a.yandex-team.ru/mail/payments-sdk-backend/internal/interactions/trust"
	"a.yandex-team.ru/mail/payments-sdk-backend/internal/logic/models"
	"a.yandex-team.ru/mail/payments-sdk-backend/internal/utils/ctxutil"
	"context"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"
	"testing"
)

type PaymentMethodsTestSuite struct {
	suite.Suite
	ctx                   context.Context
	sf                    serviceFixture
	paymentSystemsOptions models.PaymentSystemsOptions
	st                    string
	uid                   uint64
	paymentMethods        trust.PaymentMethods
	env                   models.OrderEnvironment
}

func (s *PaymentMethodsTestSuite) SetupTest() {
	s.ctx, s.sf = newPaymentServiceFixture(s.T())
	s.sf.Config.BoundPaymentMethodsWhitelist = []string{"card"}
	s.sf.Config.FamilyPay.UnlimitedBalanceInFractionalUnits = 10000
	s.paymentSystemsOptions = models.PaymentSystemsOptions{
		ApplePayEnabled:  true,
		GooglePayEnabled: true,
	}
	s.st = "service_token"
	s.uid = uint64(123456789)
	s.paymentMethods = paymentMethodsFixture()
	s.env = models.ProductionOrder
}

func (s *PaymentMethodsTestSuite) callPaymentMethods() models.UserPaymentMethods {
	result, err := s.sf.Service.PaymentMethods(s.ctx, s.uid, s.st, s.paymentSystemsOptions, s.env)
	if err != nil {
		s.Fail("Unexpected error during PaymentMethods call", err)
	}
	return result
}

func (s *PaymentMethodsTestSuite) TestOrderByBindingTimeStamp() {
	paymentMethods := s.paymentMethods
	paymentMethods.BoundPaymentMethods[0].BindingTS = "50"
	paymentMethods.BoundPaymentMethods[0].CardID = "1"
	paymentMethods.BoundPaymentMethods[1].BindingTS = "100"
	paymentMethods.BoundPaymentMethods[1].CardID = "2"

	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(paymentMethods, nil).Times(1)
	result := s.callPaymentMethods().PaymentMethods

	s.Require().Equal(
		[]string{"2", "1"},
		[]string{result[0].CardID, result[1].CardID},
	)
}

func (s *PaymentMethodsTestSuite) TestBankNameMapping() {
	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(s.paymentMethods, nil).Times(1)
	result := s.callPaymentMethods().PaymentMethods
	s.Require().Equal("UnknownBank", result[0].CardBank)
	s.Require().Equal("SberBank", result[1].CardBank)
}

func (s *PaymentMethodsTestSuite) TestOrderByLastServiceUsed() {
	paymentMethods := s.paymentMethods
	paymentMethods.BoundPaymentMethods[0].BindingTS = "50"
	paymentMethods.BoundPaymentMethods[0].CardID = "1"
	paymentMethods.BoundPaymentMethods[0].LastPaid = 0
	paymentMethods.BoundPaymentMethods[0].LastServicePaid = 1
	paymentMethods.BoundPaymentMethods[1].BindingTS = "100"
	paymentMethods.BoundPaymentMethods[1].CardID = "2"
	paymentMethods.BoundPaymentMethods[1].LastPaid = 1
	paymentMethods.BoundPaymentMethods[1].LastServicePaid = 0

	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(paymentMethods, nil).Times(1)
	result := s.callPaymentMethods().PaymentMethods

	s.Require().Equal(
		[]string{"1", "2"},
		[]string{result[0].CardID, result[1].CardID},
	)
}

func (s *PaymentMethodsTestSuite) TestOrderByLastUsed() {
	paymentMethods := s.paymentMethods
	paymentMethods.BoundPaymentMethods[0].BindingTS = "50"
	paymentMethods.BoundPaymentMethods[0].CardID = "1"
	paymentMethods.BoundPaymentMethods[0].LastPaid = 1
	paymentMethods.BoundPaymentMethods[0].LastServicePaid = 0
	paymentMethods.BoundPaymentMethods[1].BindingTS = "100"
	paymentMethods.BoundPaymentMethods[1].CardID = "2"
	paymentMethods.BoundPaymentMethods[1].LastPaid = 0
	paymentMethods.BoundPaymentMethods[1].LastServicePaid = 0

	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(paymentMethods, nil).Times(1)
	result := s.callPaymentMethods().PaymentMethods

	s.Require().Equal(
		[]string{"1", "2"},
		[]string{result[0].CardID, result[1].CardID},
	)
}

func (s *PaymentMethodsTestSuite) TestLastPaidAndLastServicePaidAtSameTimeIsNotDuplicatedInResult() {
	paymentMethods := s.paymentMethods
	paymentMethods.BoundPaymentMethods[0].BindingTS = "50"
	paymentMethods.BoundPaymentMethods[0].CardID = "1"
	paymentMethods.BoundPaymentMethods[0].LastPaid = 1
	paymentMethods.BoundPaymentMethods[0].LastServicePaid = 1
	paymentMethods.BoundPaymentMethods[1].BindingTS = "100"
	paymentMethods.BoundPaymentMethods[1].CardID = "2"
	paymentMethods.BoundPaymentMethods[1].LastPaid = 0
	paymentMethods.BoundPaymentMethods[1].LastServicePaid = 0

	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(paymentMethods, nil).Times(1)
	result := s.callPaymentMethods().PaymentMethods

	s.Require().Equal(
		[]string{"1", "2"},
		[]string{result[0].CardID, result[1].CardID},
	)
}

func (s *PaymentMethodsTestSuite) TestWithServiceToken() {
	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(s.paymentMethods, nil).Times(1)

	result := s.callPaymentMethods()
	expected := models.UserPaymentMethods{
		GooglePaySupported: false,
		ApplePaySupported:  false,
		PaymentMethods: []models.BoundPaymentMethod{
			result.PaymentMethods[0],
			result.PaymentMethods[1],
		},
		EnabledPaymentMethods: []models.EnabledPaymentMethods{
			result.EnabledPaymentMethods[0],
		},
	}
	expected.PaymentMethods[0].VerifyCvv = true
	expected.PaymentMethods[1].VerifyCvv = true
	s.Require().Equal(expected, result)
}

func (s *PaymentMethodsTestSuite) TestWithServiceTokenSandbox() {
	s.env = models.SandboxOrder
	s.sf.SandboxTrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(s.paymentMethods, nil).Times(1)

	s.callPaymentMethods()
}

func (s *PaymentMethodsTestSuite) TestWithoutServiceToken() {
	const defaultServiceToken = "default_service_token"

	s.st = ""
	s.sf.Config.DefaultAcquirer = "default"
	s.sf.Config.YaPaymentsServices = map[string]string{"default": defaultServiceToken}

	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), defaultServiceToken, s.uid).Return(paymentMethodsFixture(), nil).Times(1)

	s.callPaymentMethods()
}

func (s *PaymentMethodsTestSuite) TestWithoutServiceTokenSandbox() {
	s.env = models.SandboxOrder
	s.st = ""
	const sandboxServiceToken = "sandbox_service_token"
	s.sf.Config.SandboxServiceToken = sandboxServiceToken
	s.sf.SandboxTrustMock.EXPECT().GetPaymentMethods(gomock.Any(), sandboxServiceToken, s.uid).Return(paymentMethodsFixture(), nil).Times(1)

	s.callPaymentMethods()
}

func (s *PaymentMethodsTestSuite) TestFilterBoundPaymentMethods() {
	s.paymentMethods.BoundPaymentMethods = append(
		s.paymentMethods.BoundPaymentMethods,
		trust.BoundPaymentMethod{PaymentMethod: "yamoney_wallet"},
	)
	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(s.paymentMethods, nil).Times(1)

	result := s.callPaymentMethods()
	s.Require().Equal(len(s.paymentMethods.BoundPaymentMethods)-1, len(result.PaymentMethods))
}

func (s *PaymentMethodsTestSuite) TestEnableSBPQR() {
	s.sf.Config.HideSBPQR = false
	sbp := trust.EnabledPaymentMethods{
		PaymentMethod:  "sbp_qr",
		PaymentSystems: []string{},
	}
	s.paymentMethods.EnabledPaymentMethods = append(s.paymentMethods.EnabledPaymentMethods, sbp)
	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(s.paymentMethods, nil).Times(1)

	result := s.callPaymentMethods()
	s.Require().Contains(result.EnabledPaymentMethods, models.EnabledPaymentMethods(sbp))
}

func (s *PaymentMethodsTestSuite) TestDisableSBPQR() {
	s.sf.Config.HideSBPQR = true
	sbp := trust.EnabledPaymentMethods{
		PaymentMethod:  "sbp_qr",
		PaymentSystems: []string{},
	}
	s.paymentMethods.EnabledPaymentMethods = append(s.paymentMethods.EnabledPaymentMethods, sbp)
	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(s.paymentMethods, nil).Times(1)

	result := s.callPaymentMethods()
	s.Require().NotContains(result.EnabledPaymentMethods, models.EnabledPaymentMethods(sbp))
}

func (s *PaymentMethodsTestSuite) TestDisableAGPayPerPartner() {
	s.paymentSystemsOptions = models.PaymentSystemsOptions{
		ApplePayEnabled:  true,
		GooglePayEnabled: false,
	}

	s.sf.Config.ApplePay.Enabled = true
	s.sf.Config.GooglePay.Enabled = true
	onlyGooglePay := trust.EnabledPaymentMethods{
		PaymentMethod:  "card",
		PaymentSystems: []string{"GooglePay"},
		Currency:       "RUB",
		FirmID:         1,
	}
	includesGooglePay := trust.EnabledPaymentMethods{
		PaymentMethod:  "card",
		PaymentSystems: []string{"ApplePay", "GooglePay", "VISA", "MasterCard"},
		Currency:       "RUB",
		FirmID:         1,
	}
	s.paymentMethods.EnabledPaymentMethods = []trust.EnabledPaymentMethods{
		includesGooglePay,
		onlyGooglePay,
	}
	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(s.paymentMethods, nil).Times(1)

	result := s.callPaymentMethods()
	s.Require().ElementsMatch(result.EnabledPaymentMethods, []models.EnabledPaymentMethods{
		{
			PaymentMethod:  "card",
			PaymentSystems: []string{"ApplePay", "VISA", "MasterCard"},
			Currency:       "RUB",
			FirmID:         1,
		},
		{
			PaymentMethod:  "card",
			PaymentSystems: []string{},
			Currency:       "RUB",
			FirmID:         1,
		},
	})
}

func (s *PaymentMethodsTestSuite) TestFamilyInfo() {
	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(s.paymentMethods, nil).Times(1)
	result := s.callPaymentMethods().PaymentMethods
	s.Require().Empty(result[0].PayerInfo)
	s.Require().EqualValues(result[1].PayerInfo, &models.PayerInfo{
		UID: "family_admin_uid",
		FamilyInfo: models.FamilyInfo{
			FamilyID:  "family_id",
			Expenses:  100,
			Limit:     1000,
			Frame:     "month",
			Currency:  "RUB",
			Unlimited: false,
		},
	})
}

func (s *PaymentMethodsTestSuite) TestPartnerInfo() {
	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(s.paymentMethods, nil).Times(1)
	result := s.callPaymentMethods().PaymentMethods
	s.Require().Empty(result[0].PartnerInfo)

	s.Require().EqualValues(result[1].PartnerInfo, &models.PartnerInfo{
		IsYabankCard:      false,
		IsYabankCardOwner: true,
		IsFakeYabankCard:  true,
	})
}

func (s *PaymentMethodsTestSuite) TestFilterYabankSystem() {
	s.paymentMethods.BoundPaymentMethods = append(
		s.paymentMethods.BoundPaymentMethods,
		trust.BoundPaymentMethod{PaymentMethod: "card", PartnerInfo: &trust.PartnerInfo{
			IsYabankCard:      true,
			IsYabankCardOwner: false,
			IsFakeYabankCard:  false,
		}},
	)

	s.sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), s.st, s.uid).Return(s.paymentMethods, nil).Times(3)
	result := s.callPaymentMethods().PaymentMethods
	s.Require().Empty(result[0].PartnerInfo)
	s.Require().EqualValues(len(result), 2)

	s.ctx = ctxutil.WithSdkVersion(s.ctx, "3.8.9")
	result = s.callPaymentMethods().PaymentMethods
	s.Require().EqualValues(len(result), 2)

	s.ctx = ctxutil.WithSdkVersion(s.ctx, "3.9.0")
	result = s.callPaymentMethods().PaymentMethods
	s.Require().EqualValues(len(result), 3)
}

func TestPaymentMethodsTestSuite(t *testing.T) {
	suite.Run(t, new(PaymentMethodsTestSuite))
}
