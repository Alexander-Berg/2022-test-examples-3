package subscriptions

import (
	"context"
	"fmt"
	"reflect"
	"strconv"
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.uber.org/zap/zaptest"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/hotels/proto/data_config/promo"
	"a.yandex-team.ru/travel/library/go/sender"
	pb "a.yandex-team.ru/travel/notifier/api/subscriptions/v1"
	"a.yandex-team.ru/travel/notifier/internal/models"
	ordercommons "a.yandex-team.ru/travel/orders/proto"
	orderspromo "a.yandex-team.ru/travel/orders/proto/services/promo"
	proto1 "a.yandex-team.ru/travel/proto"
)

type fakeUserActionsLogger struct {
}

func (f *fakeUserActionsLogger) LogSubscribe(email, source, travelVertical, nationalVersion, language string, isPlusUser bool, experiments map[string]string, passportID, yandexUID string) error {
	return nil
}

func (f *fakeUserActionsLogger) LogUnsubscribe(email, nationalVersion, language string) error {
	return nil
}

func TestService(t *testing.T) {
	logger := zap.NewWithCore(zaptest.NewLogger(t).Core())
	unsubscribeListSlug := "slug"
	newService := func(
		repository *repositoryMock,
		hashGenerator *hashGeneratorMock,
		senderClient *senderClientMock,
		clock clockwork.Clock,
	) *Service {
		return NewService(
			logger,
			repository,
			&betterPriceRepositoryMock{},
			hashGenerator,
			senderClient,
			Config{SenderUnsubscribeListSlug: unsubscribeListSlug},
			clock,
			&fakeUserActionsLogger{},
			nil,
			nil,
			&schedulerMock{},
		)
	}
	ctx := context.Background()

	t.Run(
		"GetStatus/recipient doesn't exist returns ErrNoRecipient", func(t *testing.T) {
			repository := &repositoryMock{}
			email := "unknown@email.com"
			repository.On("Get", ctx, email).Return((*models.Recipient)(nil), nil)
			service := newService(repository, &hashGeneratorMock{}, &senderClientMock{}, clockwork.NewFakeClock())

			status, err := service.GetStatus(ctx, email)

			require.NoError(t, err)
			require.Empty(t, status)
			repository.AssertCalled(t, "Get", ctx, email)
		},
	)

	t.Run(
		"GetStatus/error during getting recipient returns error", func(t *testing.T) {
			repository := &repositoryMock{}
			email := "unknown@email.com"
			expectedErr := fmt.Errorf("some error")
			repository.On("Get", ctx, email).Return((*models.Recipient)(nil), expectedErr)
			service := newService(repository, &hashGeneratorMock{}, &senderClientMock{}, clockwork.NewFakeClock())

			status, err := service.GetStatus(ctx, email)

			require.Nil(t, status)
			require.Equal(t, expectedErr, err)
		},
	)

	t.Run(
		"GetStatus/existing recipient returns status", func(t *testing.T) {
			repository := &repositoryMock{}
			clock := clockwork.NewFakeClock()
			recipient := models.Recipient{
				Email:                ptr.String("recipient@email.com"),
				IsSubscribed:         true,
				SubscriptionSource:   ptr.String("source"),
				SubscriptionVertical: nil,
				SubscribedAt:         ptr.Time(clock.Now()),
				UnsubscribedAt:       nil,
			}
			expectedStatus := &models.SubscirptionStatus{
				IsSubscribed:   recipient.IsSubscribed,
				Source:         *recipient.SubscriptionSource,
				Vertical:       "",
				SubscribedAt:   clock.Now().Format(time.RFC3339),
				UnsubscribedAt: "",
			}
			repository.On("Get", ctx, recipient.GetEmail()).Return(&recipient, nil)
			service := newService(repository, &hashGeneratorMock{}, &senderClientMock{}, clock)

			status, err := service.GetStatus(ctx, recipient.GetEmail())

			require.NoError(t, err)
			require.Equal(t, *expectedStatus, *status)
		},
	)

	t.Run(
		"Subscribe/subscribes in sender and updates in database", func(t *testing.T) {
			repository := &repositoryMock{}
			clock := clockwork.NewFakeClock()
			senderClient := &senderClientMock{}
			hashGenerator := &hashGeneratorMock{}
			subscription := Subscription{
				Email:           "recipient@email.com",
				Source:          "home",
				Vertical:        "avia",
				Timezone:        "UTC",
				Language:        "ru",
				NationalVersion: "ru",
				YandexUID:       "yauid",
				PassportID:      "passid",
			}
			recipient := models.Recipient{Email: &subscription.Email}
			hashGenerator.On("Generate", recipient.GetEmail()).Return("hashed email")
			repository.On("GetOrCreateByEmail", ctx, recipient.GetEmail()).Return(&recipient, nil)
			senderRequest := sender.UnsubscribeListRequest{Email: recipient.GetEmail(), UnsubscribeListSlug: unsubscribeListSlug}
			senderClient.On("Subscribe", ctx, senderRequest).Return(nil)
			repository.On("Update", ctx, mock.Anything).Return(&recipient, nil)
			service := newService(repository, hashGenerator, senderClient, clock)

			err := service.Subscribe(ctx, subscription)

			require.NoError(t, err)
			repository.AssertCalled(t, "GetOrCreateByEmail", ctx, recipient.GetEmail())
			senderClient.AssertCalled(t, "Subscribe", ctx, senderRequest)
			repository.AssertCalled(t, "Update", ctx, recipient)
			require.True(t, recipient.IsSubscribed)
			require.Equal(t, "hashed email", *recipient.UnsubscribeHash)
			require.Equal(t, subscription.Vertical, *recipient.SubscriptionVertical)
			require.Equal(t, subscription.Source, *recipient.SubscriptionSource)
			require.Equal(t, subscription.NationalVersion, *recipient.NationalVersion)
			require.Equal(t, subscription.Language, *recipient.Language)
			require.Equal(t, subscription.Timezone, *recipient.Timezone)
			require.Equal(t, clock.Now(), *recipient.SubscribedAt)
		},
	)

	t.Run(
		"Subscribe/failed to get recipient returns error", func(t *testing.T) {
			repository := &repositoryMock{}
			email := "recipient@email.com"
			expectedErr := fmt.Errorf("some error")
			repository.On("GetOrCreateByEmail", ctx, email).Return((*models.Recipient)(nil), expectedErr)
			service := newService(repository, &hashGeneratorMock{}, &senderClientMock{}, clockwork.NewFakeClock())

			err := service.Subscribe(ctx, Subscription{Email: email})

			require.Equal(t, expectedErr, err)
			repository.AssertCalled(t, "GetOrCreateByEmail", ctx, email)
		},
	)

	t.Run(
		"Subscribe/failed to subscribe in sender returns error", func(t *testing.T) {
			repository := &repositoryMock{}
			clock := clockwork.NewFakeClock()
			senderClient := &senderClientMock{}
			recipient := models.Recipient{Email: ptr.String("recipient@email.com")}
			repository.On("GetOrCreateByEmail", ctx, recipient.GetEmail()).Return(&recipient, nil)
			senderRequest := sender.UnsubscribeListRequest{Email: recipient.GetEmail(), UnsubscribeListSlug: unsubscribeListSlug}
			expectedErr := fmt.Errorf("some error")
			senderClient.On("Subscribe", ctx, senderRequest).Return(expectedErr)
			service := newService(repository, &hashGeneratorMock{}, senderClient, clock)

			err := service.Subscribe(ctx, Subscription{Email: recipient.GetEmail()})

			require.Equal(t, expectedErr, err)
			repository.AssertCalled(t, "GetOrCreateByEmail", ctx, recipient.GetEmail())
			senderClient.AssertCalled(t, "Subscribe", ctx, senderRequest)
			repository.AssertNotCalled(t, "Update", ctx, recipient)
			require.False(t, recipient.IsSubscribed)
		},
	)

	t.Run(
		"Subscribe/failed to update in database returns error", func(t *testing.T) {
			repository := &repositoryMock{}
			clock := clockwork.NewFakeClock()
			senderClient := &senderClientMock{}
			hashGenerator := &hashGeneratorMock{}
			recipient := models.Recipient{Email: ptr.String("recipient@email.com")}
			vertical := "avia"
			source := "home"
			hashGenerator.On("Generate", recipient.GetEmail()).Return("hashed email")
			repository.On("GetOrCreateByEmail", ctx, recipient.GetEmail()).Return(&recipient, nil)
			senderRequest := sender.UnsubscribeListRequest{Email: recipient.GetEmail(), UnsubscribeListSlug: unsubscribeListSlug}
			senderClient.On("Subscribe", ctx, senderRequest).Return(nil)
			expectedErr := fmt.Errorf("some error")
			repository.On("Update", ctx, mock.Anything).Return((*models.Recipient)(nil), expectedErr)
			service := newService(repository, hashGenerator, senderClient, clock)

			err := service.Subscribe(ctx, Subscription{Email: recipient.GetEmail(), Vertical: vertical, Source: source})

			require.Equal(t, expectedErr, err)
			repository.AssertCalled(t, "GetOrCreateByEmail", ctx, recipient.GetEmail())
			senderClient.AssertCalled(t, "Subscribe", ctx, senderRequest)
			repository.AssertCalled(t, "Update", ctx, recipient)
		},
	)

	t.Run(
		"Unsubscribe/unsubscribes in sender and updates in database", func(t *testing.T) {
			repository := &repositoryMock{}
			clock := clockwork.NewFakeClock()
			senderClient := &senderClientMock{}
			recipient := models.Recipient{
				Email:           ptr.String("recipient@email.com"),
				IsSubscribed:    true,
				UnsubscribedAt:  nil,
				UnsubscribeHash: ptr.String("hash"),
			}
			repository.On("GetByHash", ctx, *recipient.UnsubscribeHash).Return(&recipient, nil)
			senderRequest := sender.UnsubscribeListRequest{Email: recipient.GetEmail(), UnsubscribeListSlug: unsubscribeListSlug}
			senderClient.On("Unsubscribe", ctx, senderRequest).Return(nil)
			repository.On("Update", ctx, mock.Anything).Return(&recipient, nil)
			service := newService(repository, &hashGeneratorMock{}, senderClient, clock)

			err := service.Unsubscribe(ctx, *recipient.UnsubscribeHash)

			require.NoError(t, err)
			repository.AssertCalled(t, "GetByHash", ctx, *recipient.UnsubscribeHash)
			senderClient.AssertCalled(t, "Unsubscribe", ctx, senderRequest)
			repository.AssertCalled(t, "Update", ctx, recipient)
			require.False(t, recipient.IsSubscribed)
			require.Equal(t, clock.Now(), *recipient.UnsubscribedAt)
		},
	)

	t.Run(
		"Subscribe/failed to get recipient returns error", func(t *testing.T) {
			repository := &repositoryMock{}
			hash := "hash"
			expectedErr := fmt.Errorf("some error")
			repository.On("GetByHash", ctx, hash).Return((*models.Recipient)(nil), expectedErr)
			service := newService(repository, &hashGeneratorMock{}, &senderClientMock{}, clockwork.NewFakeClock())

			err := service.Unsubscribe(ctx, hash)

			require.Equal(t, expectedErr, err)
			repository.AssertCalled(t, "GetByHash", ctx, hash)
		},
	)

	t.Run(
		"Unsubscribe/failed to unsubscribe in sender returns error", func(t *testing.T) {
			repository := &repositoryMock{}
			clock := clockwork.NewFakeClock()
			senderClient := &senderClientMock{}
			recipient := models.Recipient{UnsubscribeHash: ptr.String("hash"), IsSubscribed: true}
			repository.On("GetByHash", ctx, *recipient.UnsubscribeHash).Return(&recipient, nil)
			senderRequest := sender.UnsubscribeListRequest{Email: recipient.GetEmail(), UnsubscribeListSlug: unsubscribeListSlug}
			expectedErr := fmt.Errorf("some error")
			senderClient.On("Unsubscribe", ctx, senderRequest).Return(expectedErr)
			service := newService(repository, &hashGeneratorMock{}, senderClient, clock)

			err := service.Unsubscribe(ctx, *recipient.UnsubscribeHash)

			require.Equal(t, expectedErr, err)
			repository.AssertCalled(t, "GetByHash", ctx, *recipient.UnsubscribeHash)
			senderClient.AssertCalled(t, "Unsubscribe", ctx, senderRequest)
			repository.AssertNotCalled(t, "Update", ctx, recipient)
			require.True(t, recipient.IsSubscribed)
		},
	)

	t.Run(
		"Unsubscribe/failed to update in database returns error", func(t *testing.T) {
			repository := &repositoryMock{}
			clock := clockwork.NewFakeClock()
			senderClient := &senderClientMock{}
			recipient := models.Recipient{UnsubscribeHash: ptr.String("hash")}
			repository.On("GetByHash", ctx, *recipient.UnsubscribeHash).Return(&recipient, nil)
			senderRequest := sender.UnsubscribeListRequest{Email: recipient.GetEmail(), UnsubscribeListSlug: unsubscribeListSlug}
			senderClient.On("Unsubscribe", ctx, senderRequest).Return(nil)
			expectedErr := fmt.Errorf("some error")
			repository.On("Update", ctx, mock.Anything).Return((*models.Recipient)(nil), expectedErr)
			service := newService(repository, &hashGeneratorMock{}, senderClient, clock)

			err := service.Unsubscribe(ctx, *recipient.UnsubscribeHash)

			require.Equal(t, expectedErr, err)
			repository.AssertCalled(t, "GetByHash", ctx, *recipient.UnsubscribeHash)
			senderClient.AssertCalled(t, "Unsubscribe", ctx, senderRequest)
			repository.AssertCalled(t, "Update", ctx, recipient)
		},
	)
}

