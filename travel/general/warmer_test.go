package warmer

import (
	"context"
	"testing"
	"time"

	"a.yandex-team.ru/travel/library/go/metrics"
	"a.yandex-team.ru/travel/library/go/resourcestorage"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/buses/backend/internal/api/cache"
	"a.yandex-team.ru/travel/buses/backend/internal/common/dict"
	"a.yandex-team.ru/travel/buses/backend/internal/common/dict/rasp"
	commonGRPC "a.yandex-team.ru/travel/buses/backend/internal/common/grpc"
	grpcMock "a.yandex-team.ru/travel/buses/backend/internal/common/grpc/mock"
	"a.yandex-team.ru/travel/buses/backend/internal/common/logging"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

func TestWarmer(t *testing.T) {

	warmerCfg := DefaultConfig
	warmerCfg.PopularDirectionRebuildPeriod = 10 * time.Millisecond
	warmerCfg.NextStepWait = 10 * time.Millisecond
	warmerCfg.MaxTaskQueueDuration = 2 * time.Second
	warmerCfg.CalendarK = 2
	warmerCfg.CalendarDepth = 3

	newWarmerAndFriends := func() (*Warmer, *cache.SegmentsStorage,
		context.Context, context.CancelFunc, error) {
		logger, _ := logging.New(&logging.DefaultConfig)
		metricsRegistry := metrics.NewAppMetricsRegistryWithPrefix("")
		appMetrics := metrics.NewAppMetrics(metricsRegistry)

		searchCacheCfg := cache.DefaultConfig
		searchCache := cache.NewSearchRecordStorage(time.Second, appMetrics, &searchCacheCfg, logger)

		segments := cache.NewSegmentsStorage(appMetrics)

		raspRepo := rasp.NewRepo(&rasp.DefaultConfig, logger)

		ctx, ctxCancel := context.WithCancel(context.Background())

		commonGRPC.InitBufConn()
		grpcServer, err := commonGRPC.NewServer(&commonGRPC.MockedServerConfig, metricsRegistry, logger)
		if err != nil {
			ctxCancel()
			return nil, nil, ctx, func() {}, err
		}
		wpb.RegisterWorkerServiceServer(grpcServer.GetCoreServer(), grpcMock.NewMockedWorker())

		workerServiceConnection, err := commonGRPC.NewServiceConnection(&commonGRPC.MockedClientConfig, logger, ctx)
		if err != nil {
			ctxCancel()
			return nil, nil, ctx, func() {}, err
		}
		workerServiceClient := wpb.NewWorkerServiceClient(workerServiceConnection)
		go func() {
			_ = grpcServer.Serve()
		}()

		warmer := NewWarmer(warmerCfg, searchCache, segments, workerServiceClient,
			resourcestorage.MockedS3StorageConfig, "", "", raspRepo, appMetrics, nil, logger)

		return warmer, segments, ctx, func() {
			ctxCancel()
			grpcServer.Stop()
			commonGRPC.CloseBufConn()
			_ = logger.L.Sync()
		}, nil
	}

	p1 := pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 1}
	p2 := pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 2}
	p3 := pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 3}
	p4 := pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 4}
	supplierID1 := dict.GetSuppliersList()[0]
	supplier1, _ := dict.GetSupplier(supplierID1)

	t.Run("Overload", func(t *testing.T) {
		warmer, segments, ctx, cancel, err := newWarmerAndFriends()
		defer cancel()
		if !assert.NoError(t, err) {
			return
		}

		segmentsData := []*wpb.TSegment{
			{From: &p1, To: &p2}, {From: &p1, To: &p3}, {From: &p1, To: &p4},
			{From: &p2, To: &p1}, {From: &p2, To: &p3}, {From: &p2, To: &p4},
			{From: &p3, To: &p1}, {From: &p3, To: &p2}, {From: &p3, To: &p4},
			{From: &p4, To: &p1}, {From: &p4, To: &p2}, {From: &p4, To: &p3},
		}
		segments.SetSegments(supplierID1, segmentsData, time.Now())

		for i := 0; i < 10; i++ {
			for _, s := range segmentsData {
				warmer.RegisterQuery(s.From, s.To, pb.ERequestSource_SRS_WIZARD)
			}
		}

		warmer.Run(ctx)
		time.Sleep(100 * time.Millisecond)

		if !assert.Equal(t, uint32(supplier1.SearchRPS*warmerCfg.MaxTaskQueueDuration.Seconds()), warmer.scheduled) {
			return
		}
	})

	t.Run("GenWalkOrder", func(t *testing.T) {
		warmer, _, _, cancel, err := newWarmerAndFriends()
		defer cancel()
		if !assert.NoError(t, err) {
			return
		}

		if !assert.Equal(t,
			[][]int{},
			warmer.genWalkOrder(3, 0)) {
			return
		}

		if !assert.Equal(t,
			[][]int{
				{0, 0}, {1, 0}, {2, 0}, {3, 0},
			},
			warmer.genWalkOrder(4, 1)) {
			return
		}

		if !assert.Equal(t,
			[][]int{
				{0, 0}, {0, 1}, {0, 2}, {0, 3}, {1, 0},
				{1, 1}, {0, 4}, {1, 2}, {1, 3}, {2, 0},
				{2, 1}, {1, 4}, {2, 2}, {2, 3}, {2, 4},
			},
			warmer.genWalkOrder(3, 5)) {
			return
		}
	})
}
