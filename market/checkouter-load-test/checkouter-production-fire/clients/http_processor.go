package clients

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"bytes"
	"encoding/json"
	"go.uber.org/zap"
	"net/http"
	"net/url"
	"time"
)

type requestProcessor struct {
	client http.Client
}

var JSONProcessor abstractions.BodyProcessor = json.Unmarshal

func NewRequestProcessor() abstractions.RequestProcessor {
	return &requestProcessor{client: http.Client{Timeout: time.Second * 30}}
}

func BuildHTTPRequest(method string, request interface{}, url *url.URL, headers map[string]string) (result *http.Request, err error) {
	if request != nil {
		var jsonBytes []byte
		jsonBytes, err = json.Marshal(request)
		if err != nil {
			return nil, err
		}
		bodyReader := bytes.NewReader(jsonBytes)
		result, err = http.NewRequest(method, url.String(), bodyReader)
		if err != nil {
			return nil, err
		}
		result.Header.Add("Content-Type", JSON)
	} else {
		result, err = http.NewRequest(method, url.String(), nil)
		if err != nil {
			return nil, err
		}
	}

	for k, v := range headers {
		result.Header.Add(k, v)
	}

	return result, nil
}

func (c *requestProcessor) SendRequest(label string, shootContext abstractions.ShootContext, req *http.Request, request interface{}, result interface{}, logFullResponse bool, processor abstractions.BodyProcessor) error {
	ctx := shootContext.GetContext()
	logger := shootContext.GetLogger()
	logFields := shootContext.GetLogFields()

	sample := shootContext.Acquire(label)
	defer shootContext.Measure(sample)

	resp, err := c.client.Do(req.WithContext(ctx))
	if err != nil {
		logger.Warn("unsuccessful state", zap.Error(err))
		sample.SetProtoCode(-1)
		sample.SetErr(err)
		logRequest(label, logFields, nil, req, request, nil)
		return err
	}
	if resp.StatusCode != 200 {
		logger.Warn("unsuccessful state", zap.Int("httpCode", resp.StatusCode))
	}
	sample.SetProtoCode(resp.StatusCode)

	responseField, err := processRequestResult(resp, result, logFullResponse, processor)
	logRequest(label, logFields, &resp.StatusCode, req, request, responseField)
	return err
}
