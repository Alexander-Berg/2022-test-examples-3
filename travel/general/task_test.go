package task

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue/recipe"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
	"a.yandex-team.ru/travel/buses/backend/internal/common/connector"
	connectorMock "a.yandex-team.ru/travel/buses/backend/internal/common/connector/mock"
	"a.yandex-team.ru/travel/buses/backend/internal/common/dict"
	"a.yandex-team.ru/travel/buses/backend/internal/common/logbroker"
	"a.yandex-team.ru/travel/buses/backend/internal/common/logging"
	workerLogging "a.yandex-team.ru/travel/buses/backend/internal/worker/logging"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
	"a.yandex-team.ru/travel/library/go/unifiedagent"
	tpb "a.yandex-team.ru/travel/proto"
)

var (
	logger, _ = logging.New(&logging.DefaultConfig)
)

func TestSearchTask(t *testing.T) {

	const (
		topic = "default-topic"
	)

	ctx, ctxCancel := context.WithCancel(context.Background())
	defer ctxCancel()

	connectorSearchScenario := connectorMock.GetSearchScenario()
	connectorSearchScenario.SetDefault([]*pb.TRide{
		{},
	})

	searchTaskQueue := NewSearchTaskQueue(testAppMetrics)

	persqueueEnv := recipe.New(t)
	producer, err := logbroker.NewProducer(
		logbroker.ProducerConfig{TestEnv: persqueueEnv}, topic, nil, logger)
	if err != nil {
		t.Errorf("failed to init search producer: %s", err)
		return
	}
	err = producer.Run(ctx)
	if err != nil {
		t.Errorf("failed to run search producer: %s", err)
		return
	}
	consumer, err := logbroker.NewConsumer(
		logbroker.ConsumerConfig{TestEnv: persqueueEnv}, topic, "", time.Time{}, nil, nil, logger)
	if err != nil {
		t.Errorf("failed to init search consumer: %s", err)
		return
	}
	assert.NoError(t, consumer.Run(ctx))
	consumerChannel := consumer.NewChannel()

	supplier, _ := dict.GetSupplier(dict.GetSuppliersList()[0])

	communicationLogger, err := workerLogging.NewCommunicationLogger(
		&unifiedagent.ClientConfig{
			Enabled: false,
		},
		logger,
	)
	if err != nil {
		t.Errorf("failed to create communication logger: %s", err)
		return
	}

	searchTask := NewSearchTask(searchTaskQueue, &connector.MockedConfig, producer, supplier, logger,
		communicationLogger, solomon.NewRegistry(nil))

	t.Run("TestSearchTask. Get SearchResult", func(t *testing.T) {
		request := wpb.TSearchRequest{
			Header: &wpb.TRequestHeader{
				Priority: wpb.ERequestPriority_REQUEST_PRIORITY_NORMAL,
			},
			SupplierId: supplier.ID,
			From:       &pb.TPointKey{},
			To:         &pb.TPointKey{},
			Date:       &tpb.TDate{},
		}
		_, _ = searchTaskQueue.Push(&request)
		searchTask.Do()
		result := wpb.TSearchResult{}
		err = consumerChannel.ReadWithDeadline(&result, time.Now().Add(time.Second))
		if err != nil {
			t.Errorf("no search results: %s", err.Error())
			return
		}
	})

	t.Run("TestSearchTask. Priority", func(t *testing.T) {
		request1 := wpb.TSearchRequest{
			Header: &wpb.TRequestHeader{
				Priority: wpb.ERequestPriority_REQUEST_PRIORITY_NORMAL,
			},
			SupplierId: supplier.ID,
			From:       &pb.TPointKey{Id: 1},
			To:         &pb.TPointKey{},
			Date:       &tpb.TDate{},
		}
		i, _ := searchTaskQueue.Push(&request1)
		if i != 1 {
			t.Errorf("bad queue position: extected %d, got %d", 1, i)
			return
		}
		request2 := wpb.TSearchRequest{
			Header: &wpb.TRequestHeader{
				Priority: wpb.ERequestPriority_REQUEST_PRIORITY_HIGH,
			},
			SupplierId: supplier.ID,
			From:       &pb.TPointKey{Id: 2},
			To:         &pb.TPointKey{},
			Date:       &tpb.TDate{},
		}
		i, _ = searchTaskQueue.Push(&request2)
		if i != 1 {
			t.Errorf("bad queue position: extected %d, got %d", 1, i)
			return
		}
		searchTask.Do()
		result := wpb.TSearchResult{}
		err = consumerChannel.ReadWithDeadline(&result, time.Now().Add(time.Second))
		if err != nil {
			t.Errorf("no search results: %s", err.Error())
			return
		}
		if result.Request.From.Id != request2.From.Id {
			t.Errorf("bad results sequence 1")
		}

		searchTask.Do()
		err = consumerChannel.ReadWithDeadline(&result, time.Now().Add(time.Second))
		if err != nil {
			t.Errorf("no search results: %s", err.Error())
			return
		}
		if result.Request.From.Id != request1.From.Id {
			t.Errorf("bad results sequence 2")
		}
	})

	t.Run("TestSearchTask. Bad response", func(t *testing.T) {
		connectorSearchScenario.Clear()
		request := wpb.TSearchRequest{
			Header: &wpb.TRequestHeader{
				Priority: wpb.ERequestPriority_REQUEST_PRIORITY_NORMAL,
			},
			SupplierId: supplier.ID,
			From:       &pb.TPointKey{Id: 1},
			To:         &pb.TPointKey{},
			Date:       &tpb.TDate{},
		}
		_, _ = searchTaskQueue.Push(&request)
		searchTask.Do()
		result := wpb.TSearchResult{}
		err = consumerChannel.ReadWithDeadline(&result, time.Now().Add(time.Second))
		if err != nil {
			t.Errorf("no search results: %s", err.Error())
			return
		}
		if result.Header.Code != tpb.EErrorCode_EC_GENERAL_ERROR {
			t.Errorf("expected %s, got %s status", tpb.EErrorCode_EC_GENERAL_ERROR, result.Header.Code)
			return
		}
	})
}
