package httputil

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestParseExperiments(t *testing.T) {
	t.Run("TrainsBanditType", func(t *testing.T) {
		res, err := ParseExperiments("{\"TRAINS_bandit_type\":\"fix11\"}")
		assert.NoError(t, err)
		assert.Equal(t, "fix11", res.TrainsBanditType)
	})
	t.Run("TrainsBanditType null", func(t *testing.T) {
		res, err := ParseExperiments("{\"TRAINS_bandit_type\": null}")
		assert.NoError(t, err)
		assert.Equal(t, "", res.TrainsBanditType)
	})
	t.Run("TrainsBanditType type error", func(t *testing.T) {
		_, err := ParseExperiments("{\"TRAINS_bandit_type\": 12}")
		assert.Error(t, err)
	})
	t.Run("TrainsBanditType with trash", func(t *testing.T) {
		res, err := ParseExperiments("{\"TRAINS_bandit_type\":\"fix11\", \"trash\": 11}")
		assert.NoError(t, err)
		assert.Equal(t, "fix11", res.TrainsBanditType)
	})
	t.Run("empty string", func(t *testing.T) {
		res, err := ParseExperiments("")
		assert.NoError(t, err)
		assert.Equal(t, "", res.TrainsBanditType)
	})
	t.Run("only trash", func(t *testing.T) {
		res, err := ParseExperiments("{\"trash\": null}")
		assert.NoError(t, err)
		assert.Equal(t, "", res.TrainsBanditType)
	})
	t.Run("only trash struct", func(t *testing.T) {
		res, err := ParseExperiments("{\"trash\": {\"field\": \"value\"}}")
		assert.NoError(t, err)
		assert.Equal(t, "", res.TrainsBanditType)
	})
	t.Run("invalid json error", func(t *testing.T) {
		_, err := ParseExperiments("{trash: {error}}")
		assert.Error(t, err)
	})
}
