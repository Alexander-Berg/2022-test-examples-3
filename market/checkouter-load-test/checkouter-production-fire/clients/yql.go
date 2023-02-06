package clients

import (
	"a.yandex-team.ru/library/go/slices"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"path"
	"strings"
	"time"
)

type YQLImpl struct {
	URL      url.URL
	YQLToken string
}

var emptyMap = map[string]string{}

func (yql YQLImpl) ExecuteQuery(query string, requestProcessor abstractions.RequestProcessor, ctx abstractions.ShootContext, token string) (*[]abstractions.ResultRow, error) {
	req := &abstractions.QueryRequest{Action: "RUN", Type: "SQLv1", Content: query}
	body, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("cannot marshal: '%v', result: '%v', error: '%v'", req, body, err)
	}
	op, err := yql.PostOperation(body, requestProcessor, ctx, token)
	if err != nil {
		return nil, fmt.Errorf("cannot post operation, body: '%v', error: '%v'", string(body), err)
	}
	runningStatuses := []string{"IDLE", "PENDING", "RUNNING"}
	for {
		isRunning, _ := slices.Contains(runningStatuses, op.Status)
		if !isRunning {
			break
		}
		fmt.Println("Operation is not finished yet. Waiting...")
		time.Sleep(2 * time.Second)
		op, err = yql.GetOperation(op.ID, requestProcessor, ctx, token)
		if err != nil {
			return nil, err
		}
	}

	if op.Status != "COMPLETED" {
		return nil, errors.New(op.Status)
	}

	return yql.GetOperationData(op.ID, requestProcessor, ctx, token)
}

func (yql YQLImpl) GetOperations(requestProcessor abstractions.RequestProcessor, ctx abstractions.ShootContext, token string) (*[]abstractions.OperationDto, error) {
	res := &[]abstractions.OperationDto{}
	err := yql.executeRequest("get-operations", "GET", "operations", nil, emptyMap, res, requestProcessor, JSONProcessor, ctx, token)
	if err != nil {
		return nil, fmt.Errorf("cannot get yql operations, response: '%v', error: '%v'", res, err)
	}

	return res, nil
}

func (yql YQLImpl) PostOperation(body []byte, requestProcessor abstractions.RequestProcessor, ctx abstractions.ShootContext, token string) (*abstractions.OperationDto, error) {
	res := &abstractions.OperationDto{}

	err := yql.executeRequest("post-operations", "POST", "operations", bytes.NewReader(body), emptyMap, res, requestProcessor, JSONProcessor, ctx, token)
	if err != nil {
		return nil, err
	}

	return res, nil
}

func (yql YQLImpl) GetOperation(id string, requestProcessor abstractions.RequestProcessor, ctx abstractions.ShootContext, token string) (*abstractions.OperationDto, error) {
	res := &abstractions.OperationDto{}

	err := yql.executeRequest("get-operation-by-id", "GET", "operations/"+id, nil, emptyMap, res, requestProcessor, JSONProcessor, ctx, token)
	if err != nil {
		return nil, err
	}

	return res, nil
}

func (yql YQLImpl) GetOperationData(id string, requestProcessor abstractions.RequestProcessor, ctx abstractions.ShootContext, token string) (*[]abstractions.ResultRow, error) {
	res := &[]abstractions.ResultRow{}

	err := yql.executeRequest("get-result-by-operation-id", "GET", "operations/"+id+"/results_data", nil,
		map[string]string{
			"format":      "json",
			"write_index": "0",
		}, res, requestProcessor, yqlMultipleJSONProcessor, ctx, token)
	if err != nil {
		return nil, err
	}

	return res, nil
}

func (yql YQLImpl) executeRequest(
	label string,
	method string,
	path string,
	body io.Reader,
	params map[string]string,
	result interface{},
	requestProcessor abstractions.RequestProcessor,
	bodyProcessor abstractions.BodyProcessor,
	ctx abstractions.ShootContext,
	token string,
) error {
	req, err := yql.makeRequest(method, path, body, ctx, token)
	if err != nil {
		return err
	}

	query := req.URL.Query()
	for k, v := range params {
		query.Add(k, v)
	}

	req.URL.RawQuery = query.Encode()

	return requestProcessor.SendRequest("yql-"+label, ctx, req, body, result, true, bodyProcessor)
}

func (yql YQLImpl) makeRequest(method string,
	path_ string,
	body io.Reader,
	ctx abstractions.ShootContext,
	token string,
) (*http.Request, error) {
	u := yql.URL
	u.Path += path.Join("/", path_)
	req, err := http.NewRequestWithContext(ctx.GetContext(), method, u.String(), body)
	if err != nil {
		return nil, err
	}
	req.Header.Add("Authorization", "OAuth "+token)
	return req, nil
}

func yqlMultipleJSONProcessor(buf []byte, res interface{}) error {
	var result = res.(*[]abstractions.ResultRow)
	var arr = *result

	for _, raw := range strings.Split(string(buf), "\n") {
		row := abstractions.ResultRow{}
		err := json.Unmarshal([]byte(raw), &row.Values)
		if err == nil {
			arr = append(arr, row)
		}
	}
	*result = arr

	return nil
}
