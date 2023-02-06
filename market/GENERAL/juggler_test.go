package storage

import (
	"a.yandex-team.ru/market/sre/library/proto/juggler_pb"
	"github.com/golang/protobuf/proto"
	"github.com/google/go-cmp/cmp"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestJugglerEventStorage(t *testing.T) {
	events := []*juggler_pb.Event{
		{
			Host:    "test",
			Service: "ssh",
			Status:  "ok",
		},
		{
			Host:    "test4",
			Service: "web",
			Status:  "fail",
		},
	}

	storage := new(JugglerEventStorage)

	for _, event := range events {
		err := storage.PutEvent(event)
		assert.NoError(t, err)
	}

	savedEvents, errors := storage.GetAllEvents()

	assert.True(t, cmp.Equal(events, savedEvents, cmp.Comparer(proto.Equal)))
	assert.Empty(t, errors)
}
