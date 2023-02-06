package app

import (
	"testing"

	"github.com/stretchr/testify/assert"

	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

func TestStatusWithMessage(t *testing.T) {
	errorStatus := NewStatusWithMessage(pb.EStatus_STATUS_EXTERNAL_ERROR, "something happened")
	okStatus := NewStatusWithMessage(pb.EStatus_STATUS_OK, "everything fine")

	t.Run("String", func(t *testing.T) {
		assert.Equal(t, errorStatus.String(), "STATUS_EXTERNAL_ERROR: something happened")
	})

	t.Run("Ok", func(t *testing.T) {
		assert.False(t, errorStatus.Ok())
		assert.True(t, okStatus.Ok())
	})
}
