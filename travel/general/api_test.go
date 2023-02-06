package connector

import (
	"encoding/json"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

func TestJSONTime(t *testing.T) {
	t.Run("Marshal_time", func(t *testing.T) {
		v, err := json.Marshal(JSONTime(time.Date(2000, 1, 1, 12, 0, 0, 0, time.UTC)))
		if assert.NoError(t, err) {
			assert.Equal(t, []byte("\"2000-01-01T12:00:00\""), v)
		}
	})

	t.Run("Marshal_zero", func(t *testing.T) {
		v, err := json.Marshal(JSONTime{})
		if assert.NoError(t, err) {
			assert.Equal(t, []byte("null"), v)
		}
	})

	t.Run("Unmarshal_time", func(t *testing.T) {
		var v JSONTime
		err := json.Unmarshal([]byte("\"2000-01-01T12:00:00\""), &v)
		if assert.NoError(t, err) {
			assert.Equal(t, JSONTime(time.Date(2000, 1, 1, 12, 0, 0, 0, time.UTC)), v)
		}
	})

	t.Run("Unmarshal_zero", func(t *testing.T) {
		var v JSONTime
		err := json.Unmarshal([]byte("null"), &v)
		if assert.NoError(t, err) {
			assert.True(t, v.Time().IsZero())
		}
	})
}

func TestRideBenefit(t *testing.T) {
	const sampleBenefit = RideBenefit(pb.EBenefitType_BENEFIT_TYPE_COFFEE)
	var sampleBenefitJSON, _ = json.Marshal(benefits[sampleBenefit.Unwrap()])

	t.Run("Marshal", func(t *testing.T) {
		v, err := json.Marshal(sampleBenefit)
		if assert.NoError(t, err) {
			assert.Equal(t, sampleBenefitJSON, v)
		}
	})

	t.Run("Marshal error", func(t *testing.T) {
		_, err := json.Marshal(RideBenefit(99))
		assert.EqualError(t, err, "json: error calling MarshalJSON for type connector.RideBenefit: unknown EBenefitType: 99")
	})

	t.Run("Unmarshal", func(t *testing.T) {
		var v RideBenefit
		err := json.Unmarshal(sampleBenefitJSON, &v)
		if assert.NoError(t, err) {
			assert.Equal(t, sampleBenefit, v)
		}
	})

	t.Run("Unmarshal error", func(t *testing.T) {
		var v RideBenefit
		err := json.Unmarshal([]byte("[]"), &v)
		assert.Error(t, err)
	})

	t.Run("Unmarshal unknown", func(t *testing.T) {
		var v RideBenefit
		err := json.Unmarshal([]byte("{\"id\": 99, \"name\": \"bad name\"}"), &v)
		if assert.NoError(t, err) {
			assert.Equal(t, RideBenefit(pb.EBenefitType_BENEFIT_TYPE_UNKNOWN), v)
		}
	})
}
