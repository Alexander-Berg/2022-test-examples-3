package serverconfig

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"

	srvCfg "a.yandex-team.ru/travel/app/backend/api/serverconfig/v1"
	"a.yandex-team.ru/travel/app/backend/internal/common"
)

func TestUnmarshalRaw(t *testing.T) {
	data := `
{
    "primary": {
        "config": {
            "common": {
                "foo": "FOO",
                "bar": 123,
                "testList": [
                    1,
                    2,
                    3
                ],
				"testMap": {
					"foo": 1
				}
            },
            "android": {
                "android": "specific"
            },
            "ios": {
                "ios": "specific"
            }
        }
    },
    "debug": {
        "config": {
            "ios": {
                "ios": "override"
            }
        }
    }
}
`
	var config compositeRawServerConfig
	err := json.Unmarshal([]byte(data), &config)
	assert.NoError(t, err)
	expected := compositeRawServerConfig{
		Primary: taggedRawServerConfig{
			Config: rawServerConfig{
				Common: map[string]interface{}{
					"foo":      "FOO",
					"bar":      123.0,
					"testList": []interface{}{1.0, 2.0, 3.0},
					"testMap":  map[string]interface{}{"foo": float64(1)},
				},
				IOS: map[string]interface{}{
					"ios": "specific",
				},
				Android: map[string]interface{}{
					"android": "specific",
				},
			},
		},
		Debug: taggedRawServerConfig{
			Config: rawServerConfig{
				IOS: map[string]interface{}{
					"ios": "override",
				},
			},
		},
	}
	assert.Equal(t, expected, config)
}

func TestBuildMapPrimaryCommonOnly(t *testing.T) {
	raw := compositeRawServerConfig{
		Primary: taggedRawServerConfig{
			Config: rawServerConfig{
				Common: map[string]interface{}{
					"foo": "bar",
				},
				IOS:     nil,
				Android: nil,
			},
		},
		Debug: taggedRawServerConfig{},
	}
	primIOS, primAndroid, debugIOS, debugAndroid, err := buildMaps(raw, true, common.ProductionEnv)
	assert.NoError(t, err)
	assert.NotNil(t, primIOS)
	assert.NotNil(t, primAndroid)
	assert.NotNil(t, debugIOS)
	assert.NotNil(t, debugAndroid)
	assert.Equal(t, map[string]interface{}{"foo": "bar"}, primIOS)
	assert.Equal(t, map[string]interface{}{"foo": "bar"}, primAndroid)
	assert.Equal(t, map[string]interface{}{"foo": "bar"}, debugIOS)
	assert.Equal(t, map[string]interface{}{"foo": "bar"}, debugAndroid)
}

func TestBuildMapNoCommonPrimOnly(t *testing.T) {
	raw := compositeRawServerConfig{
		Primary: taggedRawServerConfig{
			Config: rawServerConfig{
				Common:  nil,
				IOS:     map[string]interface{}{"foo": "bar"},
				Android: map[string]interface{}{"foo": "baz"},
			},
		},
		Debug: taggedRawServerConfig{},
	}
	primIOS, primAndroid, debugIOS, debugAndroid, err := buildMaps(raw, true, common.ProductionEnv)
	assert.NoError(t, err)
	assert.NotNil(t, primIOS)
	assert.NotNil(t, primAndroid)
	assert.NotNil(t, debugIOS)
	assert.NotNil(t, debugAndroid)
	assert.Equal(t, map[string]interface{}{"foo": "bar"}, primIOS)
	assert.Equal(t, map[string]interface{}{"foo": "baz"}, primAndroid)
	assert.Equal(t, map[string]interface{}{"foo": "bar"}, debugIOS)
	assert.Equal(t, map[string]interface{}{"foo": "baz"}, debugAndroid)
}

