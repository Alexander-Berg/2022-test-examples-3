package pretrip

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/gofrs/uuid"
	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/library/go/core/xerrors"
	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/library/go/renderer"
	"a.yandex-team.ru/travel/library/go/sender"
	"a.yandex-team.ru/travel/notifier/internal/database"
	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/orders"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/blocks"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/interfaces"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/logging/renderlog/record"
)

const isoDateFormat = "2006-01-02"

type fakeNotificationTransaction struct{}

func (t *fakeNotificationTransaction) Update(notification models.Notification, logger log.Logger) error {
	return nil
}

func (t *fakeNotificationTransaction) Commit(log.Logger) error {
	return nil
}

func (t *fakeNotificationTransaction) Rollback(log.Logger) error {
	return nil
}

type fakeNotificationsRepository struct {
	notifications     map[uint64]models.Notification
	alwaysReturnError bool
}

func (r *fakeNotificationsRepository) ChangeStatus(
	_ context.Context, notification models.Notification, status models.NotificationStatus,
) error {
	notification = r.notifications[notification.ID]
	notification.Status = status
	r.notifications[notification.ID] = notification
	return nil
}

func (r *fakeNotificationsRepository) MarkTemporarilyFailed(_ context.Context, notification models.Notification) error {
	notification = r.notifications[notification.ID]
	notification.Failures++
	r.notifications[notification.ID] = notification
	return nil
}

func (r *fakeNotificationsRepository) GetForRecipient(
	_ context.Context, recipientID int32, from time.Time, until time.Time,
) ([]models.Notification, error) {
	result := make([]models.Notification, 0)
	if r.alwaysReturnError {
		return result, fmt.Errorf("repository error")
	}
	for _, value := range r.notifications {
		if value.NotifyAt.IsZero() {
			continue
		}
		if value.NotifyAt.Before(from) || value.NotifyAt.After(until) {
			continue
		}
		if *value.RecipientID == recipientID {
			result = append(result, value)
		}
	}
	return result, nil
}

func (r *fakeNotificationsRepository) AlreadySentForOrder(_ context.Context, orderID string, notificationType models.NotificationType) ([]models.Notification, error) {
	result := make([]models.Notification, 0)
	if r.alwaysReturnError {
		return result, fmt.Errorf("repository error")
	}
	for _, value := range r.notifications {
		if value.OrderID != nil && *value.OrderID == orderID {
			result = append(result, value)
		}
	}
	return result, nil
}

func (r *fakeNotificationsRepository) Get(id uint64) models.Notification {
	return r.notifications[id]
}

func (r *fakeNotificationsRepository) Put(notification models.Notification) {
	r.notifications[notification.ID] = notification
}

func (r *fakeNotificationsRepository) SetAlwaysReturnError(alwaysReturnError bool) {
	r.alwaysReturnError = alwaysReturnError
}

func (r *fakeNotificationsRepository) BeginTransaction(
	context.Context,
	models.Notification,
	database.TransactionOptions,
) (database.NotificationTransaction, error) {
	return &fakeNotificationTransaction{}, nil
}

func (r *fakeNotificationsRepository) Update(notification models.Notification, logger log.Logger) error {
	r.notifications[notification.ID] = notification
	return nil
}

func (r *fakeNotificationsRepository) Commit(logger log.Logger) error {
	return nil
}

func (r *fakeNotificationsRepository) Rollback(logger log.Logger) error {
	return nil
}

func newFakeNotificationsRepository() fakeNotificationsRepository {
	return fakeNotificationsRepository{
		notifications: make(map[uint64]models.Notification),
	}
}

type fakeRecipientsRepository struct{}

func (r *fakeRecipientsRepository) GetOrCreate(_ context.Context, recipient models.Recipient) (*models.Recipient, error) {
	return &recipient, nil
}

type fakeOrdersRepository struct {
	interfaces.OrdersRepository
	mock.Mock
}

func (r *fakeOrdersRepository) GetByCorrelationID(ctx context.Context, correlationID string) ([]models.Order, error) {
	args := r.Called(ctx, correlationID)
	return args.Get(0).([]models.Order), args.Error(1)
}

func (r *fakeOrdersRepository) GetByID(ctx context.Context, orderID string) (models.Order, error) {
	args := r.Called(ctx, orderID)
	return args.Get(0).(models.Order), args.Error(1)
}

type fakeRenderer struct{}

func (r *fakeRenderer) RenderStructured(context.Context, []renderer.Block) (*renderer.StructuredHTML, error) {
	panic("Not implemented")
}

type fakeSender struct{}

func (s *fakeSender) SendTransactional(ctx context.Context, sendRequest sender.TransactionalRequest) ([]byte, error) {
	panic("Not implemented")
}

