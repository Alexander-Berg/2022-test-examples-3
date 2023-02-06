package useractions

import (
	"encoding/json"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"google.golang.org/protobuf/proto"

	uapb "a.yandex-team.ru/logbroker/unified_agent/plugins/grpc_input/proto"
)

type mockUnifiedAgent struct {
	mock.Mock
}

func (m *mockUnifiedAgent) Send(proto.Message, []*uapb.Request_MessageMetaItem) error {
	panic("implement me")
}

func (m *mockUnifiedAgent) SendJSON(message interface{}, _ []*uapb.Request_MessageMetaItem) error {
	s, err := json.Marshal(message)
	fmt.Println(string(s))
	return err
}

func (m *mockUnifiedAgent) GetAck() error {
	panic("implement me")
}

func (m *mockUnifiedAgent) Close() error {
	panic("implement me")
}

// Checks that LogSubscribe records will be successfully serialized as JSON
func TestLogger_LogSubscribeSerializable(t *testing.T) {
	claim := assert.New(t)
	unifiedclient := &mockUnifiedAgent{}
	err := NewLogger(unifiedclient).LogSubscribe(
		"vasily@example.com",
		"payment",
		"avia",
		"ru",
		"ru",
		true,
		map[string]string{"a": "b", "c": "d"},
		"passport",
		"yandexuid",
	)
	claim.NoError(err)
}
