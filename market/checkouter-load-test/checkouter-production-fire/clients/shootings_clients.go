package clients

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"fmt"
	"go.uber.org/zap"
	"net/url"
)

type shootingClients struct {
	oTrace       OTrace
	stockStorage StockStorage
	report       Report
	checkouter   Checkouter
	yqlStocks    YQLStocks
	loyalty      LoyaltyClient
}

func (g *shootingClients) GetLoyaltyClient() LoyaltyClient {
	return g.loyalty
}

func (g *shootingClients) GetYQLStocksClient() YQLStocksClient {
	return &g.yqlStocks
}

type ClientOptions struct {
	Environment       string
	CheckouterBaseURL *url.URL
	TvmSecret         util.SecretConfig
	YqlSecret         util.SecretConfig
	RpsLimiterOptions
}

func GetShootingClients(options ClientOptions, deps *configs.ClientDependencies) ShootingClients {
	environment := options.Environment
	limiter := InitRpsLimiter(options.RpsLimiterOptions)
	var checkouterBaseURL url.URL
	ticketFunc := deps.TicketFunc
	requestProcessor := deps.RequestProcessor
	categoriesReader := deps.CategoriesReader
	yql := deps.YQL

	zap.L().Info("Create client", zap.Reflect("options", options))
	if environment == configs.Production {
		tvmSecret := deps.SecretResolver(options.TvmSecret)
		if options.CheckouterBaseURL == nil {
			checkouterBaseURL = getCheckouterProdURL()
		} else {
			checkouterBaseURL = *options.CheckouterBaseURL
		}
		return newShootingClients(
			OTrace{getAnonymousClient(getOTraceProdURL(), limiter, requestProcessor)},
			StockStorage{getAuthenticatedClient(getStockStorageProdURL(), limiter, requestProcessor, ticketFunc(tvmProdID, 2011220, tvmSecret))},
			Report{categoriesReader: categoriesReader, ClientConfig: getAnonymousClient(getReportProdURL(), limiter, requestProcessor)},
			Checkouter{getAuthenticatedClient(checkouterBaseURL, limiter, requestProcessor, ticketFunc(tvmProdID, 2010064, tvmSecret))},
			YQLStocks{YQLConfig: prodYqlConfig, secret: options.YqlSecret, yql: yql, requestProcessor: requestProcessor},
			NewLoyaltyClient(options.Environment, tvmSecret, limiter, deps),
		)

	}
	if environment == configs.Testing {
		tvmSecret := deps.SecretResolver(options.TvmSecret)
		if options.CheckouterBaseURL == nil {
			checkouterBaseURL = getCheckouterTestURL()
		} else {
			checkouterBaseURL = *options.CheckouterBaseURL
		}
		return newShootingClients(
			OTrace{getAnonymousClient(getOTraceTestURL(), limiter, requestProcessor)},
			StockStorage{getAuthenticatedClient(getStockStorageTestURL(), limiter, requestProcessor, ticketFunc(tvmTestID, 2011222, tvmSecret))},
			Report{categoriesReader: categoriesReader, ClientConfig: getAnonymousClient(getReportTestURL(), limiter, requestProcessor)},
			Checkouter{getAuthenticatedClient(checkouterBaseURL, limiter, requestProcessor, ticketFunc(tvmTestID, 2010068, tvmSecret))},
			YQLStocks{YQLConfig: testYqlConfig, secret: options.YqlSecret, yql: yql, requestProcessor: requestProcessor},
			NewLoyaltyClient(options.Environment, tvmSecret, limiter, deps),
		)

	}
	panic(fmt.Errorf("incorrect enviroment: '%v'", environment))
}

func newShootingClients(
	oTrace OTrace,
	stockStorage StockStorage,
	report Report,
	checkouter Checkouter,
	yqlStocks YQLStocks,
	loyalty LoyaltyClient,
) ShootingClients {
	return &shootingClients{
		oTrace:       oTrace,
		stockStorage: stockStorage,
		report:       report,
		checkouter:   checkouter,
		yqlStocks:    yqlStocks,
		loyalty:      loyalty,
	}
}

func (g *shootingClients) GetOTrace() OTraceClient {
	return &g.oTrace
}

func (g *shootingClients) GetStockStorage() StockStorageClient {
	return &g.stockStorage
}

func (g *shootingClients) GetReport() ReportClient {
	return &g.report
}

func (g *shootingClients) GetCheckouter() CheckouterClient {
	return &g.checkouter
}

type ShootingClients interface {
	GetOTrace() OTraceClient
	GetStockStorage() StockStorageClient
	GetReport() ReportClient
	GetCheckouter() CheckouterClient
	GetYQLStocksClient() YQLStocksClient
	GetLoyaltyClient() LoyaltyClient
}
