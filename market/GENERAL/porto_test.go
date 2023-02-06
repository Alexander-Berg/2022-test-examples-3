package porto

import (
	"bytes"
	"crypto/rand"
	"os"
	"os/exec"
	"strings"
	"syscall"
	"testing"

	"a.yandex-team.ru/market/sre/library/golang/porto/rpc"
)

func FailOnError(t *testing.T, conn API, err error) {
	if err == nil {
		if conn != nil {
			if conn.GetLastError() != 0 {
				t.Error("GetLastError() return value isn't zero")
				t.FailNow()
			}
			if conn.GetLastErrorMessage() != "" {
				t.Error("GetLastErrorMesage() return value isn't empty")
				t.FailNow()
			}
		}
	} else {
		if conn != nil && conn.GetLastError() != 0 {
			t.Errorf("error %s (%s)",
				rpc.EError_name[int32(conn.GetLastError())],
				conn.GetLastErrorMessage())
		} else {
			t.Error(err)
		}
	}
}

func ConnectToPorto(t *testing.T) API {
	conn, err := Connect()
	if conn == nil {
		t.Error(err)
		t.FailNow()
	}
	return conn
}

const testContainer string = "golang_testContainer"
const testVolume string = "/tmp/golang_testVolume"
const testLayer string = "golang_testLayer"
const testTarball = "/tmp/" + testLayer + ".tgz"
const testStorage string = "go_abcde"
const testPlace string = "/tmp/golang_place"

func testGetVersion(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	maj, min, err := conn.GetVersion()
	FailOnError(t, conn, err)
	t.Logf("Porto version %s.%s", maj, min)
}

func testPlist(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	plist, err := conn.Plist()
	FailOnError(t, conn, err)
	for i := range plist {
		if plist[i].Name == "command" {
			t.Logf("Porto supports command property: %s", plist[i].Description)
			return
		}
	}
	t.Error("Porto doesn't support command property")
	t.FailNow()
}

func testDlist(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	dlist, err := conn.Dlist()
	FailOnError(t, conn, err)
	for i := range dlist {
		if dlist[i].Name == "state" {
			t.Logf("Porto supports state data: %s", dlist[i].Description)
			return
		}
	}
	t.Error("Porto doesn't support state data")
	t.FailNow()
}

func testCreateWeak(t *testing.T) {
	conn := ConnectToPorto(t)
	FailOnError(t, conn, conn.CreateWeak(testContainer))
	_ = conn.Close()

	conn = ConnectToPorto(t)
	defer conn.Close()
	err := conn.Destroy(testContainer)
	if err == nil {
		t.Fail()
	}
}

func testCreate(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.Create(testContainer))
}

func testList(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	list, err := conn.List()
	FailOnError(t, conn, err)
	for i := range list {
		if list[i] == testContainer {
			return
		}
	}
	t.Error("Created container isn't presented in a list")
	t.FailNow()
}

func testSetProperty(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.SetProperty(testContainer, "command", "sleep 10000"))
}

func testGetProperty(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	value, err := conn.GetProperty(testContainer, "command")
	FailOnError(t, conn, err)
	if value != "sleep 10000" {
		t.Error("Got a wrong command value")
		t.FailNow()
	}
}

func testStart(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.Start(testContainer))
}

func testPause(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.Pause(testContainer))
}

func testResume(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.Resume(testContainer))
}

func testKill(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.Kill(testContainer, syscall.SIGTERM))
}

func testWait(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	containers := []string{testContainer}
	container, err := conn.Wait(containers, -1)
	FailOnError(t, conn, err)
	if container != testContainer {
		t.Error("Wait returned a wrong container")
		t.FailNow()
	}
}

func testGetData(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	value, err := conn.GetData(testContainer, "state")
	FailOnError(t, conn, err)
	if value != "dead" {
		t.Error("Got a wrong state value")
		t.FailNow()
	}
}

func testGet(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	containers := []string{testContainer}
	variables := []string{"state", "exit_status"}
	resp, err := conn.Get(containers, variables)
	FailOnError(t, conn, err)
	if resp[testContainer]["state"].Value != "dead" {
		t.Error("Got a wrong state value")
		t.FailNow()
	}
	if resp[testContainer]["exit_status"].Value != "15" {
		t.Error("Got a wrong exit_status value")
		t.FailNow()
	}
}

func testConvertPath(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	resp, err := conn.ConvertPath("/", testContainer, testContainer)
	FailOnError(t, conn, err)
	if resp != "/" {
		t.Error("Got wrong path conversion")
		t.FailNow()
	}
}

func testStop(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.Stop(testContainer))
}

func testDestroy(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.Destroy(testContainer))
}

// VolumeAPI
func testListVolumeProperties(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	properties, err := conn.ListVolumeProperties()
	FailOnError(t, conn, err)
	for i := range properties {
		if properties[i].Name == "backend" {
			return
		}
	}
	t.Error("Porto doesn't list backend volume property")
	t.FailNow()
}

func testCreateVolume(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	_ = os.Remove(testVolume)
	_ = os.Mkdir(testVolume, 0755)
	config := make(map[string]string)
	config["private"] = "golang test volume"
	_, err := conn.CreateVolume(testVolume, config)
	FailOnError(t, conn, err)
}

func testListVolumes(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	volumes, err := conn.ListVolumes("", "")
	FailOnError(t, conn, err)
	for i := range volumes {
		if volumes[i].Path == testVolume {
			return
		}
	}
	t.Error("Porto doesn't list previously created volume")
	t.FailNow()
}