type fakeOrderProvider struct{}

func (op *fakeOrderProvider) GetOrderInfoByID(_ context.Context, id string) (*orders.OrderInfo, error) {
	return &orders.OrderInfo{ID: id}, nil
}

type fakeBlocksProvider struct{}

func (bp *fakeBlocksProvider) GetBlocks(context.Context, []blocks.BlockConfig, *orders.OrderInfo, models.Notification) (
	[]renderer.Block,
	error,
) {
	panic("Not implemented")
}

type fakeSettlementAccusativeTitleExtractor struct{}

func (f *fakeSettlementAccusativeTitleExtractor) GetAccusativeTitleWithPreposition(i int) (string, bool) {
	return "в Москву", true
}

func (f *fakeSettlementAccusativeTitleExtractor) GetPreposition(i int) string {
	return "в"
}

type fakeSettlementIDExtractor struct {
	interfaces.RoutePointsExtractor
}

func (f *fakeSettlementIDExtractor) ExtractArrivalSettlementID(order *orders.OrderInfo) (id int, err error) {
	return 213, nil
}

type fakeExpiredNotificationDeadlineHandler struct{}

func (e fakeExpiredNotificationDeadlineHandler) OnNotificationDeadline(
	context.Context,
	*models.Notification,
	*orders.OrderInfo,
) error {
	return nil
}

type fakeRollOutService struct{}

func (f *fakeRollOutService) IsEnabledForEmail(string) bool {
	return true
}

type fakeRenderLogger struct{}

func (f fakeRenderLogger) Log(*record.RenderLogRecord) error {
	panic("implement me")
}

func newTestProcessor() (*Processor, clockwork.Clock, *fakeNotificationsRepository, *fakeOrdersRepository) {
	logger := &nop.Logger{}
	notificationsRepository := newFakeNotificationsRepository()
	ordersRepository := &fakeOrdersRepository{}
	clock := clockwork.NewFakeClock()

	processor := NewProcessor(
		logger,
		&notificationsRepository,
		&fakeRecipientsRepository{},
		ordersRepository,
		&fakeRenderer{},
		&fakeSender{},
		&fakeOrderProvider{},
		&fakeBlocksProvider{},
		ProcessingConfig{},
		clock,
		&fakeSettlementIDExtractor{},
		&fakeExpiredNotificationDeadlineHandler{},
		&fakeRollOutService{},
		&fakeRenderLogger{},
		&fakeSettlementAccusativeTitleExtractor{},
	)
	return processor, clock, &notificationsRepository, ordersRepository
}

