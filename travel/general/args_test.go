package sandboxplanner

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"os"
	"testing"
)

func TestGetArgsWithEnv(t *testing.T) {
	_ = os.Setenv(ArgReplacesEnvKey, `{"replace-value": "new-value"}`)
	defer os.Unsetenv(ArgReplacesEnvKey)

	testCases := []struct {
		srcValue      string
		expectedValue string
	}{
		{"replace-value", "new-value"},
		{"new-value", "new-value"},
		{"other-value", "other-value"},
	}

	for _, tc := range testCases {
		t.Run(tc.srcValue, func(t *testing.T) {
			args := []string{tc.srcValue, "middle-value", tc.srcValue}
			args, err := GetReplacedArgsFromEnv(args)
			require.NoError(t, err)

			assert.Equal(t, args[0], tc.expectedValue)
			assert.Equal(t, args[1], "middle-value")
			assert.Equal(t, args[2], tc.expectedValue)
		})
	}
}

func TestReplaceInplace(t *testing.T) {
	_ = os.Setenv(ArgReplacesEnvKey, `{"replace-value": "new-value"}`)
	defer os.Unsetenv(ArgReplacesEnvKey)

	defer resetArgs(os.Args)
	os.Args = []string{
		"program",
		"-first",
		"replace-value",
		"-second",
		"other",
	}

	expected := []string{
		"program",
		"-first",
		"new-value",
		"-second",
		"other",
	}

	err := ReplaceArgsFromEnv()
	require.NoError(t, err)
	assert.Equal(t, os.Args, expected)
}

func TestMissingEnv(t *testing.T) {
	defer resetArgs(os.Args)
	os.Args = []string{
		"program",
		"-test-key",
		"replace-value",
	}
	expected := os.Args[:]

	err := ReplaceArgsFromEnv()
	require.NoError(t, err)
	assert.Equal(t, os.Args, expected)
}

func TestBadEnv(t *testing.T) {
	defer resetArgs(os.Args)

	_ = os.Setenv(ArgReplacesEnvKey, `{"}`)
	err := ReplaceArgsFromEnv()
	assert.Error(t, err)
}

func TestPartReplacing(t *testing.T) {
	paramsMap := replaceParamsMapType{
		"part1": "new-part1",
		"part3": "new-part3",
	}

	arg := "part1.part2.part3"
	assert.Equal(t, replaceArg(arg, paramsMap), "new-part1.part2.new-part3")
}

func resetArgs(args []string) {
	os.Args = args
}
