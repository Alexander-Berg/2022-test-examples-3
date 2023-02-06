package common

import "testing"

func TestIOS(t *testing.T) {
	actual := ParseUserAgent("ru.yandex.travel.app/1.0 (Apple iPhone12; iOS 14.7.1)")
	expected := UserAgentData{
		RawUA:              "ru.yandex.travel.app/1.0 (Apple iPhone12; iOS 14.7.1)",
		IsStandardFormat:   true,
		KnownOS:            OSTypeIOS,
		AppName:            "ru.yandex.travel.app",
		AppVersion:         "1.0",
		AppVersionCode:     0,
		DeviceManufacturer: "Apple",
		DeviceName:         "iPhone12",
		OSName:             "iOS",
		OSVersion:          "14.7.1",
	}
	if actual != expected {
		t.Errorf("Unexpected user-agent: %v != %v", actual, expected)
	}
}

func TestAndroid(t *testing.T) {
	actual := ParseUserAgent("ru.yandex.sample/4.56.1234 (Motorola MB860; Android 4.0.1)")
	expected := UserAgentData{
		RawUA:              "ru.yandex.sample/4.56.1234 (Motorola MB860; Android 4.0.1)",
		IsStandardFormat:   true,
		KnownOS:            OSTypeAndroid,
		AppName:            "ru.yandex.sample",
		AppVersion:         "4.56.1234",
		AppVersionCode:     0,
		DeviceManufacturer: "Motorola",
		DeviceName:         "MB860",
		OSName:             "Android",
		OSVersion:          "4.0.1",
	}
	if actual != expected {
		t.Errorf("Unexpected user-agent: %v != %v", actual, expected)
	}
}

func TestAndroidWithVersionNumber(t *testing.T) {
	actual := ParseUserAgent("ru.yandex.sample/4.56.1234(121) (Motorola MB860; Android 4.0.1)")
	expected := UserAgentData{
		RawUA:              "ru.yandex.sample/4.56.1234(121) (Motorola MB860; Android 4.0.1)",
		IsStandardFormat:   true,
		KnownOS:            OSTypeAndroid,
		AppName:            "ru.yandex.sample",
		AppVersion:         "4.56.1234",
		AppVersionCode:     uint64(121),
		DeviceManufacturer: "Motorola",
		DeviceName:         "MB860",
		OSName:             "Android",
		OSVersion:          "4.0.1",
	}
	if actual != expected {
		t.Errorf("Unexpected user-agent: %v != %v", actual, expected)
	}
}

func TestOtherFormat(t *testing.T) {
	actual := ParseUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 YaBrowser/21.8.0.1716 Yowser/2.5 Safari/537.36")
	expected := UserAgentData{
		RawUA:            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 YaBrowser/21.8.0.1716 Yowser/2.5 Safari/537.36",
		IsStandardFormat: false,
		KnownOS:          OSTypeUnknown,
	}
	if actual != expected {
		t.Errorf("Unexpected user-agent: %v != %v", actual, expected)
	}
}
