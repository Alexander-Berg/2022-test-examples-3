package expmap

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestParseExperimentMap(t *testing.T) {
	em, err := ParseExperimentMap("1001111:fix11;1001112:k-armed")
	assert.NoError(t, err, "parse error")
	exp := ExperimentMap{
		"1001111": "fix11",
		"1001112": "k-armed",
	}
	assert.Equal(t, exp, em)
}

func TestParseExpBoxes(t *testing.T) {
	b, err := ParseExpBoxes("240483,0,-1;240487,0,-1")
	assert.NoError(t, err, "parse error")
	assert.Equal(t, []string{"240483", "240487"}, b)
}

func TestExpValueFromTestIds(t *testing.T) {
	exp := ExperimentMap{
		"1001111": "fix11",
		"1001112": "k-armed",
	}
	testIds := []string{"240483", "1001111", "240487", "1001111"}
	v, err := exp.expValueFromTestIds(testIds)
	assert.NoError(t, err)
	assert.Equal(t, "fix11", v)
}
