package provider

import (
	"a.yandex-team.ru/mail/payments-sdk-backend/internal/interactions/trust"
	"a.yandex-team.ru/mail/payments-sdk-backend/internal/interactions/yapay"
	"a.yandex-team.ru/mail/payments-sdk-backend/internal/logic/models"
	"a.yandex-team.ru/mail/payments-sdk-backend/internal/utils/ctxutil"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/require"
	"testing"
)

type AntiFraudCheckMode int

const (
	antiFraudCallIndifferent AntiFraudCheckMode = iota
	antiFraudAssertCalled                       = iota
	antiFraudAssertNotCalled                    = iota
)

type InteractionMockParams struct {
	isYaPay            bool
	uid                uint64
	payToken           string
	turboappID         string
	email              string
	serviceToken       string
	purchaseToken      string
	order              yapay.Order
	paymentMethods     trust.PaymentMethods
	startPaymentInfo   trust.StartPaymentInfo
	afPaymentMethods   trust.AFPaymentMethods
	paymentStatus      trust.PaymentStatus
	antiFraudCheckMode AntiFraudCheckMode
	credit             bool
}

func SetupInteractionMocks(sf serviceFixture, params InteractionMockParams) {
	if params.isYaPay {
		sf.YapayMock.EXPECT().StartOrder(gomock.Any(), params.uid, params.payToken, params.turboappID, params.email).Return(params.order, nil).Times(1)
	}

	sf.TrustMock.EXPECT().GetPaymentMethods(gomock.Any(), params.serviceToken, params.uid).Return(params.paymentMethods, nil).Times(1)
	if params.antiFraudCheckMode == antiFraudCallIndifferent || params.antiFraudCheckMode == antiFraudAssertCalled {
		antiFraudMock := sf.TrustMock.EXPECT().GetAFPaymentMethods(gomock.Any(), params.purchaseToken, params.serviceToken, params.uid).Return(params.afPaymentMethods, nil)
		if params.antiFraudCheckMode == antiFraudAssertCalled {
			antiFraudMock.Times(1)
		} else {
			antiFraudMock.AnyTimes()
		}
	}
	sf.TrustMock.EXPECT().GetPaymentStatus(gomock.Any(), params.purchaseToken, params.serviceToken, params.uid).Return(params.paymentStatus, nil).Times(1)
	sf.TrustMock.EXPECT().StartPayment(gomock.Any(), params.purchaseToken, params.serviceToken, params.uid, params.credit).Return(params.startPaymentInfo, nil).Times(1)
}

