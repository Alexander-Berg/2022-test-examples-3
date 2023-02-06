package test

import (
	"database/sql"
	"github.com/golang/mock/gomock"
	"testing"

	"a.yandex-team.ru/travel/avia/feature_flag_api/internal/configprovider"
	"a.yandex-team.ru/travel/avia/feature_flag_api/internal/dataprovider/mock"
	"a.yandex-team.ru/travel/avia/feature_flag_api/internal/db/mock"
	"a.yandex-team.ru/travel/avia/feature_flag_api/internal/indexbuilder"
	"a.yandex-team.ru/travel/avia/feature_flag_api/internal/models"
)

func TestIndexBuilder_Normal(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()

	fakeConnector := mock_db.NewMockIConnector(mockCtrl)
	fakeServiceProvider := mock_dataProvider.NewMockIServiceProvider(mockCtrl)
	fakeFlagProvider := mock_dataProvider.NewMockIFlagProvider(mockCtrl)
	fakeRelationsProvider := mock_dataProvider.NewMockIRelationsProvider(mockCtrl)

	var con *sql.DB
	fakeConnector.EXPECT().Connect(gomock.Any()).Return(con, nil)
	fakeServiceProvider.EXPECT().Fetch(con).Return([]*models.Service{
		{
			ID:   1,
			Code: "first-service",
		},
	}, nil)
	fakeFlagProvider.EXPECT().Fetch(con).Return([]*models.FeatureFlag{
		{
			ID:    10,
			Code:  "first-flag",
			State: models.Enable,
		},
	}, nil)
	fakeRelationsProvider.EXPECT().Fetch(con).Return([]*models.ServiceFeatureFlagRelation{
		{
			ServiceID:     1,
			FeatureFlagID: 10,
		},
		{
			ServiceID:     10,
			FeatureFlagID: 1,
		},
	}, nil)

	provider := indexbuilder.New(
		fakeConnector,
		fakeServiceProvider,
		fakeFlagProvider,
		fakeRelationsProvider,
		&configprovider.Config{
			Mysql: &configprovider.MySQLConfig{},
		},
	)

	index, err := provider.FetchAndBuild()

	if err != nil {
		t.Errorf("Can not build config %v", err)
	}

	if index.ServiceByID[1].Code != "first-service" {
		t.Errorf("Not correct index")
	}
	if _, ok := index.ServiceByID[10]; ok {
		t.Errorf("Not correct index")
	}

	if index.ServiceByCode["first-service"].ID != 1 {
		t.Errorf("Not correct index")
	}
	if _, ok := index.ServiceByCode["first-flag"]; ok {
		t.Errorf("Not correct index")
	}

	if index.FlagByID[10].Code != "first-flag" {
		t.Errorf("Not correct index")
	}
	if _, ok := index.FlagByID[1]; ok {
		t.Errorf("Not correct index")
	}

	if index.FlagByCode["first-flag"].ID != 10 {
		t.Errorf("Not correct index")
	}
	if _, ok := index.FlagByCode["first-service"]; ok {
		t.Errorf("Not correct index")
	}

	if !index.FlagIdsByServiceID[1][10] {
		t.Errorf("Not correct index")
	}

	if _, ok := index.FlagIdsByServiceID[10]; ok {
		t.Errorf("Not correct index")
	}
}
