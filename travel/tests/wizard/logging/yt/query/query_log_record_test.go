package query

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/parameters"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/parameters/dynamic"
	queryLog "a.yandex-team.ru/travel/avia/wizard/pkg/wizard/logging/yt/query"
)

func TestNewQueryLogRecord_TakeReqIDsFromQueryParameters(t *testing.T) {
	queryParameters := parameters.QueryParameters{MainReqID: "123", ReqID: "456", JobID: "789", AviaDynamic: &dynamic.AviaDynamic{}}

	queryLogRecord := queryLog.NewRecord(&queryParameters, "", "normalized")

	assert.Equal(t, queryLogRecord.MainReqID, queryParameters.MainReqID)
	assert.Equal(t, queryLogRecord.ReqID, queryParameters.ReqID)
	assert.Equal(t, queryLogRecord.JobID, queryParameters.JobID)
	assert.Equal(t, *queryLogRecord.FlightNumber, "normalized")
}
