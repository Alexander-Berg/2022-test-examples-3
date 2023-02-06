package clients

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/model"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"bytes"
	"errors"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"golang.org/x/sync/semaphore"
	"io"
	"net/http"
	"net/url"
	"path"
)

const JSON = "application/json"

type RpsLimiter interface {
	GetSemaphore(label string) (weighted *semaphore.Weighted, ok bool)
}

type rpsLimiter struct {
	labelToSemaphoreWeighted map[string]*semaphore.Weighted
}

type unlimitedRpsLimiter struct {
}

func (u *unlimitedRpsLimiter) GetSemaphore(label string) (weighted *semaphore.Weighted, ok bool) {
	return semaphore.NewWeighted(1), true
}

func (r *rpsLimiter) GetSemaphore(label string) (weighted *semaphore.Weighted, ok bool) {
	weighted, ok = r.labelToSemaphoreWeighted[label]
	return
}

type RpsLimiterOptions struct {
	RpsLimiterRate     float32
	Rps                int
	CartRepeats        int
	ChooseOfferRepeats int
	Handles            []model.Handle
}

func NewUnlimitedLimiter() RpsLimiter {
	return &unlimitedRpsLimiter{}
}

func InitRpsLimiter(options RpsLimiterOptions) RpsLimiter {
	baseRps := util.Max(int(float32(options.Rps)*options.RpsLimiterRate), 1)
	labelToSemaphoreWeighted := map[string]*semaphore.Weighted{
		"":                                    semaphore.NewWeighted(int64(baseRps)),
		model.LabelCheckouterCart:             semaphore.NewWeighted(int64(baseRps * options.CartRepeats)),
		model.LabelLoyaltyFetchBonusesForCart: semaphore.NewWeighted(int64(baseRps * options.CartRepeats)),
		model.LabelCheckouterCheckout:         semaphore.NewWeighted(int64(baseRps)),
		model.LabelCheckouterGetOrders:        semaphore.NewWeighted(int64(baseRps)),
		model.LabelSsGetAvailableAmounts:      semaphore.NewWeighted(int64(baseRps * options.ChooseOfferRepeats)),
		model.LabelCheckouterUnfreeze:         semaphore.NewWeighted(int64(baseRps * 10)),
		model.LabelSsUnfreezeStocks:           semaphore.NewWeighted(int64(baseRps * 10)),
		model.LabelReportGetStocks:            semaphore.NewWeighted(1),
	}
	for _, h := range options.Handles {
		labelToSemaphoreWeighted[h.GetLabel()] = semaphore.NewWeighted(int64(baseRps * h.Repeats))
	}
	return &rpsLimiter{
		labelToSemaphoreWeighted: labelToSemaphoreWeighted,
	}
}

func (c *ClientConfig) WithPathAndQuery(relativePath string, params string) *url.URL {
	u := c.url
	u.Path = path.Join(u.Path, relativePath)
	u.RawQuery = params
	return &u
}

func (c *ClientConfig) Get(label string, shootContext abstractions.ShootContext, u *url.URL, result interface{}, logFullResponse bool) (err error) {
	headers := map[string]string{}
	err = c.enrichHeaders(headers)
	if err != nil {
		return err
	}
	return c.SendRequest(label, shootContext, "GET", u, headers, nil, result, logFullResponse)
}

func (c *ClientConfig) Post(label string, shootContext abstractions.ShootContext, u *url.URL, headers map[string]string, request interface{}, result interface{}, logFullResponse bool) (err error) {
	err = c.enrichHeaders(headers)
	if err != nil {
		return err
	}
	return c.SendRequest(label, shootContext, "POST", u, headers, request, result, logFullResponse)
}

func (c *ClientConfig) Delete(label string, shootContext abstractions.ShootContext, url *url.URL, logFullResponse bool) error {
	headers := map[string]string{}
	err := c.enrichHeaders(headers)
	if err != nil {
		return err
	}
	return c.SendRequest(label, shootContext, "DELETE", url, headers, nil, nil, logFullResponse)
}

func (c *ClientConfig) SendRequest(label string, shootContext abstractions.ShootContext, method string, url *url.URL, headers map[string]string, request interface{}, result interface{}, logFullResponse bool) error {
	if rpsLimiter, ok := c.rpsLimiter.GetSemaphore(label); ok {
		if !rpsLimiter.TryAcquire(1) {
			logger := shootContext.GetLogger()
			labelField := zap.String("label", label)
			logger.Info("rps limiter exceeded limit", labelField)
			err := rpsLimiter.Acquire(shootContext.GetContext(), 1)
			if err != nil {
				logger.Error("rps limiter failed", labelField, zap.Error(err))
				return err
			}
			logger.Info("rps limiter accept request", labelField)
		}
		defer rpsLimiter.Release(1)
	} else {
		logger := shootContext.GetLogger()
		logger.WithOptions(zap.AddStacktrace(zapcore.ErrorLevel)).Warn("label unknown", zap.String("label", label))
	}
	req, err := BuildHTTPRequest(method, request, url, headers)
	if err != nil {
		return err
	}
	return c.requestProcessor.SendRequest(label, shootContext, req, request, result, logFullResponse, JSONProcessor)
}

func logRequest(label string, contextFields []zap.Field, statusCode *int, request *http.Request, requestBody interface{}, responseField *zap.Field) {
	logFields := append(toRequestIndexFields(statusCode, request), contextFields...)
	GetRequestIndexLogger().Info(label, logFields...)
	GetRequestLogger().Info(label, appendNonIndexFields(logFields, requestBody, request.URL, responseField)...)
}

func processRequestResult(resp *http.Response, res interface{}, logFullResponse bool, processor abstractions.BodyProcessor) (field *zap.Field, err error) {
	buffer := &bytes.Buffer{}
	_, err = io.Copy(buffer, resp.Body)
	if err != nil {
		return nil, err
	}

	buf := buffer.Bytes()
	if buf != nil {
		err = processor(buf, res)
	} else {
		return nil, errors.New("cannot create buffer")
	}

	var f zap.Field
	if logFullResponse {
		f = zap.String("response", string(buf))
	} else {
		f = zap.Reflect("response", res)
	}

	return &f, err
}

func appendNonIndexFields(fields []zap.Field, request interface{}, url *url.URL, responseField *zap.Field) []zap.Field {
	fields = append([]zap.Field{zap.String("url", url.String())}, fields...)
	if request != nil {
		fields = append(fields, zap.Reflect("request", request))
	}
	if responseField != nil {
		fields = append(fields, *responseField)
	}

	return fields
}

func toRequestIndexFields(statusCode *int, request *http.Request) []zap.Field {
	var logFields = []zap.Field{zap.String("req-id", ExtractReqID(request.Header))}
	if statusCode != nil {
		logFields = append(logFields,
			zap.Int("statusCode", *statusCode))
	}
	return logFields
}
