package landings

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/handlers/responses"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/landings"
)

func TestSerializeTariff(t *testing.T) {
	secretKey := "secret"
	tariff := &responses.Tariff{
		Currency: "RUR",
		Price:    123.0,
	}
	createdAt := int32(123456)

	serializardTariff := landings.SerializeTariff(tariff, createdAt, secretKey)

	deserializedTariff, deserializedCreatedAt, err := landings.DeserializeTariff(serializardTariff, secretKey)

	assert.NoError(t, err)
	assert.Equal(t, tariff.Price, deserializedTariff.Price)
	assert.Equal(t, tariff.Currency, deserializedTariff.Currency)
	assert.Equal(t, createdAt, deserializedCreatedAt)
}
