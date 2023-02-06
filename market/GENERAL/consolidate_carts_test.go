package consolidatecarts

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/graph"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestConsolidateCarts(t *testing.T) {
	{
		// Стандартный кейс
		var data []CartPath
		data = append(data, CartPath{
			Label: "cart1",
			// Создаем обычный путь WH(11)->MV(12)->WH(13)
			Path: newPath(1, 3, nil, 0),
			Dates: []string{
				"2022-01-02",
				"2022-01-03",
				"2022-01-04",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart2",
			// Путь WH(21)->MV(22)->WH(23)->MV(24) и добавляем к нему 1 сегмент из прошлого маршрута
			// Получаем WH(21)->MV(22)->WH(23)->MV(24)->WH(13)
			Path: newPath(2, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-03",
				"2022-01-04",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})

		// В данному кейсе у нас оба маршрута имеют общую точку WH(13)
		// 				   WH(11)->MV(12)      при этом данный маршрут доходит до клиента
		//					     		 \     в эти числа [2,3,4]
		//								  WH(13) // 		ИТОГ -> группа из 2 заказов может приехать [3,4]
		//								 /     а этот маршрут в числа [3,4]
		// WH(21)->MV(22)->WH(23)->MV(24)
		wantGroups := []*cr.ConsolidationGrouping{
			{
				// Оптимальная группа, все товары из корзины могут быть привезены 3 и 4 числа
				CartLabels:     []string{"cart1", "cart2"},
				AvailableDates: []string{"2022-01-03", "2022-01-04"},
			},
		}
		groups := ConsolidateCarts(data)
		require.Equal(t, wantGroups, groups, "Кейс с 1 группой на 2/2 корзины")
	}

	{
		// Кейс с 2 группами, которые не пересекаются по датам и корзинам
		var data []CartPath
		data = append(data, CartPath{
			Label: "cart1",
			// Создаем обычный путь WH(11)->MV(12)->WH(13)
			Path: newPath(1, 3, nil, 0),
			Dates: []string{
				"2022-01-01",
				"2022-01-02",
				"2022-01-03",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart2",
			// Путь WH(21)->MV(22)->WH(23)->MV(24) и добавляем к нему 1 сегмент из 1 маршрута
			// Получаем WH(21)->MV(22)->WH(23)->MV(24)->WH(13)
			Path: newPath(2, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-02",
				"2022-01-03",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart3",
			// Путь WH(31)->MV(32)->WH(33)->MV(34) и добавляем к нему 1 сегмент из 1 маршрута
			// Получаем WH(31)->MV(32)->WH(33)->MV(34)->WH(13)
			Path: newPath(3, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-04",
				"2022-01-05",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart4",
			// Путь WH(41)->MV(42)->WH(43)->MV(44) и добавляем к нему 1 сегмент из 1 маршрута
			// Получаем WH(41)->MV(42)->WH(43)->MV(44)->WH(13)
			Path: newPath(4, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-04",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})

		// В данному кейсе у нас 4 маршрута имеют общую точку WH(13)
		// 1  	             WH(11)->MV(12) (1,2,3 числа) ↘
		// 2 WH(31)->MV(32)->WH(33)->MV(34) (2,3 числа)   ↘
		//								      				→ WH(13)
		// 3 WH(31)->MV(32)->WH(33)->MV(34) (4,5 числа)   ↗
		// 4 WH(41)->MV(42)->WH(43)->MV(44) (4 числа)     ↗
		//
		// В данном кейсе пересечение есть только у первой корзины со второй на датах 2 и 3
		// и у корзины третей и четвертой на дате 4
		wantGroups := []*cr.ConsolidationGrouping{
			{
				CartLabels:     []string{"cart1", "cart2"},
				AvailableDates: []string{"2022-01-02", "2022-01-03"},
			},
			{
				CartLabels:     []string{"cart3", "cart4"},
				AvailableDates: []string{"2022-01-04"},
			},
		}
		groups := ConsolidateCarts(data)
		require.Equal(t, wantGroups, groups, "Кейс с 2 не пересекающимися группами по 2 корзины")
	}

	{
		// Пересекающиеся группы
		var data []CartPath
		data = append(data, CartPath{
			Label: "cart1",
			// Создаем обычный путь WH(11)->MV(12)->WH(13)
			Path: newPath(1, 3, nil, 0),
			Dates: []string{
				"2022-01-01",
				"2022-01-02",
				"2022-01-03",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart2",
			// Путь WH(21)->MV(22)->WH(23)->MV(24) и добавляем к нему 1 сегмент из 1 маршрута
			// Получаем WH(21)->MV(22)->WH(23)->MV(24)->WH(13)
			Path: newPath(2, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-02",
				"2022-01-03",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart3",
			// Путь WH(31)->MV(32)->WH(33)->MV(34) и добавляем к нему 1 сегмент из 1 маршрута
			// Получаем WH(31)->MV(32)->WH(33)->MV(34)->WH(13)
			Path: newPath(3, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-03",
				"2022-01-04",
				"2022-01-05",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart4",
			// Путь WH(41)->MV(42)->WH(43)->MV(44) и добавляем к нему 1 сегмент из 1 маршрута
			// Получаем WH(41)->MV(42)->WH(43)->MV(44)->WH(13)
			Path: newPath(4, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-04",
				"2022-01-05",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart5",
			// Путь WH(51)->MV(52)->WH(53)->MV(54) и добавляем к нему 1 сегмент из 1 маршрута
			// Получаем WH(51)->MV(52)->WH(53)->MV(54)->WH(13)
			Path: newPath(4, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-05",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})

		// В данному кейсе у нас 5 маршрута имеют общую точку WH(13)
		// 1 			     WH(11)->MV(12) (1,2,3 числа) ↘
		// 2 WH(21)->MV(22)->WH(23)->MV(24) (2,3 числа)   ↘
		// 3 WH(31)->MV(32)->WH(33)->MV(34) (3,4,5 числа)     → WH(13)
		// 4 WH(41)->MV(42)->WH(43)->MV(44) (4,5 числа)   ↗
		// 5 WH(51)->MV(52)->WH(53)->MV(54) (5 числа)     ↗
		//
		// Корзина 3 имеет пересечение с корзинами 4 и 5,
		// но мы берем приоритет на более раннюю доставку в 1 группе корзин
		// поэтому маршрут 3 попадает только в 1 группу
		wantGroups := []*cr.ConsolidationGrouping{
			{
				CartLabels:     []string{"cart1", "cart2", "cart3"},
				AvailableDates: []string{"2022-01-03"},
			},
			{
				CartLabels:     []string{"cart4", "cart5"},
				AvailableDates: []string{"2022-01-05"},
			},
		}
		groups := ConsolidateCarts(data)
		require.Equal(t, wantGroups, groups, "Кейс с 2 пересекающимися группами")
	}

	{
		// Корзина с не пересекающимся маршрутом
		var data []CartPath
		data = append(data, CartPath{
			Label: "cart1",
			// Создаем обычный путь WH(11)->MV(12)->WH(13)
			Path: newPath(1, 3, nil, 0),
			Dates: []string{
				"2022-01-01",
				"2022-01-02",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart2",
			// Путь WH(21)->MV(22)->WH(23)->MV(24) и добавляем к нему 1 сегмент из 1 маршрута
			// Получаем WH(21)->MV(22)->WH(23)->MV(24)->WH(13)
			Path: newPath(2, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-01",
				"2022-01-02",
				"2022-01-03",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart3",
			// Путь WH(31)->MV(32)->WH(33)->MV(34)->WH(35)
			Path: newPath(3, 5, nil, 0),
			Dates: []string{
				"2022-01-03",
				"2022-01-04",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart4",
			// Путь WH(41)->MV(42)->WH(43)->MV(44) и добавляем к нему 1 сегмент из 1 маршрута
			// Получаем WH(41)->MV(42)->WH(43)->MV(44)->WH(13)
			Path: newPath(4, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-01",
				"2022-01-04",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})

		// В данному кейсе у нас 4 маршрута имеют общую точку WH(13)
		// 1  	             WH(11)->MV(12) (1,2 числа)   ↘
		// 2 WH(31)->MV(32)->WH(33)->MV(34) (1,2,3 числа) ↘
		//								      				→ WH(13)
		// 4 WH(41)->MV(42)->WH(43)->MV(44) (1,4 числа)   ↗
		//
		// 3 WH(31)->MV(32)->WH(33)->MV(34)->WH(35) (3,4 числа) - не имеет общих сегментов
		//
		// Маршрут без общих сегментов уходит в остаточную группу
		wantGroups := []*cr.ConsolidationGrouping{
			{
				CartLabels:     []string{"cart1", "cart2", "cart4"},
				AvailableDates: []string{"2022-01-01"},
			},
			{
				CartLabels:     []string{"cart3"},
				AvailableDates: []string{"2022-01-03", "2022-01-04"},
			},
		}
		groups := ConsolidateCarts(data)
		require.Equal(t, wantGroups, groups, "Кейс корзина с не пересекающимся маршрутом")
	}

	{
		// Пересекающиеся группы, с остаточной тривиальной группой
		var data []CartPath
		data = append(data, CartPath{
			Label: "cart1",
			// Создаем обычный путь WH(11)->MV(12)->WH(13)
			Path: newPath(1, 3, nil, 0),
			Dates: []string{
				"2022-01-01",
				"2022-01-02",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart2",
			// Путь WH(21)->MV(22)->WH(23)->MV(24) и добавляем к нему 1 сегмент из 1 маршрута
			// Получаем WH(21)->MV(22)->WH(23)->MV(24)->WH(13)
			Path: newPath(2, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-01",
				"2022-01-02",
				"2022-01-03",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart3",
			// Путь WH(31)->MV(32)->WH(33)->MV(34) и добавляем к нему 1 сегмент из 1 маршрута
			// Получаем WH(31)->MV(32)->WH(33)->MV(34)->WH(13)
			Path: newPath(3, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-03",
				"2022-01-04",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})
		data = append(data, CartPath{
			Label: "cart4",
			// Путь WH(41)->MV(42)->WH(43)->MV(44) и добавляем к нему 1 сегмент из 1 маршрута
			// Получаем WH(41)->MV(42)->WH(43)->MV(44)->WH(13)
			Path: newPath(4, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-01",
				"2022-01-04",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
		})

		// В данному кейсе у нас 4 маршрута имеют общую точку WH(13)
		// 1  	             WH(11)->MV(12) (1,2 числа)   ↘
		// 2 WH(31)->MV(32)->WH(33)->MV(34) (1,2,3 числа) ↘
		//								      				→ WH(13)
		// 3 WH(31)->MV(32)->WH(33)->MV(34) (3,4 числа)   ↗
		// 4 WH(41)->MV(42)->WH(43)->MV(44) (1,4 числа)   ↗
		//
		// В данном кейсе имеем 3 корзины, которые могут быть доставлены в 1 дату.
		// Приоритет берем на более раннюю дату, поэтому корзина 4 попадает в 1 группу.
		// Остаточная корзина 3 не может попасть в 1 группу, поэтому она по остаточному принципу
		// попадает в группу 2
		wantGroups := []*cr.ConsolidationGrouping{
			{
				CartLabels:     []string{"cart1", "cart2", "cart4"},
				AvailableDates: []string{"2022-01-01"},
			},
			{
				CartLabels:     []string{"cart3"},
				AvailableDates: []string{"2022-01-03", "2022-01-04"},
			},
		}
		groups := ConsolidateCarts(data)
		require.Equal(t, wantGroups, groups, "Кейс с 2 пересекающимися группами, с остаточной тривиальной")
	}
	{
		// 100 корзин в 1 регион
		var data []CartPath
		wantGroups := []*cr.ConsolidationGrouping{
			{
				AvailableDates: []string{"2022-01-01", "2022-01-04"},
			},
			{
				AvailableDates: []string{"2022-01-01", "2022-01-04"},
			},
		}
		for i := 0; i < 100; i++ {
			cart := fmt.Sprintf("cart%d", i)
			data = append(data, CartPath{
				Label: cart,
				// Путь WH(11)->MV(12)->WH(13)
				Path: newPath(1, 3, nil, 0),
				Dates: []string{
					"2022-01-01",
					"2022-01-04",
				},
				GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
			})
			if i < 64 {
				wantGroups[0].CartLabels = append(wantGroups[0].CartLabels, cart)
			} else {
				wantGroups[1].CartLabels = append(wantGroups[1].CartLabels, cart)
			}
		}
		groups := ConsolidateCarts(data)
		require.Equal(t, wantGroups, groups)
	}

	{
		// Разные адреса
		var data []CartPath
		data = append(data, CartPath{
			Label: "cart1",
			// Создаем обычный путь WH(11)->MV(12)->WH(13)
			Path: newPath(1, 3, nil, 0),
			Dates: []string{
				"2022-01-02",
				"2022-01-03",
				"2022-01-04",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1.5, Lon: 1.5},
		})
		data = append(data, CartPath{
			Label: "cart2",
			// Путь WH(21)->MV(22)->WH(23)->MV(24) и добавляем к нему 1 сегмент из прошлого маршрута
			// Получаем WH(21)->MV(22)->WH(23)->MV(24)->WH(13)
			Path: newPath(2, 4, data[0].Path, 1),
			Dates: []string{
				"2022-01-03",
				"2022-01-04",
			},
			GpsCoords: &cr.GpsCoords{Lat: 1.4, Lon: 1.4},
		})

		// В данному кейсе у нас оба маршрута имеют общую точку WH(13)
		// 				   WH(11)->MV(12)      при этом данный маршрут доходит до клиента
		//					     		 \     в эти числа [2,3,4]
		//								  WH(13)  ------------- но едут на разные адреса, поэтому не группируем
		//								 /     а этот маршрут в числа [3,4]
		// WH(21)->MV(22)->WH(23)->MV(24)
		wantGroups := []*cr.ConsolidationGrouping{
			{
				CartLabels:     []string{"cart1"},
				AvailableDates: []string{"2022-01-02", "2022-01-03", "2022-01-04"},
			},
			{
				CartLabels:     []string{"cart2"},
				AvailableDates: []string{"2022-01-03", "2022-01-04"},
			},
		}
		groups := ConsolidateCarts(data)
		require.Equal(t, wantGroups, groups, "Кейс с разными адресами")
	}
}

func newPath(pathNumber int, pathLen int, jointPath []*graph.Node, jointNodesCount int) []*graph.Node {
	segmentType := graph.SegmentTypeWarehouse
	var path []*graph.Node
	for i := 1; i <= pathLen; i++ {
		node := &graph.Node{
			LogisticSegment: graph.LogisticSegment{
				ID:   int64(i + (10 * pathNumber)),
				Type: segmentType,
			},
		}

		if segmentType == graph.SegmentTypeWarehouse {
			segmentType = graph.SegmentTypeMovement
		} else {
			segmentType = graph.SegmentTypeWarehouse
		}

		path = append(path, node)
	}

	if jointPath != nil {
		path = append(path, jointPath[len(jointPath)-jointNodesCount:]...)
	}

	return path
}
