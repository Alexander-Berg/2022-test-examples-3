package parameters

import (
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	wizardContext "a.yandex-team.ru/travel/avia/wizard/pkg/wizard/context"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/flags"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/parameters"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/parameters/checkers"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/parameters/dynamic"
	"a.yandex-team.ru/travel/avia/wizard/tests/wizard/mocks"
)

var (
	aviaDynamicParser     = new(mocks.AviaDynamicParserMock)
	flagsParser           = new(mocks.FlagsParserMock)
	queryParametersParser = parameters.NewQueryParametersParser(
		aviaDynamicParser,
		flagsParser,
		checkers.NewDummyChecker(),
		nil,
	)
)

func makeRequest(queryParameters map[string]string) *http.Request {
	q := make(url.Values)
	for k, v := range queryParameters {
		q.Set(k, v)
	}
	return httptest.NewRequest(http.MethodGet, "/?"+q.Encode(), nil)
}

func TestParseQueryParameters(t *testing.T) {
	rawAviaDynamic := "{}"
	rawFlags := ""
	aviaDynamicParser.On("Parse", rawAviaDynamic).Return((*dynamic.AviaDynamic)(nil), nil)
	flagsParser.On("Parse", rawFlags).Return(make(flags.Flags), nil)
	request := makeRequest(map[string]string{
		"main_reqid":   "123",
		"reqid":        "456",
		"geo_id":       "789",
		"from_id":      "c123",
		"to_id":        "s123",
		"lang":         "ru",
		"tld":          "123",
		"avia_dynamic": rawAviaDynamic,
	})
	request = request.WithContext(wizardContext.WithJobID(request.Context(), "0000"))

	queryParameters, err := queryParametersParser.Parse(request)
	require.NoError(t, err)

	assert.Equal(t, "123", queryParameters.MainReqID)
	assert.Equal(t, "456", queryParameters.ReqID)
	assert.Equal(t, "0000", queryParameters.JobID)
	assert.Equal(t, 789, queryParameters.GeoID)
	assert.Equal(t, "c123", *queryParameters.FromID)
	assert.Equal(t, "s123", *queryParameters.ToID)
	assert.Equal(t, "ru", queryParameters.Lang.String())
	assert.Equal(t, "123", queryParameters.Tld)
	assert.Equal(t, (*dynamic.AviaDynamic)(nil), queryParameters.AviaDynamic)

	aviaDynamicParser.AssertExpectations(t)
}