type repositoryMock struct {
	mock.Mock
}

type betterPriceRepositoryMock struct {
	mock.Mock
	BetterPriceSubscriptionsRepository
}

func (r *repositoryMock) Get(ctx context.Context, email string) (*models.Recipient, error) {
	args := r.Called(ctx, email)
	return args.Get(0).(*models.Recipient), args.Error(1)
}

func (r *repositoryMock) GetOrCreateByEmail(ctx context.Context, email string) (*models.Recipient, error) {
	args := r.Called(ctx, email)
	return args.Get(0).(*models.Recipient), args.Error(1)
}

func (r *repositoryMock) Update(ctx context.Context, recipient models.Recipient) (*models.Recipient, error) {
	args := r.Called(ctx, recipient)
	return args.Get(0).(*models.Recipient), args.Error(1)
}

func (r *repositoryMock) GetByHash(ctx context.Context, hash string) (*models.Recipient, error) {
	args := r.Called(ctx, hash)
	return args.Get(0).(*models.Recipient), args.Error(1)
}

type hashGeneratorMock struct {
	mock.Mock
}

func (p *hashGeneratorMock) Generate(email string) string {
	args := p.Called(email)
	return args.String(0)
}

type senderClientMock struct {
	mock.Mock
}

func (s *senderClientMock) SendTransactional(ctx context.Context, sendRequest sender.TransactionalRequest) ([]byte, error) {
	panic("implement me")
}

