package metavalidator

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestValidateSign(t *testing.T) {
	validator := NewSignValidator("/upload/", "12345678")

	query := make(map[string]string)
	query["key"] = "testservice/testvideo"

	result, err := validator.Validate("58a0b47a294e1", query, "83f3760fd2bfbf0a25d979711d4b8df399c9e906e98dafaca645ca3911c0e2c9")
	require.NoError(t, err)
	require.True(t, result)
}

func TestParametersOrder(t *testing.T) {
	validator := NewSignValidator("/upload/", "12345678")

	q1 := make(map[string]string)
	q1["param1"] = "123"
	q1["param2"] = "456"

	result, err := validator.Validate("58a0b47a294e1", q1, "101437f21b2297e0ec1b901d5d27c83f2794938a6e0eee6c99afe263c6aee045")
	require.NoError(t, err)
	require.True(t, result)

	q2 := make(map[string]string)
	q2["param2"] = "456"
	q2["param1"] = "123"

	result, err = validator.Validate("58a0b47a294e1", q2, "101437f21b2297e0ec1b901d5d27c83f2794938a6e0eee6c99afe263c6aee045")
	require.NoError(t, err)
	require.True(t, result)
}
