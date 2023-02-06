package zephyrlib

import (
	"testing"

	"github.com/stretchr/testify/assert"

	pb "a.yandex-team.ru/search/zephyr/proto"
)

func TestAccessStorage_ResolveKey(t *testing.T) {
	client := NewStorageClientMock()
	s := NewAccessStorage(client)

	client.SetKey("alpha", "test", "production")
	client.SetKey("bravo", "test", "dev")

	for _, k := range []*pb.AccessKey{
		{Key: "alpha", Project: "test", Stage: "production", Valid: true},
		{Key: "bravo", Project: "test", Stage: "dev", Valid: true},
		{Key: "charlie"},
	} {
		project, stage, err := s.ResolveKey(k.Key)
		assert.Equal(t, k.Project, project, k)
		assert.Equal(t, k.Stage, stage, k)

		if k.Valid {
			assert.NoError(t, err, k)
		} else {
			assert.Error(t, err, k)
		}
	}
}