func (s *senderClientMock) Unsubscribe(ctx context.Context, request sender.UnsubscribeListRequest) error {
	args := s.Called(ctx, request)
	return args.Error(0)
}

func (s *senderClientMock) Subscribe(ctx context.Context, request sender.UnsubscribeListRequest) error {
	args := s.Called(ctx, request)
	return args.Error(0)
}

type schedulerMock struct {
	mock.Mock
}

func (s *schedulerMock) Schedule(ctx context.Context, notification models.Notification, now time.Time) (*models.Notification, error) {
	args := s.Called(ctx, notification, now)
	return args.Get(0).(*models.Notification), args.Error(1)
}

func TestRemoveEmail(t *testing.T) {
	testCases := []struct {
		input    string
		expected string
	}{
		{
			input:    "https://sender.yandex-team.ru/api/0/travel/unsubscribe/list/4FZH5022-3A91?email=a1.b2.c3%40yandex.ru",
			expected: "https://sender.yandex-team.ru/api/0/travel/unsubscribe/list/4FZH5022-3A91?email=HIDDEN_EMAIL",
		},
		{
			input:    "https://sender.yandex-team.ru/api/0/travel/unsubscribe/list/4FZH5022-3A91?email=a1.b2.c3%40yandex.ru&arg1=1&arg2&arg3=val3",
			expected: "https://sender.yandex-team.ru/api/0/travel/unsubscribe/list/4FZH5022-3A91?email=HIDDEN_EMAIL&arg1=1&arg2&arg3=val3",
		},
		{
			input:    "some text before https://sender.yandex-team.ru/api/0/travel/unsubscribe/list/4FZH5022-3A91?email=a1.b2.c3%40yandex.ru&arg1=1&arg2&arg3=val3 some text after",
			expected: "some text before https://sender.yandex-team.ru/api/0/travel/unsubscribe/list/4FZH5022-3A91?email=HIDDEN_EMAIL&arg1=1&arg2&arg3=val3 some text after",
		},
		{
			input:    "error while requesting Sender: Delete \"https://sender.yandex-team.ru/api/0/travel/unsubscribe/list/4FZH5022-3A91?email=abc.def%40yandex.ru\": context deadline exceeded",
			expected: "error while requesting Sender: Delete \"https://sender.yandex-team.ru/api/0/travel/unsubscribe/list/4FZH5022-3A91?email=HIDDEN_EMAIL\": context deadline exceeded",
		},
	}
	for _, tc := range testCases {
		t.Run(
			"removeEmail from error message", func(t *testing.T) {
				require.Equal(t, tc.expected, removeEmail(tc.input))
			},
		)
	}
}

