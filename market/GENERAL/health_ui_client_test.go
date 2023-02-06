package healthuiclient

import (
	"github.com/stretchr/testify/require"
	"testing"
)

func TestConvertConfigVersionMapToStructBasic(t *testing.T) {
	configVersionMap := map[string]interface{}{
		"dataSource": map[string]interface{}{
			"logBroker": map[string]interface{}{
				"topics":          []string{"market-health-testing--other"},
				"hostGlob":        "*",
				"pathGlob":        "**-key-value.log",
				"ignorePathsGlob": []string{"**/mstat-antifraud-orders-key-value.log"},
			},
		},
	}
	expectedConfigVersionStruct := LogshatterConfigVersionEntity{
		DataSource: DataSourcesEntity{
			LogBroker: &DataSourceLogBrokerEntity{
				Topics:          []string{"market-health-testing--other"},
				HostGlob:        "*",
				PathGlob:        "**-key-value.log",
				IgnorePathsGlob: []string{"**/mstat-antifraud-orders-key-value.log"},
			},
		},
	}
	healthUIClient, _ := NewHealthUIClient(Testing, "")
	actualConfigVersionStruct, err := healthUIClient.ConvertConfigVersionMapToStruct(configVersionMap)
	require.NoError(t, err)
	require.Equal(t, expectedConfigVersionStruct, *actualConfigVersionStruct)
}

func TestConvertConfigVersionMapToStructWithoutLogbroker(t *testing.T) {
	configVersionMap := map[string]interface{}{
		"dataSource": map[string]interface{}{
			"tracker": []interface{}{
				map[string]interface{}{
					"queue": "MARKETINFRA",
				},
				map[string]interface{}{
					"queue": "MARKETINFRATEST",
				},
			},
		},
	}
	expectedConfigVersionStruct := LogshatterConfigVersionEntity{
		DataSource: DataSourcesEntity{
			LogBroker: nil,
		},
	}
	healthUIClient, _ := NewHealthUIClient(Testing, "")
	actualConfigVersionStruct, err := healthUIClient.ConvertConfigVersionMapToStruct(configVersionMap)
	require.NoError(t, err)
	require.Equal(t, expectedConfigVersionStruct, *actualConfigVersionStruct)
}

func TestConvertConfigVersionMapToStructWithoutTopicsTestEnv(t *testing.T) {
	configVersionMap := map[string]interface{}{
		"dataSource": map[string]interface{}{
			"logBroker": map[string]interface{}{
				"hostGlob":        "*",
				"pathGlob":        "**-key-value.log",
				"ignorePathsGlob": []string{"**/mstat-antifraud-orders-key-value.log"},
			},
		},
	}
	expectedConfigVersionStruct := LogshatterConfigVersionEntity{
		DataSource: DataSourcesEntity{
			LogBroker: &DataSourceLogBrokerEntity{
				Topics: []string{
					"logbroker://market-health-stable--other",
					"logbroker://market-health-prestable--other",
					"logbroker://market-health-testing--other",
				},
				HostGlob:        "*",
				PathGlob:        "**-key-value.log",
				IgnorePathsGlob: []string{"**/mstat-antifraud-orders-key-value.log"},
			},
		},
	}
	healthUIClient, _ := NewHealthUIClient(Testing, "")
	actualConfigVersionStruct, err := healthUIClient.ConvertConfigVersionMapToStruct(configVersionMap)
	require.NoError(t, err)
	require.Equal(t, expectedConfigVersionStruct, *actualConfigVersionStruct)
}

func TestConvertConfigVersionMapToStructWithoutTopicsProdEnv(t *testing.T) {
	configVersionMap := map[string]interface{}{
		"dataSource": map[string]interface{}{
			"logBroker": map[string]interface{}{
				"hostGlob":        "*",
				"pathGlob":        "**-key-value.log",
				"ignorePathsGlob": []string{"**/mstat-antifraud-orders-key-value.log"},
			},
		},
	}
	expectedConfigVersionStruct := LogshatterConfigVersionEntity{
		DataSource: DataSourcesEntity{
			LogBroker: &DataSourceLogBrokerEntity{
				Topics: []string{
					"logbroker://market-health-stable--other",
					"logbroker://market-health-prestable--other",
				},
				HostGlob:        "*",
				PathGlob:        "**-key-value.log",
				IgnorePathsGlob: []string{"**/mstat-antifraud-orders-key-value.log"},
			},
		},
	}
	healthUIClient, _ := NewHealthUIClient(Production, "")
	actualConfigVersionStruct, err := healthUIClient.ConvertConfigVersionMapToStruct(configVersionMap)
	require.NoError(t, err)
	require.Equal(t, expectedConfigVersionStruct, *actualConfigVersionStruct)
}

func TestConvertConfigVersionMapToStructWithoutIgnorePathsGlob(t *testing.T) {
	configVersionMap := map[string]interface{}{
		"dataSource": map[string]interface{}{
			"logBroker": map[string]interface{}{
				"topics":   []string{"market-health-testing--other"},
				"hostGlob": "*",
				"pathGlob": "**-key-value.log",
			},
		},
	}
	expectedConfigVersionStruct := LogshatterConfigVersionEntity{
		DataSource: DataSourcesEntity{
			LogBroker: &DataSourceLogBrokerEntity{
				Topics:          []string{"market-health-testing--other"},
				HostGlob:        "*",
				PathGlob:        "**-key-value.log",
				IgnorePathsGlob: nil,
			},
		},
	}
	healthUIClient, _ := NewHealthUIClient(Testing, "")
	actualConfigVersionStruct, err := healthUIClient.ConvertConfigVersionMapToStruct(configVersionMap)
	require.NoError(t, err)
	require.Equal(t, expectedConfigVersionStruct, *actualConfigVersionStruct)
}