func TestProcessor_HasHigherPriorityNotification(t *testing.T) {
	ctx := context.Background()

	t.Run(
		"empty repository - no duplicates", func(t *testing.T) {
			processor, _, _, _ := newTestProcessor()
			result, err := processor.hasHigherPriorityNotification(ctx, &models.Notification{Order: &models.Order{ID: "1"}})
			assert.NoError(t, err)
			assert.False(t, result)
		},
	)

	t.Run(
		"no order in notification", func(t *testing.T) {
			processor, _, _, _ := newTestProcessor()
			_, err := processor.hasHigherPriorityNotification(ctx, &models.Notification{})
			assert.Error(t, err)
			assert.True(t, xerrors.Is(err, ErrNotificationWithNoOrder))
		},
	)

	t.Run(
		"don't count notification itself as a duplicate", func(t *testing.T) {
			processor, clock, repository, _ := newTestProcessor()
			notification := models.Notification{
				ID:       1,
				NotifyAt: clock.Now(),
				Order:    &models.Order{ID: "1", Type: models.OrderTrain},
			}
			repository.Put(notification)
			result, err := processor.hasHigherPriorityNotification(ctx, &notification)
			assert.NoError(t, err)
			assert.False(t, result)
		},
	)

	t.Run(
		"error when retrieving existing notifications", func(t *testing.T) {
			processor, clock, repository, _ := newTestProcessor()
			notification := models.Notification{
				ID:          1,
				RecipientID: ptr.Int32(1),
				NotifyAt:    clock.Now(),
				Order:       &models.Order{Type: models.OrderHotel},
			}
			repository.Put(notification)
			repository.SetAlwaysReturnError(true)
			_, err := processor.hasHigherPriorityNotification(ctx, &notification)
			assert.Error(t, err)
		},
	)

	type testData struct {
		name           string
		currentType    models.OrderType
		existingType   models.OrderType
		existingStatus models.NotificationStatus
		existingShift  time.Duration
		expected       bool
	}

	tests := []testData{
		{
			name:           "avia notification is never a duplicate",
			currentType:    models.OrderAvia,
			existingType:   models.OrderAvia,
			existingStatus: models.NotificationStatusPlanned,
			existingShift:  time.Duration(time.Hour),
			expected:       false,
		},
		{
			name:           "train notification ignores avia as a duplicate",
			currentType:    models.OrderTrain,
			existingType:   models.OrderAvia,
			existingStatus: models.NotificationStatusPlanned,
			existingShift:  time.Duration(time.Hour),
			expected:       false,
		},
		{
			name:           "hotel notification ignores avia as a duplicate",
			currentType:    models.OrderHotel,
			existingType:   models.OrderAvia,
			existingStatus: models.NotificationStatusPlanned,
			existingShift:  time.Duration(time.Hour),
			expected:       false,
		},
		{
			name:           "hotel notification has a train duplicate",
			currentType:    models.OrderHotel,
			existingType:   models.OrderTrain,
			existingStatus: models.NotificationStatusPlanned,
			existingShift:  time.Duration(time.Hour),
			expected:       true,
		},
		{
			name:           "train notification does not count hotel one as a duplicate",
			currentType:    models.OrderTrain,
			existingType:   models.OrderHotel,
			existingStatus: models.NotificationStatusPlanned,
			existingShift:  time.Duration(time.Hour),
			expected:       false,
		},
		{
			name:           "cancelled notification in the future is not a duplicate",
			currentType:    models.OrderHotel,
			existingType:   models.OrderTrain,
			existingStatus: models.NotificationStatusCancelled,
			existingShift:  time.Duration(time.Hour),
			expected:       false,
		},
		{
			name:           "cancelled notification in the past is not a duplicate",
			currentType:    models.OrderHotel,
			existingType:   models.OrderTrain,
			existingStatus: models.NotificationStatusCancelled,
			existingShift:  time.Duration(-time.Hour),
			expected:       false,
		},
		{
			// impossible case, but its still a duplicate because its not cancelled
			name:           "sent notification in the future is a duplicate",
			currentType:    models.OrderHotel,
			existingType:   models.OrderTrain,
			existingStatus: models.NotificationStatusSent,
			existingShift:  time.Hour,
			expected:       true,
		},
		{
			name:           "sent notification in the past is a duplicate",
			currentType:    models.OrderHotel,
			existingType:   models.OrderTrain,
			existingStatus: models.NotificationStatusSent,
			existingShift:  -time.Hour,
			expected:       true,
		},
	}
	for _, test := range tests {
		t.Run(
			test.name, func(t *testing.T) {
				processor, clock, repository, _ := newTestProcessor()
				notification1 := models.Notification{
					ID:          1,
					RecipientID: ptr.Int32(1),
					NotifyAt:    clock.Now(),
					Order:       &models.Order{ID: "1", Type: test.currentType},
					Type:        models.NotificationTypePretrip,
				}
				notification2 := models.Notification{
					ID:          2,
					RecipientID: ptr.Int32(1),
					NotifyAt:    clock.Now().Add(test.existingShift),
					Order:       &models.Order{ID: "1", Type: test.existingType},
					Status:      test.existingStatus,
					Type:        models.NotificationTypePretrip,
				}
				repository.Put(notification1)
				repository.Put(notification2)
				result, err := processor.hasHigherPriorityNotification(ctx, &notification1)
				assert.NoError(t, err)
				assert.Equal(t, test.expected, result)
			},
		)
	}

	t.Run(
		"cancel if there are duplicates", func(t *testing.T) {
			processor, clock, repository, _ := newTestProcessor()
			recipient := models.Recipient{
				ID: 1,
			}
			notification1 := models.Notification{
				ID:          1,
				RecipientID: ptr.Int32(1),
				Recipient:   &recipient,
				NotifyAt:    clock.Now(),
				Order:       &models.Order{Type: models.OrderHotel},
				Status:      models.NotificationStatusPlanned,
				Type:        models.NotificationTypePretrip,
			}
			repository.Put(notification1)
			// notification2 type has a higher priority, so notification1 shall be cancelled
			notification2 := models.Notification{
				ID:          2,
				RecipientID: ptr.Int32(1),
				Recipient:   &recipient,
				NotifyAt:    clock.Now().Add(time.Hour),
				Order:       &models.Order{Type: models.OrderTrain},
				Status:      models.NotificationStatusPlanned,
				Type:        models.NotificationTypePretrip,
			}
			repository.Put(notification2)
			iterationID := uuid.Nil
			processor.Process(ctx, &notification1, repository, iterationID)
			assert.EqualValues(t, models.NotificationStatusCancelled, repository.Get(1).Status)
		},
	)
}