func TestPaymentService_InitPayment(t *testing.T) {
	const (
		merchantName         = "<name>"
		merchantScheduleText = "<schedule_text>"
		merchantOGRN         = "<ogrn>"
		merchantCity         = "<city>"
		merchantCountry      = "<country>"
		merchantHome         = "<home>"
		merchantStreet       = "<street>"
		merchantZip          = "<zip>"
	)

	tests := []struct {
		name                  string
		isYaPay               bool
		payToken              string
		turboappID            string
		email                 string
		uid                   uint64
		purchaseToken         string
		acquire               string
		payEnv                string
		inputServiceToken     string
		serviceToken          string
		licenceURL            string
		total                 string
		currency              string
		gwMerchantID          string
		sdkVersion            string
		gateway               models.GPayGateway
		afData                map[int]bool
		afExpected            map[int]bool
		apEnabled             bool
		gpEnabled             bool
		forceCvv              string
		paymentMarkup         map[string]map[string]string
		paymentMarkupExpected map[string]string
		credit                bool
	}{
		{
			name:          "YaPay_Basic",
			isYaPay:       true,
			payToken:      "payment:<hash>",
			turboappID:    "<turboapp_id>",
			email:         "test@test.ru",
			uid:           uint64(123456789),
			purchaseToken: "<purchase_token>",
			acquire:       "kassa",
			payEnv:        "production",
			serviceToken:  "<service_token>",
			licenceURL:    "https://licence",
			total:         "100.00",
			currency:      "RUB",
			gwMerchantID:  "<gpay_merchant_id>",
			sdkVersion:    "1.0.0",
			gateway:       models.GPayGatewayYaPayments,
			afData:        map[int]bool{0: true, 1: true},
			afExpected:    map[int]bool{0: true, 1: true},
			apEnabled:     true,
			gpEnabled:     true,
		},
		{
			name:          "YaPay_WithoutEmail",
			isYaPay:       true,
			payToken:      "payment:<hash>",
			uid:           uint64(123456789),
			purchaseToken: "<purchase_token>",
			acquire:       "kassa",
			payEnv:        "production",
			serviceToken:  "<service_token>",
			licenceURL:    "https://licence",
			total:         "100.00",
			currency:      "RUB",
			gwMerchantID:  "<gpay_merchant_id>",
			sdkVersion:    "1.0.0",
			gateway:       models.GPayGatewayYaPayments,
			afData:        map[int]bool{0: true, 1: true},
			afExpected:    map[int]bool{0: true, 1: true},
			apEnabled:     true,
			gpEnabled:     true,
		},
		{
			name:              "Trust_Basic",
			isYaPay:           false,
			payToken:          "<purchase_token>",
			turboappID:        "<turboapp_id>",
			uid:               uint64(123456789),
			purchaseToken:     "<purchase_token>",
			payEnv:            "production",
			inputServiceToken: "<service_token>",
			serviceToken:      "<service_token>",
			total:             "100.00",
			currency:          "RUB",
			gwMerchantID:      "<service_token>",
			sdkVersion:        "1.0.0",
			gateway:           models.GPayGatewayTrust,
			afData:            map[int]bool{0: true, 1: true},
			afExpected:        map[int]bool{0: true, 1: true},
			apEnabled:         true,
			gpEnabled:         true,
			paymentMarkup: map[string]map[string]string{
				"123": {"spasibo": "1.5", "card": "2.3"},
				"456": {"spasibo": "1.2", "card": "2.78"},
			},
			paymentMarkupExpected: map[string]string{
				"spasibo": "2.7",
				"card":    "5.08",
			},
			credit: true,
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			paymentMethods := paymentMethodsFixture()

			order := yapay.Order{
				PurchaseToken: test.purchaseToken,
				AcquirerType:  test.acquire,
				Environment:   test.payEnv,
				PaymentSystemsOptions: yapay.PaymentSystemsOptions{
					ApplePayEnabled:  true,
					GooglePayEnabled: true,
				},
				Merchant: yapay.Merchant{
					Name:         merchantName,
					ScheduleText: merchantScheduleText,
					OGRN:         merchantOGRN,
					LegalAddress: yapay.LegalAddress{
						City:    merchantCity,
						Country: merchantCountry,
						Home:    merchantHome,
						Street:  merchantStreet,
						Zip:     merchantZip,
					},
				},
			}

			af := trust.AFPaymentMethods{
				Status: "success",
				PaymentMethods: []trust.AFBoundPaymentMethod{
					{CardID: paymentMethods.BoundPaymentMethods[0].CardID, NeedCvv: test.afData[0]},
					{CardID: paymentMethods.BoundPaymentMethods[1].CardID, NeedCvv: test.afData[1]},
				},
			}

			ps := trust.PaymentStatus{
				Status:        "success",
				PurchaseToken: test.purchaseToken,
				Total:         test.total,
				Currency:      test.currency,
				PaymentMarkup: test.paymentMarkup,
			}

			ctx, sf := newPaymentServiceFixture(t)
			ctx = ctxutil.WithSdkVersion(ctx, test.sdkVersion)
			ctx = ctxutil.WithForceCvv(ctx, test.forceCvv)

			sf.Config.YaPaymentsServices = map[string]string{test.acquire: test.serviceToken}
			sf.Config.LicenceURL = test.licenceURL
			sf.Config.GooglePay.Gateway.YaPay = test.gwMerchantID
			sf.Config.BoundPaymentMethodsWhitelist = []string{"card"}

			sf.Config.GooglePay.Enabled = test.gpEnabled
			sf.Config.ApplePay.Enabled = test.apEnabled

			SetupInteractionMocks(sf, InteractionMockParams{
				isYaPay:          test.isYaPay,
				uid:              test.uid,
				payToken:         test.payToken,
				turboappID:       test.turboappID,
				email:            test.email,
				serviceToken:     test.serviceToken,
				purchaseToken:    test.purchaseToken,
				order:            order,
				paymentMethods:   paymentMethods,
				paymentStatus:    ps,
				afPaymentMethods: af,
				credit:           test.credit,
			})

			res, err := sf.Service.InitPayment(ctx, test.uid, test.inputServiceToken, test.email, test.payToken, test.turboappID, test.credit)

			if err != nil {
				t.Fail()
				return
			}

			expectedOrder := models.Order{
				Token:        test.purchaseToken,
				LicenseURL:   test.licenceURL,
				Currency:     test.currency,
				Environment:  models.OrderEnvironment(test.payEnv),
				Total:        test.total,
				AcquirerType: test.acquire,
			}
			expectedGPay := models.GPayData{
				GatewayMerchantID: test.gwMerchantID,
				Gateway:           test.gateway,
			}

			expectedPM := models.UserPaymentMethods{
				GooglePaySupported: test.gpEnabled,
				ApplePaySupported:  test.apEnabled,
				PaymentMethods: []models.BoundPaymentMethod{
					res.PaymentMethods[0],
					res.PaymentMethods[1],
				},
				EnabledPaymentMethods: []models.EnabledPaymentMethods{
					res.EnabledPaymentMethods[0],
				},
			}
			expectedPM.PaymentMethods[0].VerifyCvv = test.afExpected[0]
			expectedPM.PaymentMethods[1].VerifyCvv = test.afExpected[1]

			expectedM := models.Merchant{}
			if test.isYaPay {
				expectedM = models.Merchant{
					Name:         merchantName,
					OGRN:         merchantOGRN,
					ScheduleText: merchantScheduleText,
					LegalAddress: models.LegalAddress{
						City:    merchantCity,
						Country: merchantCountry,
						Home:    merchantHome,
						Street:  merchantStreet,
						Zip:     merchantZip,
					},
				}
			}

			paymentMarkupExpected := make(map[string]string)
			if test.paymentMarkupExpected != nil {
				paymentMarkupExpected = test.paymentMarkupExpected
			}

			expected := models.Payment{
				Order:              expectedOrder,
				UserPaymentMethods: expectedPM,
				GooglePay:          &expectedGPay,
				Merchant:           expectedM,
				PayMethodMarkup:    paymentMarkupExpected,
			}

			require.Equal(t, expected, res)
		})
	}
}