func TestPromoCodeNominalTypeIsInSync(t *testing.T) {
	for k, v := range pb.EPromoCodeNominalType_name {
		if _, exists := ordercommons.EPromoCodeNominalType_name[k]; !exists {
			t.Errorf("Please, sync commons.EPromoCodeNominalType "+
				"at a.yandex-team.ru/travel/orders/proto."+
				" It does not contain value for index %d with name %s", k, v)
		}
	}
	for k, v := range ordercommons.EPromoCodeNominalType_name {
		if _, exists := pb.EPromoCodeNominalType_name[k]; !exists {
			t.Errorf("Please, sync subscriptions.EPromoCodeNominalType "+
				"at a.yandex-team.ru/travel/notifier/api/subscriptions/v1."+
				" It does not contain value for index %d with name %s", k, v)
		}
	}
}

type mockPromoEventsRepository struct {
	mock.Mock
}

func (m *mockPromoEventsRepository) GetAll() ([]*promo.TPromoEvent, error) {
	c := m.Mock.MethodCalled("GetAll")
	return c.Get(0).([]*promo.TPromoEvent), c.Error(1)

}
func s(s string) *string { return &s }
func b(b bool) *bool     { return &b }

type mockPromoCampaignsRepository struct {
	mock.Mock
}

func (m *mockPromoCampaignsRepository) CreatePromoCode(
	ctx context.Context,
	promoCampaignID string,
	code string,
	validTill time.Time,
	promoActionDetails *orderspromo.TGetPromoActionResp,
) (
	*orderspromo.TCreatePromoCodeRsp, error,
) {
	c := m.Mock.MethodCalled("CreatePromoCode", s)
	v := c.Get(0)
	var resp *orderspromo.TCreatePromoCodeRsp
	if v == nil {
		resp = nil
	} else {
		resp = v.(*orderspromo.TCreatePromoCodeRsp)
	}
	return resp, c.Error(1)
}

