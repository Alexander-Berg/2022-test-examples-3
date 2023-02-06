package helpers

import (
	"bytes"
	"testing"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/helpers"
	"a.yandex-team.ru/travel/proto/dicts/avia"
)

func TestWriteRead(t *testing.T) {
	message := avia.TSettlement{
		Id: 11,
	}

	buf := bytes.NewBufferString("")
	err := helpers.WritePtotobufIntoBuffer(buf, &message)

	if err != nil {
		t.Error(err)
		return
	}

	result, err := helpers.CutDataIntoProtobufRows(buf.Bytes())

	if err != nil {
		t.Error(err)
		return
	}

	if !assert.Equal(t, len(result), 1) {
		t.Errorf("Expected length of result 0. Got %v", len(result))
	}

	gotMessage := avia.TSettlement{}
	err = proto.Unmarshal(result[0], &gotMessage)

	if err != nil {
		t.Error(err)
		return
	}

	assert.Equal(t, message.Id, gotMessage.Id)
}

func TestWriteReadMany(t *testing.T) {
	buf := bytes.NewBufferString("")

	for _, i := range []int{1, 2, 3} {
		err := helpers.WritePtotobufIntoBuffer(buf, &avia.TSettlement{Id: int64(i)})
		if err != nil {
			t.Error(err)
		}
	}

	result, err := helpers.CutDataIntoProtobufRows(buf.Bytes())

	if err != nil {
		t.Error(err)
		return
	}

	if !assert.Equal(t, len(result), 3) {
		t.Errorf("Expected length of result 0. Got %v", len(result))
	}

	for i, raw := range result {
		gotMessage := avia.TSettlement{}
		err = proto.Unmarshal(raw, &gotMessage)

		if err != nil {
			t.Error(err)
			return
		}

		assert.Equal(t, gotMessage.Id, int64(i+1))
	}
}