func TestBuildMapMergeCommonPrimOnly(t *testing.T) {
	raw := compositeRawServerConfig{
		Primary: taggedRawServerConfig{
			Config: rawServerConfig{
				Common:  map[string]interface{}{"base": "base", "toErase": "filled"},
				IOS:     map[string]interface{}{"foo": "bar", "toErase": nil},
				Android: map[string]interface{}{"foo": "baz", "base": "qux"},
			},
		},
		Debug: taggedRawServerConfig{},
	}
	primIOS, primAndroid, debugIOS, debugAndroid, err := buildMaps(raw, true, common.ProductionEnv)
	assert.NoError(t, err)
	assert.NotNil(t, primIOS)
	assert.NotNil(t, primAndroid)
	assert.NotNil(t, debugIOS)
	assert.NotNil(t, debugAndroid)
	assert.Equal(t, map[string]interface{}{"foo": "bar", "base": "base", "toErase": nil}, primIOS)
	assert.Equal(t, map[string]interface{}{"foo": "baz", "base": "qux", "toErase": "filled"}, primAndroid)
	assert.Equal(t, map[string]interface{}{"foo": "bar", "base": "base", "toErase": nil}, debugIOS)
	assert.Equal(t, map[string]interface{}{"foo": "baz", "base": "qux", "toErase": "filled"}, debugAndroid)
}

func TestBuildMapMergePrimAndDebug(t *testing.T) {
	raw := compositeRawServerConfig{
		Primary: taggedRawServerConfig{
			Config: rawServerConfig{
				Common:  map[string]interface{}{"base": "base", "toErase": "filled"},
				IOS:     map[string]interface{}{"foo": "bar", "toErase": nil},
				Android: map[string]interface{}{"foo": "baz", "base": "qux"},
			},
		},
		Debug: taggedRawServerConfig{
			Config: rawServerConfig{
				Common: map[string]interface{}{
					"base": "base-debug",
				},
				IOS: map[string]interface{}{
					"foo": "ios-debug",
				},
				Android: map[string]interface{}{
					"foo": "android-debug",
				},
			},
		},
	}
	primIOS, primAndroid, debugIOS, debugAndroid, err := buildMaps(raw, true, common.ProductionEnv)
	assert.NoError(t, err)
	assert.NotNil(t, primIOS)
	assert.NotNil(t, primAndroid)
	assert.NotNil(t, debugIOS)
	assert.NotNil(t, debugAndroid)
	assert.Equal(t, map[string]interface{}{"foo": "ios-debug", "base": "base-debug", "toErase": nil}, debugIOS)
	assert.Equal(t, map[string]interface{}{"foo": "android-debug", "base": "base-debug", "toErase": "filled"}, debugAndroid)
}

func TestNoPrimaryError(t *testing.T) {
	raw := compositeRawServerConfig{}
	_, _, _, _, err := buildMaps(raw, true, common.ProductionEnv)
	assert.Error(t, err)
}

func TestParse(t *testing.T) {
	mergedMap := map[string]interface{}{
		"user_profile": map[string]interface{}{
			"elements": []string{"USER_PROFILE_ELEMENT_PLUS",
				"USER_PROFILE_ELEMENT_NOTIFICATIONS",
				"USER_PROFILE_ELEMENT_PASSENGERS",
				"USER_PROFILE_ELEMENT_SUPPORT_CHAT",
				"USER_PROFILE_ELEMENT_PROMOCODES",
				"USER_PROFILE_ELEMENT_PAYMENT_METHODS",
				"USER_PROFILE_ELEMENT_DARK_THEME",
				"USER_PROFILE_ELEMENT_EXIT"},
		},
	}
	res, err := parseConfigs(mergedMap, mergedMap, mergedMap, mergedMap, true)
	assert.NoError(t, err)
	expectedConfig := &srvCfg.ServerConfig{UserProfile: &srvCfg.UserProfileConfig{Elements: []srvCfg.UserProfileElement{
		srvCfg.UserProfileElement_USER_PROFILE_ELEMENT_PLUS,
		srvCfg.UserProfileElement_USER_PROFILE_ELEMENT_NOTIFICATIONS,
		srvCfg.UserProfileElement_USER_PROFILE_ELEMENT_PASSENGERS,
		srvCfg.UserProfileElement_USER_PROFILE_ELEMENT_SUPPORT_CHAT,
		srvCfg.UserProfileElement_USER_PROFILE_ELEMENT_PROMOCODES,
		srvCfg.UserProfileElement_USER_PROFILE_ELEMENT_PAYMENT_METHODS,
		srvCfg.UserProfileElement_USER_PROFILE_ELEMENT_DARK_THEME,
		srvCfg.UserProfileElement_USER_PROFILE_ELEMENT_EXIT,
	}}}
	assert.Equal(t, expectedConfig.UserProfile.Elements, res.Primary.Ios.UserProfile.Elements)
}
