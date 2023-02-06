package transportmodel

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

func TestTransportModelStorage_Basic(t *testing.T) {
	transportModel := rasp.TTransportModel{
		Id:     1,
		Code:   "КОД",
		CodeEn: "CODE",
	}

	tmStorage := NewTransportModelStorage()
	tmStorage.PutTransportModel(&transportModel)

	assert.Equal(t, &transportModel, tmStorage.GetByID(1), "getByID")
	assert.Equal(t, &transportModel, tmStorage.GetByCode("CODE"), "getByCode")
	assert.Equal(t, &transportModel, tmStorage.GetByCode("КОД"), "getByCodeEn")
}
