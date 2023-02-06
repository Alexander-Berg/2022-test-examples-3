package test

import (
	"testing"

	"github.com/golang/mock/gomock"

	"a.yandex-team.ru/travel/avia/feature_flag_api/internal/configprovider"
	"a.yandex-team.ru/travel/avia/feature_flag_api/internal/environmentvariableprovider/mock"
)

func compareConfig(actual *configprovider.Config, expected *configprovider.Config) (string, bool) {
	if actual.Environment != expected.Environment {
		return "Environment", false
	}

	mActual := actual.Mysql
	mExpected := expected.Mysql

	//ReadHosts  []string

	if len(mActual.ReadHosts) != len(mExpected.ReadHosts) {
		return "ReadHosts", false
	}
	for i := range mActual.ReadHosts {
		if mActual.ReadHosts[i] != mExpected.ReadHosts[i] {
			return "ReadHosts", false
		}
	}

	if mActual.UserName != mExpected.UserName {
		return "UserName", false
	}
	if mActual.Password != mExpected.Password {
		return "Password", false
	}
	if mActual.Protocol != mExpected.Protocol {
		return "Protocol", false
	}
	if mActual.Port != mExpected.Port {
		return "Port", false
	}
	if mActual.Schema != mExpected.Schema {
		return "Schema", false
	}

	return "", true
}

func TestConfigProvider_Normal(t *testing.T) {

	mockCtrl := gomock.NewController(t)
	fakeEnvironmentProvider := mock_environmentVariableProvider.NewMockIEnvironmentVariableProvider(mockCtrl)

	fakeEnvironmentProvider.EXPECT().
		ReadRequiredEnv("ENVIRONMENT").
		Return("SomeEnvironment", nil)
	fakeEnvironmentProvider.EXPECT().
		ReadRequiredEnv("DB_HOST").
		Return("SomeHost", nil)
	fakeEnvironmentProvider.EXPECT().
		ReadRequiredEnv("DB_PORT").
		Return("11211", nil)
	fakeEnvironmentProvider.EXPECT().
		ReadRequiredEnv("DB_USER_NAME").
		Return("SomeUserName", nil)
	fakeEnvironmentProvider.EXPECT().
		ReadRequiredEnv("DB_PROTOCOL").
		Return("SomeProtocol", nil)
	fakeEnvironmentProvider.EXPECT().
		ReadRequiredEnv("DB_PASSWORD").
		Return("SomePassword", nil)
	fakeEnvironmentProvider.EXPECT().
		ReadRequiredEnv("DB_SCHEMA").
		Return("SomeSchema", nil)
	fakeEnvironmentProvider.EXPECT().
		ReadEnv("LISTEN_PORT").
		Return("SomeListenPort")
	fakeEnvironmentProvider.EXPECT().
		ReadEnv("INDEX_WORKER_SUCCESS_SLEEP_SECONDS").
		Return("60")
	fakeEnvironmentProvider.EXPECT().
		ReadEnv("INDEX_WORKER_FAIL_SLEEP_SECONDS").
		Return("300")

	provider := configprovider.New(
		fakeEnvironmentProvider,
	)

	actual, err := provider.GetConfig()

	if err != nil {
		t.Errorf("Can not build config %v", err)
	}

	differentField, isEqual := compareConfig(actual, &configprovider.Config{
		Environment: "SomeEnvironment",
		ListenPort:  "SomeListenPort",
		Mysql: &configprovider.MySQLConfig{
			ReadHosts: []string{"SomeHost"},
			UserName:  "SomeUserName",
			Password:  "SomePassword",
			Protocol:  "SomeProtocol",
			Port:      11211,
			Schema:    "SomeSchema",
		},
	})

	if !isEqual {
		t.Errorf("Some field is different [%v]", differentField)
	}
}