func testLinkVolume(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.Create(testContainer))
	FailOnError(t, conn, conn.LinkVolume(testVolume, testContainer))
}

func testExportLayer(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.ExportLayer(testVolume, "/tmp/goporto.tgz"))
	_ = os.Remove("/tmp/goporto.tgz")
}

func testUnlinkVolume(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.UnlinkVolume(testVolume, testContainer))
	FailOnError(t, conn, conn.UnlinkVolume(testVolume, "/"))
	FailOnError(t, conn, conn.Destroy(testContainer))
	_ = os.Remove(testVolume)
}

// LayerAPI
func testImportLayer(t *testing.T) {
	// Prepare tarball
	_ = os.Mkdir("/tmp/golang_testTarball", 0755)
	defer os.Remove("/tmp/golang_testTarball")
	_ = os.Mkdir("/tmp/golang_testTarball/dir", 0755)
	defer func() {
		_ = os.Remove("/tmp/golang_testTarball/dir")
	}()

	cmd := exec.Command("tar", "-czf", testTarball,
		"/tmp/golang/testTarball")
	err := cmd.Start()
	if err != nil {
		t.Error("Can't prepare test tarball")
		t.SkipNow()
	}
	_ = cmd.Wait()
	defer os.Remove(testTarball)

	// Import
	conn := ConnectToPorto(t)
	defer conn.Close()

	FailOnError(t, conn,
		conn.ImportLayer(testLayer, testTarball, false))
}

func testLayerPrivate(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()

	FailOnError(t, conn, conn.SetLayerPrivate(testLayer, "", "456"))
	private, err := conn.GetLayerPrivate(testLayer, "")
	FailOnError(t, conn, err)

	if private == "456" {
		return
	}

	t.Error("Can't get/set new layer private")
	t.FailNow()
}

func testListLayers(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	layers, err := conn.ListLayers()
	FailOnError(t, conn, err)
	for i := range layers {
		if layers[i] == testLayer {
			return
		}
	}
	t.Error("Porto doesn't list previously imported layer")
	t.FailNow()
}

func testListLayers2(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	layers, err := conn.ListLayers2("", "")
	FailOnError(t, conn, err)
	for i := range layers {
		if layers[i].Name == testLayer &&
			layers[i].PrivateValue == "456" &&
			layers[i].LastUsage < 10 {

			return
		}
	}
	t.Error("Porto doesn't list previously imported layer as object")
	t.FailNow()
}

func testRemoveLayer(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.RemoveLayer(testLayer))
}

func TestSendRecvData(t *testing.T) {
	buff := new(bytes.Buffer)
	data := make([]byte, 1024*1024+1024)
	_, err := rand.Read(data)
	if err != nil {
		t.Fatalf("unable to generate random array: %v", err)
	}

	if err := sendData(buff, data); err != nil {
		t.Fatalf("SendData returns unexpected error: %v", err)
	}

	result, err := recvData(buff)
	if err != nil {
		t.Fatalf("RecvData returns unexpected error: %v", err)
	}

	if !bytes.Equal(data, result) {
		t.Fatalf("result is not the same as input")
	}
}

func testCreateStorage(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()

	config := make(map[string]string)
	config["backend"] = "native"
	config["storage"] = testStorage
	config["private"] = "12345"

	volume, err := conn.CreateVolume("", config)
	FailOnError(t, conn, err)
	FailOnError(t, conn, conn.UnlinkVolume3(volume.Path, "", false))
}

func testListStorage(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()

	storages, err := conn.ListStorage("", "")
	FailOnError(t, conn, err)

	for i := range storages {
		if storages[i].Name == testStorage &&
			storages[i].PrivateValue == "12345" {

			return
		}
	}

	t.Error("Failed listing storages")
	t.FailNow()
}

func testRemoveStorage(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()
	FailOnError(t, conn, conn.RemoveStorage(testStorage, ""))
}

func testPlace_(t *testing.T) {
	conn := ConnectToPorto(t)
	defer conn.Close()

	_ = os.RemoveAll(testPlace)
	_ = os.Mkdir(testPlace, 0755)
	_ = os.Mkdir(testPlace+"/porto_volumes", 0755)
	_ = os.Mkdir(testPlace+"/porto_layers", 0755)
	_ = os.Mkdir(testPlace+"/porto_storage", 0755)

	config := make(map[string]string)
	config["private"] = "golang test volume"
	config["place"] = testPlace
	config["storage"] = "abcd"

	volume, err := conn.CreateVolume("", config)
	FailOnError(t, conn, err)

	if !strings.Contains(volume.Path, testPlace) {
		t.Error("Volume does not use desired place")
		t.FailNow()
	}

	_, err = os.Stat(volume.Path)
	FailOnError(t, conn, err)

	storages, err := conn.ListStorage(testPlace, "")
	FailOnError(t, conn, err)

	if len(storages) == 0 || storages[0].Name != "abcd" {
		t.Error("Storage failed to be created in place")
		t.FailNow()
	}

	FailOnError(t, conn, conn.UnlinkVolume3(volume.Path, "", false))
	FailOnError(t, conn, conn.RemoveStorage("abcd", testPlace))
	FailOnError(t, conn, os.Remove(testPlace+"/porto_volumes"))
	FailOnError(t, conn, os.Remove(testPlace+"/porto_layers"))
	FailOnError(t, conn, os.Remove(testPlace+"/porto_storage"))
	FailOnError(t, conn, os.Remove(testPlace))
}
