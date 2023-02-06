package searcher

import (
	pcpb "a.yandex-team.ru/travel/trains/search_api/api/price_calendar"
	"bytes"
	"container/list"
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"
	"go.uber.org/atomic"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/travel/library/go/httputil/client"
	"a.yandex-team.ru/travel/trains/library/go/httputil/clients/mordabackend"
	mbModels "a.yandex-team.ru/travel/trains/library/go/httputil/clients/mordabackend/models"
	"a.yandex-team.ru/travel/trains/library/go/httputil/clients/pathfinderproxy"
	pfpModels "a.yandex-team.ru/travel/trains/library/go/httputil/clients/pathfinderproxy/models"
	"a.yandex-team.ru/travel/trains/library/go/httputil/clients/trainapi"
	taModels "a.yandex-team.ru/travel/trains/library/go/httputil/clients/trainapi/models"
	"a.yandex-team.ru/travel/trains/search_api/internal/searcher/models"
)

func buildMockedTransport(t *testing.T, responses ...interface{}) http.RoundTripper {
	l := list.New()
	for _, response := range responses {
		reflectValue := reflect.ValueOf(response)
		if reflectValue.Kind() == reflect.Ptr && reflectValue.IsNil() {
			continue
		}
		l.PushBack(response)
	}
	return client.TransportMock(func(req *http.Request) (*http.Response, error) {
		response := l.Front()

		if response == nil {
			return &http.Response{
				StatusCode: http.StatusBadRequest,
				Body:       ioutil.NopCloser(bytes.NewBuffer([]byte{})),
				Header:     make(http.Header),
			}, nil
		}

		l.Remove(response)

		respBody, err := json.Marshal(response.Value)
		assert.NoError(t, err)
		return &http.Response{
			StatusCode: http.StatusOK,
			Body:       ioutil.NopCloser(bytes.NewBuffer(respBody)),
			Header:     make(http.Header),
		}, nil
	})
}

func loadModelFromFile(t *testing.T, fn string, model interface{}) {
	body, err := ioutil.ReadFile(fn)
	assert.NoError(t, err)
	err = json.Unmarshal(body, model)
	assert.NoError(t, err)
}

type MockedPriceCalendarService struct {
}

func (s *MockedPriceCalendarService) PriceCalendar(_ context.Context, _, _ string, _ models.UserArgs) (*pcpb.PriceCalendarResponse, error) {
	return nil, fmt.Errorf("some error")
}

func (s *MockedPriceCalendarService) PriceCalendarRange(_ context.Context, _, _, _, _ string, _ models.UserArgs) (*pcpb.PriceCalendarResponse, error) {
	return nil, fmt.Errorf("some error")
}

func buildMockedSearcher(t *testing.T,
	logger log.Logger,
	cfg Config,
	ctx context.Context,
	trainAPIResponses []interface{},
	mordaBackendResponses []interface{},
	pathfinderProxyResponses []interface{},
) *Searcher {

	trainAPIClient, err := trainapi.NewTrainAPIClientWithTransport(
		&cfg.TrainAPI, logger, buildMockedTransport(t, trainAPIResponses...),
	)
	assert.NoError(t, err)
	mordaBackendClient, err := mordabackend.NewMordaBackendClientWithTransport(
		&cfg.MordaBackend, logger, buildMockedTransport(t, mordaBackendResponses...),
	)
	assert.NoError(t, err)
	pathfinderProxyClient, err := pathfinderproxy.NewPathfinderProxyClientWithTransport(
		ctx, &cfg.PathfinderProxy, logger, buildMockedTransport(t, pathfinderProxyResponses...),
	)
	assert.NoError(t, err)

	cfg.EnableParallelSearch = false
	activePartners := atomic.Value{}
	activePartners.Store([]string{taModels.PartnerCodeIM, taModels.PartnerCodeUFS})

	return &Searcher{
		logger: logger,
		cfg:    cfg,
		ctx:    ctx,

		trainAPIClient:        trainAPIClient,
		mordaBackendClient:    mordaBackendClient,
		pathfinderProxyClient: pathfinderProxyClient,

		activePartners: &activePartners,
		boyEnabled:     atomic.NewBool(true),

		priceCalendarService: &MockedPriceCalendarService{},
	}
}

