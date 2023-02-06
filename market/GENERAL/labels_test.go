package plugins

import (
	"a.yandex-team.ru/market/sre/tools/prostarter/internal/env"
	"a.yandex-team.ru/market/sre/tools/prostarter/internal/loader"
	"github.com/stretchr/testify/assert"
	"os"
	"testing"
)

func fullLabelEnv(value string) string {
	return env.Prefix + "_LABEL_" + value
}

func TestLabels(t *testing.T) {
	clearEnv := func() {
		_ = os.Unsetenv(fullLabelEnv("TEST_LABEL"))
		_ = os.Unsetenv(fullLabelEnv("TEST_LABEL_2"))
	}
	clearEnv()
	defer clearEnv()

	values := loader.Values{}

	err := Labels(values)
	assert.NoError(t, err)

	values.Labels = map[string]string{
		"test_label":   "1",
		"TEST_label-2": "2",
	}

	err = Labels(values)
	assert.NoError(t, err)
	assert.Equal(t, "1", os.Getenv(fullLabelEnv("TEST_LABEL")))
	assert.Equal(t, "2", os.Getenv(fullLabelEnv("TEST_LABEL_2")))
}