func TestPaymentService_InitPayment_Flags(t *testing.T) {
	paymentMethodsFixture := func(paymentSystems []string) trust.PaymentMethods {
		return trust.PaymentMethods{
			Status: "success",
			EnabledPaymentMethods: []trust.EnabledPaymentMethods{
				{
					PaymentMethod:  "card",
					PaymentSystems: paymentSystems,
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
				},
			},
		}
	}
	params := struct {
		name           string
		isYaPay        bool
		payToken       string
		turboappID     string
		email          string
		uid            uint64
		purchaseToken  string
		acquire        string
		payEnv         string
		serviceToken   string
		licenceURL     string
		total          string
		currency       string
		gwMerchantID   string
		sdkVersion     string
		gateway        models.GPayGateway
		afData         map[int]bool
		afExpected     map[int]bool
		apEnabled      bool
		gpEnabled      bool
		forceCvv       string
		paymentSystems []string
		credit         bool
	}{
		isYaPay:        true,
		paymentSystems: []string{"ApplePay", "GooglePay", "VISA", "MasterCard"},
		payToken:       "payment:<hash>",
		turboappID:     "<turboapp_id>",
		uid:            uint64(123456789),
		purchaseToken:  "<purchase_token>",
		payEnv:         "production",
		serviceToken:   "<service_token>",
	}

	agpayTests := []struct {
		name                   string
		uidApEnabled           bool
		uidGpEnabled           bool
		configApEnabled        bool
		configGpEnabled        bool
		partnerApEnabled       bool
		partnerGpEnabled       bool
		serviceGpEnabled       bool
		serviceApEnabled       bool
		systemOptionsEnabled   bool
		expectedGpEnabled      bool
		expectedApEnabled      bool
		expectedPaymentSystems []string
		credit                 bool
	}{
		{
			name:                   "AllEnabled",
			uidApEnabled:           true,
			uidGpEnabled:           true,
			configApEnabled:        true,
			configGpEnabled:        true,
			partnerApEnabled:       true,
			partnerGpEnabled:       true,
			serviceGpEnabled:       true,
			serviceApEnabled:       true,
			systemOptionsEnabled:   true,
			expectedGpEnabled:      true,
			expectedApEnabled:      true,
			expectedPaymentSystems: []string{"ApplePay", "GooglePay", "VISA", "MasterCard"},
		},
		{
			name:                   "ConfigDisabled",
			uidApEnabled:           false,
			uidGpEnabled:           false,
			configApEnabled:        false,
			configGpEnabled:        false,
			partnerApEnabled:       true,
			partnerGpEnabled:       true,
			serviceGpEnabled:       false,
			serviceApEnabled:       false,
			systemOptionsEnabled:   true,
			expectedGpEnabled:      false,
			expectedApEnabled:      false,
			expectedPaymentSystems: []string{"VISA", "MasterCard"},
		},
		{
			name:                   "ConfigDisabledButServiceEnabled",
			uidApEnabled:           true,
			uidGpEnabled:           true,
			configApEnabled:        false,
			configGpEnabled:        false,
			partnerApEnabled:       true,
			partnerGpEnabled:       true,
			serviceGpEnabled:       true,
			serviceApEnabled:       true,
			systemOptionsEnabled:   true,
			expectedGpEnabled:      true,
			expectedApEnabled:      true,
			expectedPaymentSystems: []string{"ApplePay", "GooglePay", "VISA", "MasterCard"},
		},
		{
			name:                   "ConfigAndServiceDisabledButUidEnabled",
			uidApEnabled:           true,
			uidGpEnabled:           true,
			configApEnabled:        false,
			configGpEnabled:        false,
			partnerApEnabled:       true,
			partnerGpEnabled:       true,
			serviceGpEnabled:       false,
			serviceApEnabled:       false,
			systemOptionsEnabled:   true,
			expectedGpEnabled:      true,
			expectedApEnabled:      true,
			expectedPaymentSystems: []string{"ApplePay", "GooglePay", "VISA", "MasterCard"},
		},
		{
			name:                   "PartnerIsDisabled",
			uidApEnabled:           true,
			uidGpEnabled:           true,
			configApEnabled:        true,
			configGpEnabled:        true,
			partnerApEnabled:       false,
			partnerGpEnabled:       false,
			serviceGpEnabled:       true,
			serviceApEnabled:       true,
			systemOptionsEnabled:   true,
			expectedGpEnabled:      false,
			expectedApEnabled:      false,
			expectedPaymentSystems: []string{"VISA", "MasterCard"},
		},
		{
			name:                   "PartnerIsDisabled_ButFlagIsClear",
			uidApEnabled:           true,
			uidGpEnabled:           true,
			configApEnabled:        true,
			configGpEnabled:        true,
			partnerApEnabled:       false,
			partnerGpEnabled:       false,
			serviceGpEnabled:       true,
			serviceApEnabled:       true,
			systemOptionsEnabled:   false,
			expectedGpEnabled:      true,
			expectedApEnabled:      true,
			expectedPaymentSystems: []string{"ApplePay", "GooglePay", "VISA", "MasterCard"},
		},
	}
	for _, test := range agpayTests {
		t.Run("AGPayTest_"+test.name, func(t *testing.T) {
			paymentMethods := paymentMethodsFixture(params.paymentSystems)

			order := yapay.Order{
				PurchaseToken: params.purchaseToken,
				AcquirerType:  params.acquire,
				Environment:   params.payEnv,
				PaymentSystemsOptions: yapay.PaymentSystemsOptions{
					ApplePayEnabled:  test.partnerApEnabled,
					GooglePayEnabled: test.partnerGpEnabled,
				},
			}

			af := trust.AFPaymentMethods{}
			ps := trust.PaymentStatus{}
			ctx, sf := newPaymentServiceFixture(t)

			sf.Config.YaPaymentsServices = map[string]string{params.acquire: params.serviceToken}
			sf.Config.GooglePay.Enabled = test.configGpEnabled
			sf.Config.ApplePay.Enabled = test.configApEnabled
			sf.Config.ApplePay.PerService = map[string]bool{params.serviceToken: test.serviceApEnabled}
			sf.Config.GooglePay.PerService = map[string]bool{params.serviceToken: test.serviceGpEnabled}
			sf.Config.ApplePay.PerUID = map[uint64]bool{params.uid: test.uidApEnabled}
			sf.Config.GooglePay.PerUID = map[uint64]bool{params.uid: test.uidGpEnabled}
			sf.Config.PartnerPaymentOptionsEnabled = test.systemOptionsEnabled
			sf.Config.BoundPaymentMethodsWhitelist = []string{"card"}

			SetupInteractionMocks(sf, InteractionMockParams{
				isYaPay:          params.isYaPay,
				uid:              params.uid,
				payToken:         params.payToken,
				turboappID:       params.turboappID,
				email:            params.email,
				serviceToken:     params.serviceToken,
				purchaseToken:    params.purchaseToken,
				order:            order,
				paymentMethods:   paymentMethods,
				paymentStatus:    ps,
				afPaymentMethods: af,
			})

			res, err := sf.Service.InitPayment(ctx, params.uid, params.serviceToken, params.email, params.payToken, params.turboappID, params.credit)

			if err != nil {
				t.Fail()
				return
			}

			expectedPM := models.UserPaymentMethods{
				GooglePaySupported: test.expectedGpEnabled,
				ApplePaySupported:  test.expectedApEnabled,
				EnabledPaymentMethods: []models.EnabledPaymentMethods{
					{
						PaymentMethod:  "card",
						PaymentSystems: test.expectedPaymentSystems,
						Currency:       "RUB",
						FirmID:         1,
					},
				},
				PaymentMethods: []models.BoundPaymentMethod{
					res.PaymentMethods[0],
				},
			}

			require.Equal(t, expectedPM, res.UserPaymentMethods)
		})
	}

	cvvTests := []struct {
		name                  string
		sdkVersion            string
		forceCvv              string
		serviceExists         bool
		serviceCheckAntiFraud bool
		serviceVerifyCvv      bool
		afExists              bool
		afCvv                 bool
		expected              bool
		antiFraudCheckMode    AntiFraudCheckMode
	}{
		{
			name:               "NoSDKVersion",
			sdkVersion:         "",
			serviceExists:      true,
			serviceVerifyCvv:   false,
			expected:           true,
			antiFraudCheckMode: antiFraudAssertNotCalled,
		},
		{
			name:               "ForceCVV",
			sdkVersion:         "1.0.0",
			forceCvv:           "1",
			serviceExists:      true,
			serviceVerifyCvv:   false,
			expected:           true,
			antiFraudCheckMode: antiFraudAssertNotCalled,
		},
		{
			name:                  "DoNotCheckAntiFraud",
			sdkVersion:            "1.0.0",
			serviceExists:         true,
			serviceCheckAntiFraud: false,
			serviceVerifyCvv:      true,
			afExists:              true,
			afCvv:                 false,
			expected:              true,
			antiFraudCheckMode:    antiFraudAssertNotCalled,
		},
		{
			name:                  "CheckAntiFraud",
			sdkVersion:            "1.0.0",
			serviceExists:         true,
			serviceCheckAntiFraud: true,
			serviceVerifyCvv:      true,
			afExists:              true,
			afCvv:                 false,
			expected:              false,
			antiFraudCheckMode:    antiFraudAssertCalled,
		},
		{
			name:               "NoService",
			sdkVersion:         "1.0.0",
			serviceExists:      false,
			expected:           true,
			antiFraudCheckMode: antiFraudAssertNotCalled,
		},
		{
			name:                  "ServiceNo_AF_NoCVV",
			sdkVersion:            "1.0.0",
			serviceExists:         true,
			serviceCheckAntiFraud: false,
			serviceVerifyCvv:      false,
			expected:              false,
			antiFraudCheckMode:    antiFraudAssertNotCalled,
		},
	}
	for _, test := range cvvTests {
		t.Run("VerifyCVV_"+test.name, func(t *testing.T) {
			paymentMethods := paymentMethodsFixture(params.paymentSystems)

			order := yapay.Order{
				PurchaseToken:         params.purchaseToken,
				AcquirerType:          params.acquire,
				Environment:           params.payEnv,
				PaymentSystemsOptions: yapay.PaymentSystemsOptions{},
			}

			var afPaymentMethods []trust.AFBoundPaymentMethod
			if test.afExists {
				afPaymentMethods = []trust.AFBoundPaymentMethod{
					{CardID: paymentMethods.BoundPaymentMethods[0].CardID, NeedCvv: test.afCvv},
				}
			}
			af := trust.AFPaymentMethods{
				Status:         "success",
				PaymentMethods: afPaymentMethods,
			}
			ps := trust.PaymentStatus{}
			ctx, sf := newPaymentServiceFixture(t)
			sf.Config.BoundPaymentMethodsWhitelist = []string{"card"}

			ctx = ctxutil.WithSdkVersion(ctx, test.sdkVersion)
			ctx = ctxutil.WithForceCvv(ctx, test.forceCvv)

			sf.Config.YaPaymentsServices = map[string]string{params.acquire: params.serviceToken}
			if test.serviceExists {
				sf.Config.Services = []Service{
					{
						XServiceToken:  params.serviceToken,
						CheckAntiFraud: test.serviceCheckAntiFraud,
						VerifyCvv:      test.serviceVerifyCvv,
					},
				}
				sf.Config.serviceByToken = nil
				if sf.Config.buildServiceByToken() != nil {
					t.Fail()
					return
				}
			}

			SetupInteractionMocks(sf, InteractionMockParams{
				isYaPay:            params.isYaPay,
				uid:                params.uid,
				payToken:           params.payToken,
				turboappID:         params.turboappID,
				email:              params.email,
				serviceToken:       params.serviceToken,
				purchaseToken:      params.purchaseToken,
				order:              order,
				paymentMethods:     paymentMethods,
				paymentStatus:      ps,
				afPaymentMethods:   af,
				antiFraudCheckMode: test.antiFraudCheckMode,
			})

			res, err := sf.Service.InitPayment(ctx, params.uid, params.serviceToken, params.email, params.payToken, params.turboappID, params.credit)

			if err != nil {
				t.Fail()
				return
			}

			require.Equal(t, test.expected, res.UserPaymentMethods.PaymentMethods[0].VerifyCvv)
		})
	}
}