func TestProcessor_IsMultiOrderCorrelationID(t *testing.T) {
	ctx := context.Background()

	t.Run(
		"empty repository - no orders", func(t *testing.T) {
			processor, _, _, _ := newTestProcessor()
			result, err := processor.isMultiOrderCorrelationID(ctx, models.Order{CorrelationID: "1"})
			assert.NoError(t, err)
			assert.False(t, result)
		},
	)

	t.Run(
		"single order", func(t *testing.T) {
			processor, _, _, ordersRepository := newTestProcessor()
			order := models.Order{CorrelationID: "1", Type: models.OrderTrain}
			relatedOrders := []models.Order{order}
			ordersRepository.On("GetByCorrelationID", mock.Anything, "1").Return(relatedOrders, nil)
			result, err := processor.isMultiOrderCorrelationID(ctx, order)
			assert.NoError(t, err)
			assert.False(t, result)
		},
	)

	t.Run(
		"breaks if repository generates an error", func(t *testing.T) {
			processor, _, _, ordersRepository := newTestProcessor()
			order := models.Order{CorrelationID: "1", Type: models.OrderTrain}
			ordersRepository.On("GetByCorrelationID", mock.Anything, "1").Return([]models.Order{}, fmt.Errorf("test error"))
			_, err := processor.isMultiOrderCorrelationID(ctx, order)
			assert.Error(t, err)
		},
	)

	t.Run(
		"set of two orders", func(t *testing.T) {
			processor, _, _, ordersRepository := newTestProcessor()
			order1 := models.Order{ID: "1", CorrelationID: "1", StartDate: date("2021-01-15"), Type: models.OrderTrain}
			order2 := models.Order{ID: "2", CorrelationID: "1", StartDate: date("2021-01-16"), Type: models.OrderTrain}
			relatedOrders := []models.Order{order1, order2}
			ordersRepository.
				On(
					"GetByCorrelationID",
					mock.Anything,
					"1",
				).Return(relatedOrders, nil).Times(len(relatedOrders))

			for _, order := range relatedOrders {
				result, err := processor.isMultiOrderCorrelationID(ctx, order)
				assert.NoError(t, err)
				assert.True(t, result)
			}
		},
	)
}

func date(date string) time.Time {
	result, _ := time.Parse(isoDateFormat, date)
	return result
}

func TestProcessor_AlreadySentInternal(t *testing.T) {
	type testData struct {
		name          string
		notifications []models.Notification
		newSubtypes   []models.NotificationSubtype
		expected      bool
	}

	allSubtypes := make([]models.NotificationSubtype, 0)
	for _, subtype := range models.AllNotificationSubtypes {
		allSubtypes = append(allSubtypes, subtype)
	}

	tests := []testData{
		{
			name:          "nothing has been sent ever before",
			notifications: []models.Notification{},
			newSubtypes:   allSubtypes,
			expected:      false,
		},
		{
			name: "any two letters have been sent",
			notifications: []models.Notification{
				{
					Type:    models.NotificationTypePretrip,
					Subtype: models.NotificationWeekBefore,
				},
				{
					Type:    models.NotificationTypePretrip,
					Subtype: models.NotificationWeekBefore,
				},
			},
			newSubtypes: allSubtypes,
			expected:    true,
		},
		{
			name: "week-before letter has been sent and now we have day-before",
			notifications: []models.Notification{
				{
					Type:    models.NotificationTypePretrip,
					Subtype: models.NotificationWeekBefore,
				},
			},
			newSubtypes: []models.NotificationSubtype{models.NotificationDayBefore},
			expected:    false,
		},
		{
			name: "week-before letter has been sent and now we have adhoc or another week-before",
			notifications: []models.Notification{
				{
					Type:    models.NotificationTypePretrip,
					Subtype: models.NotificationWeekBefore,
				},
			},
			newSubtypes: []models.NotificationSubtype{models.NotificationWeekBefore, models.NotificationAdhoc},
			expected:    true,
		},
		{
			name: "adhoc letter has been sent",
			notifications: []models.Notification{
				{
					Type:    models.NotificationTypePretrip,
					Subtype: models.NotificationAdhoc,
				},
			},
			newSubtypes: allSubtypes,
			expected:    true,
		},
		{
			name: "day-before letter has been sent",
			notifications: []models.Notification{
				{
					Type:    models.NotificationTypePretrip,
					Subtype: models.NotificationDayBefore,
				},
			},
			newSubtypes: allSubtypes,
			expected:    true,
		},
	}
	for _, test := range tests {
		for _, subtype := range test.newSubtypes {
			testName := fmt.Sprintf("%s, subtype=%s", test.name, subtype)
			t.Run(
				testName, func(t *testing.T) {
					result := isAlreadySentInternal(test.notifications, subtype)
					assert.Equal(t, test.expected, result)
				},
			)
		}
	}
}
