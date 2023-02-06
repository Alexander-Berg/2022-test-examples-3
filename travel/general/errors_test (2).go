package connector

import (
	"errors"
	"net/http"
	"testing"

	"github.com/stretchr/testify/assert"
)

var errorTestCases = [...]struct {
	Name           string
	ResponseStatus int
	ResponseBody   string
	Check          func(*testing.T, error) bool
}{
	{
		Name:           "ErrWithMetadata 422",
		ResponseStatus: http.StatusUnprocessableEntity,
		ResponseBody: `
{
    "message": "Invalid ride_segment_id",
    "error": {
        "message": "Invalid ride_segment_id",
        "code": 110
    }
}`,
		Check: func(t *testing.T, err error) bool {
			if assert.Error(t, err) {
				assert.Contains(t, err.Error(), ": connector error: Invalid ride_segment_id [Code = 110]")

				var innerErr ErrWithMetadata
				if assert.True(t, errors.As(err, &innerErr)) {
					return assert.Equal(t, ErrWithMetadata{
						Code:    110,
						Message: "Invalid ride_segment_id",
					}, innerErr)
				}
			}
			return false
		},
	},
	{
		Name:           "ErrWithMetadata 502",
		ResponseStatus: http.StatusBadGateway,
		ResponseBody: `
{
    "message": "error",
    "error": {
        "message": "error",
        "code": 100
    }
}`,
		Check: func(t *testing.T, err error) bool {
			if assert.Error(t, err) {
				assert.Contains(t, err.Error(), ": connector error: error [Code = 100]")

				var innerErr ErrWithMetadata
				if assert.True(t, errors.As(err, &innerErr)) {
					return assert.Equal(t, ErrWithMetadata{
						Code:    100,
						Message: "error",
					}, innerErr)
				}
			}
			return false
		},
	},
	{
		Name:           "ErrUnavailable",
		ResponseStatus: http.StatusInternalServerError,
		ResponseBody:   "non-json response",
		Check: func(t *testing.T, err error) bool {
			if assert.Error(t, err) {
				assert.Contains(t, err.Error(), ": connector unavailable: bad response code: 500")

				var innerErr ErrUnavailable
				if assert.True(t, errors.As(err, &innerErr)) {
					return assert.EqualError(t, innerErr, "connector unavailable: bad response code: 500")
				}
			}
			return false
		},
	},
}
