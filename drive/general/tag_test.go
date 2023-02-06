package models

import (
	"testing"

	"a.yandex-team.ru/drive/analytics/goback/models/tags"
)

func TestCarServiceTag(t *testing.T) {
	tag := Tag{
		Tag:      "lock_to_tech_support",
		Data:     "BAAAAAgBEAEKHTI0LjA1INC00LXQvNC+0L3RgtCw0LYgNDfQutC8",
		Snapshot: "EQAAAAoPZGV2aWNlX3NuYXBzaG90Cghtc2NfYXJlYRILCKMRGIbyrfYFIAASDgi3EBjI8632BU0AyGFGEgsIuxAYj5Wr9gUgXRIMCLwQGPSYq/YFIPQDGioJAAAAAFbTS0ARAAAAoKm4QkAdAAAAACUAAHpDKAEw1POt9gU42dyt9gU=",
	}
	data, err := tag.ParseData("car_service_tag")
	if err != nil {
		t.Fatal("Error:", err)
	}
	if _, ok := data.(*tags.SimpleCarTagData); !ok {
		t.Fatal("Data should be of type *SimpleCarTagData")
	}
	snapshot, err := tag.ParseSnapshot()
	if err != nil {
		t.Fatal("Error:", err)
	}
	if _, ok := snapshot.(*tags.DeviceSnapshot); !ok {
		t.Fatal("Snapshot should be of type *DeviceSnapshot")
	}
}

func TestTelematicsTag(t *testing.T) {
	tag := Tag{
		Tag:      "telematics",
		Data:     "AgAAAAgBeyJjYl90aW1lb3V0IjowLCJoYW5kbGVyIjoiODY3OTYyMDQyNzU1MzI1LTE1OTAzOTMzNjc0ODEyMTctNDM4MzAtdmxhMS0yNTUxLXZsYS1ydC1BUEkyIiwidGltZW91dCI6MCwiY29udGV4dCI6bnVsbCwiY29tbWFuZCI6IkNMT1NFX0RPT1JTIn0=",
		Snapshot: "EQAAAAoPZGV2aWNlX3NuYXBzaG90Cg5hbGxvd19kcm9wX2NhcgoIc3BiX2FyZWESCwijERjr8a32BSABEg4ItxAY4/Ot9gVNAMwhRhILCLsQGIv0rfYFIDUaKgkAAAAgp+1NQBEAAACgJDw+QB0AAAAAJQAAPEIoATCL9K32BTjl8q32BQ==",
	}
	data, err := tag.ParseData("telematics")
	if err != nil {
		t.Fatal("Error:", err)
	}
	if _, ok := data.(*tags.TelematicsTagData); !ok {
		t.Fatal("Data should be of type *TelematicsTagData")
	}
	snapshot, err := tag.ParseSnapshot()
	if err != nil {
		t.Fatal("Error:", err)
	}
	if _, ok := snapshot.(*tags.DeviceSnapshot); !ok {
		t.Fatal("Snapshot should be of type *DeviceSnapshot")
	}
}
