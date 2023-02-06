package app

import (
	"testing"

	"github.com/stretchr/testify/assert"

	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

func TestPoints(t *testing.T) {

	app, appClose, err := NewTestApp(t, nil)
	if !assert.NoError(t, err) {
		return
	}
	defer appClose()

	t.Run("where check", func(t *testing.T) {
		points, status := app.GetPointsWhere("code:c213,s9746351")
		assert.Equal(t, pb.EStatus_STATUS_OK.String(), status.Status.String())
		assert.NotNil(t, points.Data)
		assert.Len(t, points.Data, 2)
		assert.Equal(t, "авт.вкз. Москва, автостанция Новоясеневская, г. Москва, Москва и Московская область, Россия",
			points.Data[1].Description)
		assert.Equal(t, "Europe/Moscow", points.Data[0].Timezone)
	})

	t.Run("best_geo_id check", func(t *testing.T) {
		points, status := app.GetPointsWhere("best_geo_id:36,11063")
		assert.Equal(t, pb.EStatus_STATUS_OK.String(), status.Status.String())
		assert.Len(t, points.Data, 2)
		assert.Equal(t, "г. Ставрополь, Ставропольский край, Россия", points.Data[0].Description)
		assert.Equal(t, "c36", points.Data[0].Code)
		assert.Equal(t, int32(11063), points.Data[1].BestGeoID)
	})

	t.Run("pointkey for disputed territory", func(t *testing.T) {
		point, status := app.GetPointByPointkey("c959")
		assert.Equal(t, pb.EStatus_STATUS_OK.String(), status.Status.String())
		assert.Equal(t, "г. Севастополь, Крым", point.Description)
	})

	t.Run("404 check", func(t *testing.T) {
		_, status := app.GetPointByPointkey("c11111111111111111")
		assert.Equal(t, pb.EStatus_STATUS_NOT_FOUND.String(), status.Status.String())
	})
}