func TestSearch(t *testing.T) {

	const (
		pointFrom  = "c213"
		pointTo    = "c54"
		when       = "2041-03-01"
		returnWhen = "2041-03-14"
	)

	logger, _ := zap.New(zap.ConsoleConfig(log.InfoLevel))

	t.Run("OneWaySearch(TA)", func(t *testing.T) {
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		taResp := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-1.json", taResp)

		app := buildMockedSearcher(t, logger, DefaultConfig, ctx, []interface{}{taResp}, nil, nil)
		userArgs := models.UserArgs{}
		testContext := models.TestContext{}
		searchResponse, err := app.Search(ctx, pointFrom, pointTo, when, "", "", "", false, false, userArgs, testContext)
		assert.NoError(t, err)
		assert.Equal(t, mbModels.ResponseContext{}, searchResponse.Context)
		assert.Equal(t, false, searchResponse.Querying)
		assert.NotEmpty(t, searchResponse.TransferVariants[0].Forward[0].Company.UFSTitle)
		assert.Len(t, searchResponse.TransferVariants, 11)
		assert.Len(t, searchResponse.TransferVariants[0].Forward[0].TariffsKeys, 0)
	})

	t.Run("OneWaySearch(MB)", func(t *testing.T) {
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		mbResp := &mbModels.SearchResponse{}
		loadModelFromFile(t, "gotest/mb-response-1.json", mbResp)

		app := buildMockedSearcher(t, logger, DefaultConfig, ctx, nil, []interface{}{mbResp}, nil)
		userArgs := models.UserArgs{}
		testContext := models.TestContext{}
		searchResponse, err := app.Search(ctx, pointFrom, pointTo, when, "", "", "", false, false, userArgs, testContext)
		assert.NoError(t, err)
		assert.Len(t, searchResponse.Context.TransportTypes, 1)
		assert.Equal(t, false, searchResponse.Querying)
		assert.Len(t, searchResponse.TransferVariants, 9)
	})

	t.Run("OneWaySearch(TA + MB)", func(t *testing.T) {
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		taResp := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-1.json", taResp)
		mbResp := &mbModels.SearchResponse{}
		loadModelFromFile(t, "gotest/mb-response-1.json", mbResp)

		app := buildMockedSearcher(t, logger, DefaultConfig, ctx, []interface{}{taResp}, []interface{}{mbResp}, nil)
		userArgs := models.UserArgs{}
		testContext := models.TestContext{}
		searchResponse, err := app.Search(ctx, pointFrom, pointTo, when, "", "", "", false, false, userArgs, testContext)
		assert.NoError(t, err)
		assert.Len(t, searchResponse.Context.TransportTypes, 1)
		assert.Equal(t, false, searchResponse.Querying)
		assert.Len(t, searchResponse.TransferVariants, 12)
		assert.Equal(t, models.OrderOwnerTrains, searchResponse.TransferVariants[5].OrderURL.Owner)
		assert.Equal(t, float32(2880.5), searchResponse.TransferVariants[0].MinPrice.Value)
		assert.Equal(t, currencyRub, searchResponse.TransferVariants[0].MinPrice.Currency)
		assert.Equal(t, float32(5322.7), searchResponse.TransferVariants[0].Forward[0].Tariffs.Classes.Compartment.Price.Value)
		assert.Equal(t, "static 2000003 9607404 082I_2_2 0301", searchResponse.TransferVariants[0].Forward[0].TariffsKeys[1])
	})

	t.Run("OneWaySearch(TA + MB + PFP)", func(t *testing.T) {
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		taResp := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-1.json", taResp)
		mbResp := &mbModels.SearchResponse{}
		loadModelFromFile(t, "gotest/mb-response-1.json", mbResp)
		pfpResp := &pfpModels.TransferVariantsWithPricesResponse{}
		loadModelFromFile(t, "gotest/pfp-response-1.json", pfpResp)

		app := buildMockedSearcher(t, logger, DefaultConfig, ctx, []interface{}{taResp}, []interface{}{mbResp}, []interface{}{pfpResp})
		userArgs := models.UserArgs{}
		testContext := models.TestContext{}
		searchResponse, err := app.Search(ctx, pointFrom, pointTo, when, "", "", "", false, false, userArgs, testContext)
		assert.NoError(t, err)
		assert.Len(t, searchResponse.Context.TransportTypes, 1)
		assert.Len(t, searchResponse.TransferVariants, 13)
		assert.Equal(t, models.OrderOwnerTrains, searchResponse.TransferVariants[5].OrderURL.Owner)
		assert.Equal(t, false, searchResponse.Querying)
		assert.Equal(t, float32(5322.7), searchResponse.TransferVariants[0].Forward[0].Tariffs.Classes.Compartment.Price.Value)
		assert.Equal(t, "static 2000003 9607404 082I_2_2 0301", searchResponse.TransferVariants[0].Forward[0].TariffsKeys[1])
		assert.Len(t, searchResponse.TransferVariants[12].Forward, 2)
		assert.Equal(t, "RUB", searchResponse.TransferVariants[9].Forward[0].Tariffs.Classes.Compartment.Price.Currency)
	})

	t.Run("OneWaySearchWithNoTransfersThreshold(TA + MB + PFP)", func(t *testing.T) {
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		taResp := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-1.json", taResp)
		mbResp := &mbModels.SearchResponse{}
		loadModelFromFile(t, "gotest/mb-response-1.json", mbResp)
		pfpResp := &pfpModels.TransferVariantsWithPricesResponse{}
		loadModelFromFile(t, "gotest/pfp-response-1.json", pfpResp)

		config := DefaultConfig
		config.NoTransfersThreshold = 2

		app := buildMockedSearcher(t, logger, config, ctx, []interface{}{taResp}, []interface{}{mbResp}, []interface{}{pfpResp})
		userArgs := models.UserArgs{}
		testContext := models.TestContext{}
		searchResponse, err := app.Search(ctx, pointFrom, pointTo, when, "", "", "", false, false, userArgs, testContext)
		assert.NoError(t, err)
		assert.Len(t, searchResponse.TransferVariants, 12)
		for _, variant := range searchResponse.TransferVariants {
			assert.Len(t, variant.Forward, 1)
		}

		app = buildMockedSearcher(t, logger, config, ctx, []interface{}{taResp}, []interface{}{mbResp}, []interface{}{pfpResp})
		userArgs = models.UserArgs{UaasExperiments: "{\"SOME_EXP\": \"QWE\", \"TRAINS_NO_TRANSFERS_THRESHOLD\": \"20\"}"}
		searchResponse, err = app.Search(ctx, pointFrom, pointTo, when, "", "", "", false, false, userArgs, testContext)
		assert.NoError(t, err)
		assert.Len(t, searchResponse.TransferVariants, 13)
	})

	t.Run("RoundtripSearch(TA)", func(t *testing.T) {
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		taResp1 := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-1.json", taResp1)
		taResp2 := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-2.json", taResp2)

		app := buildMockedSearcher(t, logger, DefaultConfig, ctx, []interface{}{taResp1, taResp2}, nil, nil)
		userArgs := models.UserArgs{}
		testContext := models.TestContext{}
		searchResponse, err := app.Search(ctx, pointFrom, pointTo, when, returnWhen, "", "", false, false, userArgs, testContext)
		assert.NoError(t, err)
		assert.Equal(t, mbModels.ResponseContext{}, searchResponse.Context)
		assert.Len(t, searchResponse.TransferVariants, 48)
	})

	t.Run("RoundtripSearch(TA + MB)", func(t *testing.T) {
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		taResp1 := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-1.json", taResp1)
		taResp2 := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-2.json", taResp2)
		mbResp := &mbModels.SearchResponse{}
		loadModelFromFile(t, "gotest/mb-response-1.json", mbResp)

		app := buildMockedSearcher(t, logger, DefaultConfig, ctx, []interface{}{taResp1, taResp2}, []interface{}{mbResp}, nil)
		userArgs := models.UserArgs{}
		testContext := models.TestContext{}
		searchResponse, err := app.Search(ctx, pointFrom, pointTo, when, returnWhen, "", "", false, false, userArgs, testContext)
		assert.NoError(t, err)
		assert.Len(t, searchResponse.Context.TransportTypes, 1)
		assert.Len(t, searchResponse.TransferVariants, 54)

		for _, variant := range searchResponse.TransferVariants {
			// Для туда-обратно должны быть только прямые варианты
			assert.Len(t, variant.Forward, 1)
			assert.Len(t, variant.Backward, 1)
		}
	})

	t.Run("RoundtripSearchWithPin(TA)", func(t *testing.T) {
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		taResp1 := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-1.json", taResp1)
		taResp2 := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-2.json", taResp2)

		app := buildMockedSearcher(t, logger, DefaultConfig, ctx, []interface{}{taResp1, taResp2}, nil, nil)

		const pinForwardSegment = "2000003_9607404_2245745400"
		userArgs := models.UserArgs{}
		testContext := models.TestContext{}
		searchResponse, err := app.Search(ctx, pointFrom, pointTo, when, returnWhen, pinForwardSegment, "", false, false, userArgs, testContext)
		assert.NoError(t, err)
		assert.Len(t, searchResponse.TransferVariants, 7)
		assert.Equal(t, pinForwardSegment, searchResponse.TransferVariants[4].Forward[0].ID)
	})

	t.Run("RoundtripSearchWithPin(TA)_P2Filter", func(t *testing.T) {
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		taResp1 := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-1.json", taResp1)
		taResp2 := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-2.json", taResp2)

		app := buildMockedSearcher(t, logger, DefaultConfig, ctx, []interface{}{taResp1, taResp2}, nil, nil)

		const pinForwardSegment = "train 002Э 20210301_0035"
		userArgs := models.UserArgs{}
		testContext := models.TestContext{}
		searchResponse, err := app.Search(ctx, pointFrom, pointTo, when, returnWhen, pinForwardSegment, "", false, false, userArgs, testContext)
		assert.NoError(t, err)
		assert.Len(t, searchResponse.TransferVariants, 0)
	})

	t.Run("OneWaySearchIsBot(TA + MB + PFP)", func(t *testing.T) {
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		taResp := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-1.json", taResp)
		mbResp := &mbModels.SearchResponse{}
		loadModelFromFile(t, "gotest/mb-response-1.json", mbResp)
		pfpResp := &pfpModels.TransferVariantsWithPricesResponse{}
		loadModelFromFile(t, "gotest/pfp-response-1.json", pfpResp)

		app := buildMockedSearcher(t, logger, DefaultConfig, ctx, []interface{}{taResp}, []interface{}{mbResp}, []interface{}{pfpResp})
		userArgs := models.UserArgs{
			IsBot: true,
		}
		testContext := models.TestContext{}
		searchResponse, err := app.Search(ctx, pointFrom, pointTo, when, "", "", "", false, false, userArgs, testContext)
		assert.NoError(t, err)
		assert.Len(t, searchResponse.Context.TransportTypes, 1)
		assert.Len(t, searchResponse.TransferVariants, 10)
		assert.Equal(t, models.OrderOwnerUnknown, searchResponse.TransferVariants[5].OrderURL.Owner)
		assert.Equal(t, false, searchResponse.Querying)
		assert.Len(t, searchResponse.TransferVariants[2].Forward, 1)
	})

	t.Run("OneWaySearchBoYDisabled(TA + MB + PFP)", func(t *testing.T) {
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		taResp := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-3.json", taResp)
		mbResp := &mbModels.SearchResponse{}
		loadModelFromFile(t, "gotest/mb-response-1.json", mbResp)
		pfpResp := &pfpModels.TransferVariantsWithPricesResponse{}
		loadModelFromFile(t, "gotest/pfp-response-1.json", pfpResp)

		app := buildMockedSearcher(t, logger, DefaultConfig, ctx, []interface{}{taResp}, []interface{}{mbResp}, []interface{}{pfpResp})
		app.boyEnabled.Store(false)
		app.activePartners.Store([]string{taModels.PartnerCodeUFS})
		userArgs := models.UserArgs{}
		testContext := models.TestContext{}
		searchResponse, err := app.Search(ctx, pointFrom, pointTo, when, "", "", "", false, false, userArgs, testContext)
		assert.NoError(t, err)
		assert.Equal(t, []string{taModels.PartnerCodeUFS}, searchResponse.ActivePartners)
		assert.Len(t, searchResponse.Context.TransportTypes, 1)
		assert.Len(t, searchResponse.TransferVariants, 17)
		for _, variant := range searchResponse.TransferVariants {
			if variant.MinPrice.Value != 0 {
				assert.Equal(t, models.OrderOwnerUFS, variant.OrderURL.Owner)
			}
		}
	})

	t.Run("OneWaySearchOnlyOwnedPrices(TA + PFP)", func(t *testing.T) {
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		taResp := &taModels.TrainTariffsResponse{}
		loadModelFromFile(t, "gotest/ta-response-1.json", taResp)
		pfpResp := &pfpModels.TransferVariantsWithPricesResponse{}
		loadModelFromFile(t, "gotest/pfp-response-1.json", pfpResp)

		app := buildMockedSearcher(t, logger, DefaultConfig, ctx, []interface{}{taResp}, nil, []interface{}{pfpResp})
		userArgs := models.UserArgs{}
		testContext := models.TestContext{}
		searchResponse, err := app.Search(ctx, pointFrom, pointTo, when, "", "", "", false, true, userArgs, testContext)
		assert.NoError(t, err)
		assert.Len(t, searchResponse.TransferVariants, 13)
		for _, variant := range searchResponse.TransferVariants {
			if variant.MinPrice.Value != 0 {
				assert.Equal(t, models.OrderOwnerTrains, variant.OrderURL.Owner)
			}
		}
	})
}