func (m *mockPromoCampaignsRepository) GetPromoActionDetails(ctx context.Context, s string) (response *orderspromo.TGetPromoActionResp, orderErr error) {
	c := m.Mock.MethodCalled("GetPromoActionDetails", s)
	v := c.Get(0)
	var resp *orderspromo.TGetPromoActionResp
	if v == nil {
		resp = nil
	} else {
		resp = v.(*orderspromo.TGetPromoActionResp)
	}
	return resp, c.Error(1)
}

func (m *mockPromoCampaignsRepository) PromoCodeActivationAvailable(ctx context.Context, code string) (
	*orderspromo.TPromoCodeActivationAvailableResp, error,
) {
	c := m.Mock.MethodCalled("PromoCodeActivationAvailable", s)
	v := c.Get(0)
	var resp *orderspromo.TPromoCodeActivationAvailableResp
	if v == nil {
		resp = nil
	} else {
		resp = v.(*orderspromo.TPromoCodeActivationAvailableResp)
	}
	return resp, c.Error(1)
}

func TestService_GetPromoConfig(t *testing.T) {
	campaigns := &mockPromoCampaignsRepository{}
	campaigns.On("GetPromoActionDetails", "campaign1").Return(&orderspromo.TGetPromoActionResp{
		DiscountApplicationConfig: &orderspromo.TDiscountApplicationConfig{
			MinTotalCost: &proto1.TPrice{
				Currency:  0,
				Amount:    178200,
				Precision: 2,
			},
		},
		GenerationConfig: &orderspromo.TGenerationConfig{
			Nominal:     1000,
			NominalType: ordercommons.EPromoCodeNominalType_NT_VALUE,
		},
	}, nil)

	campaigns.On("GetPromoActionDetails", "campaign2").Return(&orderspromo.TGetPromoActionResp{
		GenerationConfig: &orderspromo.TGenerationConfig{
			Nominal:     2000,
			NominalType: ordercommons.EPromoCodeNominalType_NT_VALUE,
		},
	}, nil)

	campaigns.On("GetPromoActionDetails", "campaign3").Return(nil, fmt.Errorf(""))

	type args struct {
		vertical string
		plusUser bool
		campaign string
		enabled  bool
	}
	type want struct {
		value *pb.GetPromoConfigRsp
		error error
	}
	tests := []struct {
		args args
		want want
	}{
		{
			args: args{
				vertical: "avia",
				plusUser: false,
				campaign: "campaign1",
				enabled:  true,
			},
			want: want{
				value: &pb.GetPromoConfigRsp{
					PromoCode: &pb.PromoCodeForSubscription{
						Type:         pb.EPromoCodeNominalType_fix,
						Amount:       1000,
						MinTotalCost: 1782.,
					},
				},
			},
		},
		{
			args: args{
				vertical: "hotels",
				plusUser: false,
				campaign: "campaign1",
				enabled:  true,
			},
			want: want{
				value: &pb.GetPromoConfigRsp{},
			},
		},
		{
			args: args{
				vertical: "avia",
				plusUser: false,
				campaign: "campaign2",
				enabled:  true,
			},
			want: want{
				value: &pb.GetPromoConfigRsp{
					PromoCode: &pb.PromoCodeForSubscription{
						Type:         pb.EPromoCodeNominalType_fix,
						Amount:       2000,
						MinTotalCost: 0,
					},
				},
			},
		},
		{
			args: args{
				vertical: "avia",
				plusUser: false,
				campaign: "campaign3",
				enabled:  true,
			},
			want: want{
				error: fmt.Errorf("campaign=campaign3: "),
			},
		},
		{
			args: args{
				vertical: "avia",
				plusUser: false,
				campaign: "campaign1",
				enabled:  false,
			},
			want: want{
				value: &pb.GetPromoConfigRsp{},
			},
		},
	}
	for i, test := range tests {
		t.Run(strconv.Itoa(i), func(t *testing.T) {

			events := &mockPromoEventsRepository{}
			service := &Service{
				promoEventsRepository:    events,
				promoCampaignsRepository: campaigns,
			}
			events.On("GetAll").Return([]*promo.TPromoEvent{
				{
					OrdersCampaignId: &test.args.campaign,
					Enabled:          &test.args.enabled,
					Verticals:        []string{"avia"},
				},
			}, nil)

			claim := assert.New(t)
			ctx := context.Background()
			config, err := service.GetPromoConfig(ctx, test.args.vertical)
			if err != nil && test.want.error == nil ||
				err == nil && test.want.error != nil ||
				err != nil && test.want.error != nil && err.Error() != test.want.error.Error() {
				t.Fatalf("Unexpected error. Expected = %v, actual = %v", test.want.error, err)
			}
			claim.Equal(test.want.value, config)
		})
	}
}

