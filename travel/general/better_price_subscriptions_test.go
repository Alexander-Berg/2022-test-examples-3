package database

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"
	hasql "golang.yandex/hasql/sqlx"
	"gorm.io/gorm"

	"a.yandex-team.ru/travel/notifier/internal/models"
	subscriptionsmodels "a.yandex-team.ru/travel/notifier/internal/service/subscriptions/models"
)

func TestBetterPriceSubscriptionRepository(t *testing.T) {
	testPgClient := getTestPgClient()
	repository := NewBetterPriceSubscriptionsRepository(testPgClient, true)
	dbOperationTimeout := getDBTestTimeout()
	withEmptyDB := newDBCleaner(testPgClient)

	getRecipient := func(email string) (recipient models.Recipient) {
		_ = testPgClient.ExecuteInTransaction(
			hasql.Primary, func(db *gorm.DB) error {
				return db.Model(recipient).FirstOrCreate(&recipient, models.Recipient{Email: &email}).Error
			},
		)
		return
	}

	type subscriptionOption func(subscription *models.BetterPriceSubscription)

	withPassengers := func(adults, children, infants uint32) subscriptionOption {
		return func(s *models.BetterPriceSubscription) {
			s.Adults = adults
			s.Children = children
			s.Infants = infants
		}
	}

	withServiceClass := func(class subscriptionsmodels.ServiceClass) subscriptionOption {
		return func(s *models.BetterPriceSubscription) {
			s.ServiceClass = class.String()
		}
	}

	withBaggage := func(withBaggage bool) subscriptionOption {
		return func(s *models.BetterPriceSubscription) {
			s.WithBaggage = withBaggage
		}
	}

	buildSubscription := func(
		recipient models.Recipient,
		variant models.Variant,
		options ...subscriptionOption,
	) models.BetterPriceSubscription {
		subscription := models.BetterPriceSubscription{
			Recipient:                            recipient,
			Variant:                              variant,
			PriceWithoutBaggageValue:             123,
			PriceWithoutBaggageCurrency:          "RUB",
			ExchangedPriceWithoutBaggageValue:    0,
			ExchangedPriceWithoutBaggageCurrency: "",
			PriceWithBaggageValue:                0,
			PriceWithBaggageCurrency:             "",
			ExchangedPriceWithBaggageValue:       0,
			ExchangedPriceWithBaggageCurrency:    "",
			FromPointKey:                         "",
			ToPointKey:                           "",
			DateForward:                          "",
			DateBackward:                         "",
			NationalVersion:                      "",
			Language:                             "",
			Adults:                               0,
			Children:                             0,
			Infants:                              0,
			ServiceClass:                         subscriptionsmodels.ServiceClassEconomy.String(),
			WithBaggage:                          false,
		}
		for _, opt := range options {
			opt(&subscription)
		}
		return subscription
	}

	t.Run(
		"Upsert", withEmptyDB(
			func(t *testing.T) {
				testCases := []struct {
					name string

					subscriptions []models.BetterPriceSubscription

					expectedSubscriptions int
					expectedRecipients    int
					expectedVariants      int
				}{
					{
						name: "same recipient same variant",
						subscriptions: []models.BetterPriceSubscription{
							buildSubscription(getRecipient("email"), models.Variant{ForwardKey: "1", BackwardKey: "2"}),
							buildSubscription(getRecipient("email"), models.Variant{ForwardKey: "1", BackwardKey: "2"}),
						},
						expectedSubscriptions: 1,
						expectedRecipients:    1,
						expectedVariants:      1,
					},
					{
						name: "same recipient different variants",
						subscriptions: []models.BetterPriceSubscription{
							buildSubscription(getRecipient("email"), models.Variant{ForwardKey: "1", BackwardKey: "2"}),
							buildSubscription(getRecipient("email"), models.Variant{ForwardKey: "1", BackwardKey: "3"}),
							buildSubscription(getRecipient("email"), models.Variant{ForwardKey: "4", BackwardKey: "2"}),
						},
						expectedSubscriptions: 3,
						expectedRecipients:    1,
						expectedVariants:      3,
					},
					{
						name: "different recipient same variant",
						subscriptions: []models.BetterPriceSubscription{
							buildSubscription(getRecipient("email1"), models.Variant{ForwardKey: "1", BackwardKey: "2"}),
							buildSubscription(getRecipient("email2"), models.Variant{ForwardKey: "1", BackwardKey: "2"}),
						},
						expectedSubscriptions: 2,
						expectedRecipients:    2,
						expectedVariants:      1,
					},
					{
						name: "different recipient different variants",
						subscriptions: []models.BetterPriceSubscription{
							buildSubscription(getRecipient("email1"), models.Variant{ForwardKey: "1", BackwardKey: "2"}),
							buildSubscription(getRecipient("email2"), models.Variant{ForwardKey: "1", BackwardKey: "3"}),
						},
						expectedSubscriptions: 2,
						expectedRecipients:    2,
						expectedVariants:      2,
					},
					{
						name: "different recipient different variants",
						subscriptions: []models.BetterPriceSubscription{
							buildSubscription(getRecipient("email1"), models.Variant{ForwardKey: "1", BackwardKey: "2"}),
							buildSubscription(getRecipient("email2"), models.Variant{ForwardKey: "1", BackwardKey: "3"}),
						},
						expectedSubscriptions: 2,
						expectedRecipients:    2,
						expectedVariants:      2,
					},
					{
						name: "same recipient same variant different service class",
						subscriptions: []models.BetterPriceSubscription{
							buildSubscription(getRecipient("email"), models.Variant{ForwardKey: "1", BackwardKey: "2"}),
							buildSubscription(
								getRecipient("email"),
								models.Variant{ForwardKey: "1", BackwardKey: "2"},
								withServiceClass(subscriptionsmodels.ServiceClassBusiness),
							),
						},
						expectedSubscriptions: 2,
						expectedRecipients:    1,
						expectedVariants:      1,
					},
					{
						name: "same recipient same variant different passengers",
						subscriptions: []models.BetterPriceSubscription{
							buildSubscription(
								getRecipient("email"),
								models.Variant{ForwardKey: "1", BackwardKey: "2"},
								withPassengers(1, 0, 0),
							),
							buildSubscription(
								getRecipient("email"),
								models.Variant{ForwardKey: "1", BackwardKey: "2"},
								withPassengers(1, 1, 0),
							),
							buildSubscription(
								getRecipient("email"),
								models.Variant{ForwardKey: "1", BackwardKey: "2"},
								withPassengers(1, 1, 1),
							),
						},
						expectedSubscriptions: 3,
						expectedRecipients:    1,
						expectedVariants:      1,
					},
					{
						name: "same recipient same variant with and without baggage",
						subscriptions: []models.BetterPriceSubscription{
							buildSubscription(
								getRecipient("email"),
								models.Variant{ForwardKey: "1", BackwardKey: "2"},
								withBaggage(true),
							),
							buildSubscription(
								getRecipient("email"),
								models.Variant{ForwardKey: "1", BackwardKey: "2"},
								withBaggage(false),
							),
						},
						expectedSubscriptions: 2,
						expectedRecipients:    1,
						expectedVariants:      1,
					},
				}

				for _, testCase := range testCases {
					t.Run(
						testCase.name, withEmptyDB(
							func(t *testing.T) {
								ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
								defer cancelFunc()

								for _, subscription := range testCase.subscriptions {
									err := repository.Upsert(ctx, subscription)
									require.NoError(t, err)
								}

								var subscriptionsCount int64
								var recipientsCount int64
								var variantsCount int64
								err := testPgClient.ExecuteInTransaction(
									hasql.Alive, func(db *gorm.DB) error {
										if err := db.Model(models.Recipient{}).Count(&recipientsCount).Error; err != nil {
											return err
										}
										if err := db.Model(models.Variant{}).Count(&variantsCount).Error; err != nil {
											return err
										}
										return db.Model(models.BetterPriceSubscription{}).Count(&subscriptionsCount).Error
									},
								)

								require.NoError(t, err)
								require.EqualValues(t, testCase.expectedSubscriptions, subscriptionsCount)
								require.EqualValues(t, testCase.expectedVariants, variantsCount)
								require.EqualValues(t, testCase.expectedRecipients, recipientsCount)
							},
						),
					)
				}
			},
		),
	)
}
