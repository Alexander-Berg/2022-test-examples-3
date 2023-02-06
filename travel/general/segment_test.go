package segment

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_emptySegments(t *testing.T) {
	emptySegments := SegmentGroup{}
	result := GenerateRoutes(emptySegments)
	assert.Equal(t, []SegmentGroup{}, result)
}