func TestService_getCode(t *testing.T) {
	type fields struct {
		config                             Config
		clock                              clockwork.Clock
		logger                             log.Logger
		recipientsRepository               RecipientsRepository
		betterPriceSubscriptionsRepository BetterPriceSubscriptionsRepository
		unsubscirbeHashGenerator           UnsubscribeHashGenerator
		senderClient                       sender.Client
		userActionsLogger                  UserActionsLogger
		promoEventsRepository              PromoEventsRepository
		promoCampaignsRepository           PromoCampaignsRepository
		notificationsScheduler             Scheduler
	}
	type args struct {
		id string
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   string
	}{
		{
			args: args{"12345678"},
			want: "HELLO-9AE0DAAF",
		},
		{
			args: args{""},
			want: "HELLO-00000000",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &Service{
				config:                             tt.fields.config,
				clock:                              tt.fields.clock,
				logger:                             tt.fields.logger,
				recipientsRepository:               tt.fields.recipientsRepository,
				betterPriceSubscriptionsRepository: tt.fields.betterPriceSubscriptionsRepository,
				unsubscirbeHashGenerator:           tt.fields.unsubscirbeHashGenerator,
				senderClient:                       tt.fields.senderClient,
				userActionsLogger:                  tt.fields.userActionsLogger,
				promoEventsRepository:              tt.fields.promoEventsRepository,
				promoCampaignsRepository:           tt.fields.promoCampaignsRepository,
				notificationsScheduler:             tt.fields.notificationsScheduler,
			}
			if got := s.getCode(tt.args.id); got != tt.want {
				t.Errorf("getCode() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestService_getNominal(t *testing.T) {
	type fields struct {
		config                             Config
		clock                              clockwork.Clock
		logger                             log.Logger
		recipientsRepository               RecipientsRepository
		betterPriceSubscriptionsRepository BetterPriceSubscriptionsRepository
		unsubscirbeHashGenerator           UnsubscribeHashGenerator
		senderClient                       sender.Client
		userActionsLogger                  UserActionsLogger
		promoEventsRepository              PromoEventsRepository
		promoCampaignsRepository           PromoCampaignsRepository
		notificationsScheduler             Scheduler
	}
	type args struct {
		nominalType  ordercommons.EPromoCodeNominalType
		nominalValue int
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    string
		wantErr bool
	}{
		{
			args:    args{ordercommons.EPromoCodeNominalType_NT_VALUE, 100},
			want:    "100 ₽",
			wantErr: false,
		},
		{
			args:    args{ordercommons.EPromoCodeNominalType_NT_PERCENT, 10},
			want:    "10%",
			wantErr: false,
		},
		{
			args:    args{ordercommons.EPromoCodeNominalType_NT_UNKNOWN, 0},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := Service{
				config:                             tt.fields.config,
				clock:                              tt.fields.clock,
				logger:                             tt.fields.logger,
				recipientsRepository:               tt.fields.recipientsRepository,
				betterPriceSubscriptionsRepository: tt.fields.betterPriceSubscriptionsRepository,
				unsubscirbeHashGenerator:           tt.fields.unsubscirbeHashGenerator,
				senderClient:                       tt.fields.senderClient,
				userActionsLogger:                  tt.fields.userActionsLogger,
				promoEventsRepository:              tt.fields.promoEventsRepository,
				promoCampaignsRepository:           tt.fields.promoCampaignsRepository,
				notificationsScheduler:             tt.fields.notificationsScheduler,
			}
			got, err := s.getNominal(tt.args.nominalType, tt.args.nominalValue)
			if (err != nil) != tt.wantErr {
				t.Errorf("getNominal() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("getNominal() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestService_getPriceValue(t *testing.T) {
	type fields struct {
		config                             Config
		clock                              clockwork.Clock
		logger                             log.Logger
		recipientsRepository               RecipientsRepository
		betterPriceSubscriptionsRepository BetterPriceSubscriptionsRepository
		unsubscirbeHashGenerator           UnsubscribeHashGenerator
		senderClient                       sender.Client
		userActionsLogger                  UserActionsLogger
		promoEventsRepository              PromoEventsRepository
		promoCampaignsRepository           PromoCampaignsRepository
		notificationsScheduler             Scheduler
	}
	type args struct {
		price *proto1.TPrice
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    int
		wantErr bool
	}{
		{
			args: args{&proto1.TPrice{Currency: proto1.ECurrency_C_RUB, Amount: 1000, Precision: 0}},
			want: 1000,
		},
		{
			args: args{&proto1.TPrice{Currency: proto1.ECurrency_C_RUB, Amount: 1000, Precision: 1}},
			want: 100,
		},
		{
			args: args{&proto1.TPrice{Currency: proto1.ECurrency_C_RUB, Amount: 1000, Precision: 2}},
			want: 10,
		},
		{
			args:    args{&proto1.TPrice{Currency: proto1.ECurrency_C_USD, Amount: 1000, Precision: 0}},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &Service{
				config:                             tt.fields.config,
				clock:                              tt.fields.clock,
				logger:                             tt.fields.logger,
				recipientsRepository:               tt.fields.recipientsRepository,
				betterPriceSubscriptionsRepository: tt.fields.betterPriceSubscriptionsRepository,
				unsubscirbeHashGenerator:           tt.fields.unsubscirbeHashGenerator,
				senderClient:                       tt.fields.senderClient,
				userActionsLogger:                  tt.fields.userActionsLogger,
				promoEventsRepository:              tt.fields.promoEventsRepository,
				promoCampaignsRepository:           tt.fields.promoCampaignsRepository,
				notificationsScheduler:             tt.fields.notificationsScheduler,
			}
			got, err := s.getPriceValueRounded(tt.args.price)
			if (err != nil) != tt.wantErr {
				t.Errorf("getPriceValueRounded() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("getPriceValueRounded() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestService_getPromoSendArgs(t *testing.T) {
	type fields struct {
		config                             Config
		clock                              clockwork.Clock
		logger                             log.Logger
		recipientsRepository               RecipientsRepository
		betterPriceSubscriptionsRepository BetterPriceSubscriptionsRepository
		unsubscirbeHashGenerator           UnsubscribeHashGenerator
		senderClient                       sender.Client
		userActionsLogger                  UserActionsLogger
		promoEventsRepository              PromoEventsRepository
		promoCampaignsRepository           PromoCampaignsRepository
		notificationsScheduler             Scheduler
	}
	type args struct {
		promoCode models.PromoCode
	}

	clock := clockwork.NewFakeClock()

	tests := []struct {
		name   string
		fields fields
		args   args
		want   map[string]string
	}{
		{
			args: args{promoCode: models.PromoCode{
				Code:                   "SOME-CODE",
				Nominal:                "10%",
				AddsUpWithOtherActions: false,
				MinTotalCost:           100,
				ValidTill:              clock.Now(),
			}},
			want: map[string]string{
				"code":           "SOME-CODE",
				"nominal":        "10%",
				"min_total_cost": "100 ₽",
				"valid_till":     clock.Now().Add(-24 * time.Hour).Format("2006-01-02"),
			},
		},
		{
			args: args{promoCode: models.PromoCode{
				Code:                   "ANOTHER-CODE",
				Nominal:                "100 ₽",
				AddsUpWithOtherActions: true,
				MinTotalCost:           1000,
				ValidTill:              clock.Now(),
			}},
			want: map[string]string{
				"code":                       "ANOTHER-CODE",
				"nominal":                    "100 ₽",
				"min_total_cost":             "1000 ₽",
				"valid_till":                 clock.Now().Add(-24 * time.Hour).Format("2006-01-02"),
				"adds_up_with_other_actions": "true",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &Service{
				config:                             tt.fields.config,
				clock:                              tt.fields.clock,
				logger:                             tt.fields.logger,
				recipientsRepository:               tt.fields.recipientsRepository,
				betterPriceSubscriptionsRepository: tt.fields.betterPriceSubscriptionsRepository,
				unsubscirbeHashGenerator:           tt.fields.unsubscirbeHashGenerator,
				senderClient:                       tt.fields.senderClient,
				userActionsLogger:                  tt.fields.userActionsLogger,
				promoEventsRepository:              tt.fields.promoEventsRepository,
				promoCampaignsRepository:           tt.fields.promoCampaignsRepository,
				notificationsScheduler:             tt.fields.notificationsScheduler,
			}
			if got := s.getPromoSendArgs(tt.args.promoCode); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getPromoSendArgs() = %v, want %v", got, tt.want)
			}
		})
	}
}
