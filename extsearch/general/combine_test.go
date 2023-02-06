package tc_test

import (
	"errors"
	"fmt"
	"io/ioutil"
	"os"
	"testing"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/comb"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/config"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/db"
	pb "a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/proto"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/queue"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/testutil"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/tstor"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/util"
	opb "a.yandex-team.ru/extsearch/video/robot/rt_transcoder/proto"
	"a.yandex-team.ru/library/go/core/log"
)

const (
	// sandbox resource binaries come readonly, not executable. we copy them and make executable.
	ffmpegBin  = "./ffmpeg"
	ffprobeBin = "./ffprobe"
	mp42hlsBin = "./mp42hls_bin"
)

type mockDao struct {
	mock.Mock
	db.Dao
}

func (d *mockDao) FindOrigTask(taskID string) (*opb.TTaskInfo, error) {
	args := d.Called(taskID)
	return args.Get(0).(*opb.TTaskInfo), nil
}

func (d *mockDao) FindTask(taskID string) (*pb.TTranscodeTask, error) {
	args := d.Called(taskID)
	return args.Get(0).(*pb.TTranscodeTask), nil
}

func (d *mockDao) UpdateOrigTaskOutput(taskID, user string, outputURL *string, outputs []*opb.TOutputInfo) error {
	d.Called(taskID, user, outputURL, outputs)
	return nil
}

func (d *mockDao) UpdateTaskContentStatus(taskID string, status opb.EContentStatus) error {
	d.Called(taskID, status)
	return nil
}

func (d *mockDao) UpdateTaskPreviewStatus(taskID string, status opb.EPreviewStatus) error {
	d.Called(taskID, status)
	return nil
}

func (d *mockDao) UpdateTaskSignaturesStatus(taskID string, status opb.ESignaturesStatus) error {
	d.Called(taskID, status)
	return nil
}

func (d *mockDao) UpdateOrigTaskError(taskID, user string, errorCode opb.EErrorCode, errorDesc string) error {
	d.Called(taskID, user, errorCode, errorDesc)
	return nil
}

func (d *mockDao) UpdateOrigTaskStatus(taskID string, status opb.ETaskStatus) error {
	d.Called(taskID, status)
	return nil
}

func (d *mockDao) UpdateOrigTaskUrls(taskID string, outputURL *string, outputs []*opb.TOutputInfo) error {
	d.Called(taskID, outputURL, outputs)
	return nil
}

func (d *mockDao) UpdateTaskLowResStatus(taskID string, status opb.ELowResStatus) error {
	d.Called(taskID, status)
	return nil
}

func (d *mockDao) GetTaskLowResStatus(taskID string) (opb.ELowResStatus, error) {
	args := d.Called(taskID)
	return args.Get(0).(opb.ELowResStatus), nil
}

func (d *mockDao) UpdateTaskHighResStatus(taskID string, status opb.EHighResStatus) error {
	d.Called(taskID, status)
	return nil
}

func (d *mockDao) UpdateOrigTaskThumbs(taskID string, thumbs []string, tiles *opb.TTimelineTiles) error {
	d.Called(taskID, thumbs, tiles)
	return nil
}

func (d *mockDao) GetTaskHighResStatus(taskID string) (opb.EHighResStatus, error) {
	args := d.Called(taskID)
	return args.Get(0).(opb.EHighResStatus), nil
}

func (d *mockDao) GetTaskSegmentsWithTiles(taskID string, isLowRes bool) ([]db.SegmentTilesInfo, error) {
	d.Called(taskID, isLowRes)
	return []db.SegmentTilesInfo{}, nil
}

func (d *mockDao) SetOrigTaskLowResTranscoded(taskID string) error {
	d.Called(taskID)
	return nil
}

func (d *mockDao) UpdateSentToPlainQueue(taskID string, value bool) error {
	d.Called(taskID, value)
	return nil
}

func (d *mockDao) GetOrigTaskStatus(taskID string) (status opb.ETaskStatus, err error) {
	args := d.Called(taskID)
	if len(args) == 0 {
		return opb.ETaskStatus(4), nil
	} else {
		return args.Get(0).(opb.ETaskStatus), nil
	}
}

type mockTaskStorage struct {
	mock.Mock
	tstor.TaskStorage
}

func (d *mockTaskStorage) DownloadTranscodedSegment(taskID string, timestamp uint64, segmentNum, height uint32, to string) error {
	args := d.Called(taskID, timestamp, segmentNum, height, to)
	return util.CopyFile(args.String(0), to)
}

func (d *mockTaskStorage) DownloadAllTranscodedSegments(taskID string, timestamp uint64, segmentsNum uint32, height uint32, namer func(uint32) string) error {
	args := d.Called(taskID, timestamp, segmentsNum, height, namer)

	pattern := args.String(0)
	for i := uint32(1); i <= segmentsNum; i++ {
		from := fmt.Sprintf(pattern, i, height)
		to := namer(i)
		err := util.CopyFile(from, to)
		if err != nil {
			return err
		}
	}

	return nil
}

func (d *mockTaskStorage) DownloadAllTilesAndScreens(taskID string, timestamp uint64, tilesDir string, segmentsTiles []db.SegmentTilesInfo, allTilesNum uint32, needTiles bool) error {
	d.Called(taskID, timestamp, tilesDir, segmentsTiles, allTilesNum, needTiles)
	return nil
}

func (d *mockTaskStorage) RemoveTaskData(taskID string, timestamp uint64) error {
	d.Called(taskID, timestamp)
	return nil
}

type mockUploader struct {
	mock.Mock
	comb.Uploader
}

func (u *mockUploader) S3Upload(bucket, key, secret string, taskCh chan []string, concurrency int) error {
	args := u.Called(bucket, key, secret, taskCh, concurrency)

	expected := args.Int(0)
	var seen int
	var check func(string) error
	if args.Get(1) != nil {
		check = args.Get(1).(func(string) error)
	}
	for t := range taskCh {
		seen++
		if check != nil {
			err := check(t[0])
			if err != nil {
				panic(err)
			}
		}
	}

	if seen != expected {
		panic(fmt.Errorf("%d files expected, %d seen", expected, seen))
	}
	return nil
}

func (u *mockUploader) AvatarsUpload(taskCh chan []string, total int, concurrency int, namespace, avatarsGet, avatarsPut string, lg log.Logger) ([]string, error) {
	u.Called(taskCh, total, concurrency, namespace, lg)

	if len(taskCh) != total {
		panic(fmt.Errorf("%d files in chan, %d total", len(taskCh), total))
	}

	ans := make([]string, 0, total)
	for file := range taskCh {
		ans = append(ans, file[0])
	}

	return ans, nil
}

type mockOutput struct {
	mock.Mock
	queue.Output
}

func (o *mockOutput) Push(msg string) error {
	o.Called(msg)
	return nil
}

func (o *mockOutput) PushFifo(id, msg string) error {
	o.Called(msg)
	return nil
}

type mockOutputBrokenQueue struct {
	mock.Mock
	queue.Output
}

func (o *mockOutputBrokenQueue) Push(msg string) error {
	o.Called(msg)
	return errors.New("error pushing to queue")
}

func (o *mockOutputBrokenQueue) PushFifo(id, msg string) error {
	o.Called(msg)
	return errors.New("error pushing to queue")
}

type Metrics struct {
}

func (m *Metrics) MessageProceed(combTime time.Duration, err error)                        {}
func (m *Metrics) WebhookStatus(user, wh string, d time.Duration, lowres bool, status int) {}
func (m *Metrics) SentToPlain(user string)                                                 {}
func (m *Metrics) ConcatFail(user, desc string)                                            {}

func mustReadTask(file string) (task *pb.TTranscodeTask) {
	taskStr, err := ioutil.ReadFile(file)
	if err != nil {
		panic(err)
	}

	task = &pb.TTranscodeTask{}
	err = proto.UnmarshalText(string(taskStr), task)
	if err != nil {
		err = proto.Unmarshal(taskStr, task)
	}

	if err != nil {
		panic(err)
	}

	return
}

func TestMain(m *testing.M) {
	testutil.MustCopyBin("./pack/ffmpeg", ffmpegBin)
	testutil.MustCopyBin("./pack/ffprobe", ffprobeBin)
	testutil.MustCopyBin("./mp42hls", mp42hlsBin)

	os.Exit(m.Run())
}

var met = &Metrics{}

// deleted resource by ttl
//func TestHlsOnlyVideo(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := ioutil.TempDir("", "comb-test-")
//	require.NoError(t, err)
//	defer func() { _ = os.RemoveAll(wd) }()
//
//	task := mustReadTask("task1.proto")
//
//	dao := &mockDao{}
//	dao.On("FindOrigTask", task.Id).Return(&opb.TTaskInfo{}).Once()
//	dao.On("FindTask", task.Id).Return(task).Once()
//	ourl := "https://strm.yandex.ru/transcoder-test/mt-test/td.m3u8"
//	dao.On("UpdateOrigTaskOutput", task.Id, "mt", &ourl, mock.MatchedBy(func(o []*opb.TOutputInfo) bool { return len(o) == 1 })).Return(task).Once()
//	dao.On("UpdateTaskContentStatus", task.Id, opb.EContentStatus_ECSNotNeeded).Once()
//	dao.On("UpdateTaskPreviewStatus", task.Id, opb.EPreviewStatus_EPSNotNeeded).Once()
//	dao.On("UpdateTaskSignaturesStatus", task.Id, opb.ESignaturesStatus_ESSNotNeeded).Once()
//	dao.On("GetTaskSegmentsWithTiles", task.Id, false).Once()
//	dao.On("UpdateOrigTaskThumbs", task.Id, mock.Anything, mock.Anything).Once()
//	dao.On("GetOrigTaskStatus", task.Id).Once()
//
//	ts := &mockTaskStorage{}
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(64000), mock.Anything).Return("audio-64000.mp4").Once()
//
//	for _, h := range []uint32{240, 360, 480} {
//		ts.On("DownloadAllTranscodedSegments", task.Id, mock.Anything, task.SegmentsCount-1, h, mock.Anything).Return("chunk-%d-%d.mp4").Once()
//	}
//
//	ts.On("DownloadAllTilesAndScreens", task.Id, mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Once()
//	ts.On("RemoveTaskData", task.Id, mock.Anything).Once()
//
//	up := &mockUploader{}
//
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(0, nil).Once()
//
//	// mpegts chunks uploading
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(20, nil).Times(3)
//
//	expMasterPlaylist := `#EXTM3U
//#EXT-X-VERSION:3
//#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=543696,RESOLUTION=426x240
//td_169_240p.m3u8
//#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=931728,RESOLUTION=640x360
//td_169_360p.m3u8
//#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1464896,RESOLUTION=854x480
//td_169_480p.m3u8
//`
//	// playlist uploading
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(1, func(f string) error {
//		raw, err := ioutil.ReadFile(f)
//		require.NoError(t, err)
//		require.Equal(t, expMasterPlaylist, string(raw))
//		return nil
//	}).Once()
//
//	keys := make(config.Keys)
//	keys["transcoder-test"] = config.BucketInfo{Strm: true}
//
//	metarobotQueue := &mockOutput{}
//	metarobotQueue.On("Push", mock.Anything).Once()
//
//	whBrokenQueue := &mockOutputBrokenQueue{}
//	whBrokenQueue.On("PushFifo", mock.Anything).Once()
//
//	ctx := comb.CombiningContext{
//		Lg:           lg,
//		FfmpegPath:   ffmpegBin,
//		FfprobePath:  ffprobeBin,
//		Mp42hlsPath:  mp42hlsBin,
//		TaskStorage:  ts,
//		Dao:          dao,
//		WorkDir:      wd,
//		S3keys:       keys,
//		Up:           up,
//		MrobotQ:      metarobotQueue,
//		WebhookQueue: whBrokenQueue,
//		Met:          met,
//	}
//
//	err = comb.Combine(ctx,
//		fmt.Sprintf(`Id: "%s"
//SpritesInfo: <
//HorizontalTilesNum: 5
//VerticalTilesNum: 5
//>`, task.Id))
//	require.NoError(t, err)
//
//	dao.AssertExpectations(t)
//	ts.AssertExpectations(t)
//	up.AssertExpectations(t)
//}
//
//func TestFfmpegHlsOnlyVideo(t *testing.T) {
//	lg := util.MakeLogger()
//	wd, err := ioutil.TempDir("", "comb-test-")
//	require.NoError(t, err)
//	defer func() { _ = os.RemoveAll(wd) }()
//	task := mustReadTask("task1.proto")
//	dao := &mockDao{}
//	dao.On("FindOrigTask", task.Id).Return(&opb.TTaskInfo{}).Once()
//	dao.On("FindTask", task.Id).Return(task).Once()
//	ourl := "https://strm.yandex.ru/transcoder-test/mt-test/td.m3u8"
//	dao.On("UpdateOrigTaskOutput", task.Id, "mt", &ourl, mock.MatchedBy(func(o []*opb.TOutputInfo) bool { return len(o) == 1 })).Return(task).Once()
//	dao.On("UpdateTaskContentStatus", task.Id, opb.EContentStatus_ECSNotNeeded).Once()
//	dao.On("UpdateTaskPreviewStatus", task.Id, opb.EPreviewStatus_EPSNotNeeded).Once()
//	dao.On("UpdateTaskSignaturesStatus", task.Id, opb.ESignaturesStatus_ESSNotNeeded).Once()
//	dao.On("GetTaskSegmentsWithTiles", task.Id, false).Once()
//	dao.On("UpdateOrigTaskThumbs", task.Id, mock.Anything, mock.Anything).Once()
//	dao.On("GetOrigTaskStatus", task.Id).Once()
//	ts := &mockTaskStorage{}
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(64000), mock.Anything).Return("audio-64000.mp4").Once()
//	for _, h := range []uint32{240, 360, 480} {
//		ts.On("DownloadAllTranscodedSegments", task.Id, mock.Anything, task.SegmentsCount-1, h, mock.Anything).Return("chunk-%d-%d.mp4").Once()
//	}
//	ts.On("DownloadAllTilesAndScreens", task.Id, mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Once()
//	ts.On("RemoveTaskData", task.Id, mock.Anything).Once()
//	up := &mockUploader{}
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(0, nil).Once()
//	// mpegts chunks uploading
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(20, nil).Times(3)
//	expMasterPlaylist := `#EXTM3U
//#EXT-X-VERSION:3
//#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=489176,RESOLUTION=426x240
//td_169_240p.m3u8
//#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=874952,RESOLUTION=640x360
//td_169_360p.m3u8
//#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1399478,RESOLUTION=854x480
//td_169_480p.m3u8
//`
//	// playlist uploading
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(1, func(f string) error {
//		raw, err := ioutil.ReadFile(f)
//		require.NoError(t, err)
//		require.Equal(t, expMasterPlaylist, string(raw))
//		return nil
//	}).Once()
//	keys := make(config.Keys)
//	keys["transcoder-test"] = config.BucketInfo{Strm: true}
//	metarobotQueue := &mockOutput{}
//	metarobotQueue.On("Push", mock.Anything).Once()
//	whBrokenQueue := &mockOutputBrokenQueue{}
//	whBrokenQueue.On("PushFifo", mock.Anything).Once()
//	ctx := comb.CombiningContext{
//		Lg:            lg,
//		FfmpegPath:    ffmpegBin,
//		FfprobePath:   ffprobeBin,
//		Mp42hlsPath:   mp42hlsBin,
//		TaskStorage:   ts,
//		Dao:           dao,
//		WorkDir:       wd,
//		S3keys:        keys,
//		Up:            up,
//		MrobotQ:       metarobotQueue,
//		WebhookQueue:  whBrokenQueue,
//		Met:           met,
//		MuxWithFfmpeg: true,
//	}
//	err = comb.Combine(ctx,
//		fmt.Sprintf(`Id: "%s"
//SpritesInfo: <
//HorizontalTilesNum: 5
//VerticalTilesNum: 5
//>`, task.Id))
//	require.NoError(t, err)
//	dao.AssertExpectations(t)
//	ts.AssertExpectations(t)
//	up.AssertExpectations(t)
//}

// deleted resource by ttl
//func TestHlsTiles(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := ioutil.TempDir("", "comb-test-")
//	require.NoError(t, err)
//	defer func() { _ = os.RemoveAll(wd) }()
//
//	task := mustReadTask("task_tiles.proto")
//
//	tilesStr, err := ioutil.ReadFile("tiles_info.json")
//	require.NoError(t, err)
//
//	dao := &mockDao{}
//	dao.On("FindOrigTask", task.Id).Return(&opb.TTaskInfo{TimelineTiles: string(tilesStr)})
//	dao.On("FindTask", task.Id).Return(task).Once()
//	dao.On("UpdateOrigTaskOutput", task.Id, "test", mock.Anything, mock.Anything).Return(task).Once()
//	dao.On("UpdateTaskContentStatus", task.Id, opb.EContentStatus_ECSNotNeeded).Once()
//	dao.On("UpdateTaskPreviewStatus", task.Id, opb.EPreviewStatus_EPSNotNeeded).Once()
//	dao.On("UpdateTaskSignaturesStatus", task.Id, opb.ESignaturesStatus_ESSNotNeeded).Once()
//	dao.On("GetTaskSegmentsWithTiles", task.Id, false).Once()
//	dao.On("UpdateOrigTaskThumbs", task.Id, mock.Anything, mock.Anything).Once()
//	dao.On("GetOrigTaskStatus", task.Id).Once()
//
//	ts := &mockTaskStorage{}
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(128000), mock.Anything).Return("tiles-out-0-128000.mp4").Once()
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(64000), mock.Anything).Return("tiles-out-0-64000.mp4").Once()
//	ts.On("DownloadAllTilesAndScreens", task.Id, mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Once()
//	for _, h := range []uint32{240, 360, 480, 576, 720} {
//		ts.On("DownloadAllTranscodedSegments", task.Id, mock.Anything, task.SegmentsCount-1, h, mock.Anything).Return("tiles-out-%d-%d.mp4").Once()
//	}
//
//	ts.On("RemoveTaskData", task.Id, mock.Anything).Once()
//	up := &mockUploader{}
//	// mpegts chunks uploading
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(0, nil).Once()
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(29, nil).Times(5)
//	// Zero duration and empty lowres tiles here because tiles creation logic
//	// has been completely changed since the moment when this test was created.
//	expMasterPlaylist := `#EXTM3U
//#EXT-X-VERSION:3
//#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=649622,RESOLUTION=480x240
//123_169_240p.m3u8
//#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1123304,RESOLUTION=720x360
//123_169_360p.m3u8
//#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1819840,RESOLUTION=960x480
//123_169_480p.m3u8
//#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=2896231,RESOLUTION=1152x576
//123_169_576p.m3u8
//#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=4532317,RESOLUTION=1440x720
//123_169_720p.m3u8
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.version",VALUE="2"
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.uri",VALUE="https://strm.yandex.ru/transcoder-test/sukhomlin-v-test/123_sprite-$Number$.jpg"
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.tiles",VALUE="5x5"
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.duration",VALUE="0.000000"
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.resolution",VALUE="450x225"
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.lowres.uri",VALUE=""
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.lowres.tiles",VALUE=""
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.lowres.duration",VALUE="0.000000"
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.lowres.resolution",VALUE="0x0"`
//	// playlist uploading
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(1, func(f string) error {
//		raw, err := ioutil.ReadFile(f)
//		require.NoError(t, err)
//		require.Equal(t, expMasterPlaylist, string(raw))
//		return nil
//	}).Once()
//
//	keys := make(config.Keys)
//	keys["transcoder-test"] = config.BucketInfo{Strm: true}
//
//	metarobotQueue := &mockOutput{}
//	metarobotQueue.On("Push", mock.Anything).Once()
//
//	whQueue := &mockOutput{}
//	whQueue.On("PushFifo", mock.Anything).Once()
//
//	ctx := comb.CombiningContext{
//		Lg:           lg,
//		FfmpegPath:   ffmpegBin,
//		FfprobePath:  ffprobeBin,
//		Mp42hlsPath:  mp42hlsBin,
//		TaskStorage:  ts,
//		Dao:          dao,
//		WorkDir:      wd,
//		S3keys:       keys,
//		Up:           up,
//		MrobotQ:      metarobotQueue,
//		WebhookQueue: whQueue,
//		Met:          met,
//	}
//
//	err = comb.Combine(ctx,
//		fmt.Sprintf(`Id: "%s"
//SpritesInfo: <
//HorizontalTilesNum: 5
//VerticalTilesNum: 5
//TilesInterval: 2
//TileHeight: 90
//TileWidth: 160
//SpriteHeight: 450
//SpriteWidth: 800
//SpriteDuration: 50
//ScreenHeight: 720
//ScreenWidth: 1440
//>`, task.Id))
//	require.NoError(t, err)
//
//	dao.AssertExpectations(t)
//	ts.AssertExpectations(t)
//	up.AssertExpectations(t)
//}

// deleted resource by ttl
//func TestAvatarsUpload(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := ioutil.TempDir("", "comb-test-")
//	require.NoError(t, err)
//	defer func() { _ = os.RemoveAll(wd) }()
//
//	task := mustReadTask("task_avatars.proto")
//
//	tilesStr, err := ioutil.ReadFile("tiles_info.json")
//	require.NoError(t, err)
//
//	dao := &mockDao{}
//	dao.On("FindOrigTask", task.Id).Return(&opb.TTaskInfo{TimelineTiles: string(tilesStr)})
//	dao.On("FindTask", task.Id).Return(task).Once()
//	dao.On("UpdateOrigTaskOutput", task.Id, "test", mock.Anything, mock.Anything).Return(task).Once()
//	dao.On("UpdateTaskContentStatus", task.Id, opb.EContentStatus_ECSNotNeeded).Once()
//	dao.On("UpdateTaskPreviewStatus", task.Id, opb.EPreviewStatus_EPSNotNeeded).Once()
//	dao.On("UpdateTaskSignaturesStatus", task.Id, opb.ESignaturesStatus_ESSNotNeeded).Once()
//	dao.On("GetTaskSegmentsWithTiles", task.Id, false).Once()
//	dao.On("UpdateOrigTaskThumbs", task.Id, mock.Anything, mock.Anything).Once()
//	dao.On("GetOrigTaskStatus", task.Id).Once()
//
//	ts := &mockTaskStorage{}
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(128000), mock.Anything).Return("tiles-out-0-128000.mp4").Once()
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(64000), mock.Anything).Return("tiles-out-0-64000.mp4").Once()
//	ts.On("DownloadAllTilesAndScreens", task.Id, mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Once()
//	for _, h := range []uint32{240, 360, 480, 576, 720} {
//		ts.On("DownloadAllTranscodedSegments", task.Id, mock.Anything, task.SegmentsCount-1, h, mock.Anything).Return("tiles-out-%d-%d.mp4").Once()
//	}
//
//	ts.On("RemoveTaskData", task.Id, mock.Anything).Once()
//	up := &mockUploader{}
//	// mpegts chunks uploading
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(0, nil).Once()
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(29, nil).Times(5)
//	// playlist uploading
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(1, nil).Once()
//	up.On("AvatarsUpload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Once()
//
//	keys := make(config.Keys)
//	keys["transcoder-test"] = config.BucketInfo{Strm: true}
//
//	metarobotQueue := &mockOutput{}
//	metarobotQueue.On("Push", mock.Anything).Once()
//
//	whQueue := &mockOutput{}
//	whQueue.On("PushFifo", mock.Anything).Once()
//
//	ctx := comb.CombiningContext{
//		Lg:           lg,
//		FfmpegPath:   ffmpegBin,
//		FfprobePath:  ffprobeBin,
//		Mp42hlsPath:  mp42hlsBin,
//		TaskStorage:  ts,
//		Dao:          dao,
//		WorkDir:      wd,
//		S3keys:       keys,
//		Up:           up,
//		MrobotQ:      metarobotQueue,
//		WebhookQueue: whQueue,
//		Met:          met,
//	}
//
//	err = comb.Combine(ctx,
//		fmt.Sprintf(`Id: "%s"
//SpritesInfo: <
//HorizontalTilesNum: 5
//VerticalTilesNum: 5
//TilesInterval: 2
//TileHeight: 90
//TileWidth: 160
//SpriteHeight: 450
//SpriteWidth: 800
//SpriteDuration: 50
//ScreenHeight: 720
//ScreenWidth: 1440
//>`, task.Id))
//	require.NoError(t, err)
//
//	dao.AssertExpectations(t)
//	ts.AssertExpectations(t)
//	up.AssertExpectations(t)
//}

func TestHlsHighResDelegate(t *testing.T) {
	lg := util.MakeLogger()

	wd, err := ioutil.TempDir("", "comb-test-")
	require.NoError(t, err)
	defer func() { _ = os.RemoveAll(wd) }()

	task := mustReadTask("bin_task.proto")

	if !task.OrigTask.Simple.PublishLowResFirst {
		panic("Incorrect unmarshaling")
	}

	dao := &mockDao{}
	dao.On("FindTask", task.Id).Return(task).Once()

	dao.On("GetTaskLowResStatus", task.Id).Return(opb.ELowResStatus_ELSLowResTranscoding).Once()

	dao.On("UpdateTaskHighResStatus", task.Id, opb.EHighResStatus_EHSHighResPulled).Once()

	dao.On("GetOrigTaskStatus", task.Id).Once()

	keys := make(config.Keys)
	keys["transcoder-test"] = config.BucketInfo{Strm: true}

	whQueue := &mockOutput{}
	whQueue.On("PushFifo", mock.Anything).Once()

	ctx := comb.CombiningContext{
		Lg:           lg,
		FfmpegPath:   ffmpegBin,
		FfprobePath:  ffprobeBin,
		Mp42hlsPath:  mp42hlsBin,
		Dao:          dao,
		WorkDir:      wd,
		S3keys:       keys,
		Met:          met,
		WebhookQueue: whQueue,
	}

	err = comb.Combine(ctx, fmt.Sprintf("Id: \"%s\"\n IsLowRes: false", task.Id))
	require.NoError(t, err)

	dao.AssertExpectations(t)
}

// deleted resource by ttl
//func TestOneOutputTask(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := ioutil.TempDir("", "comb-test-")
//	require.NoError(t, err)
//	defer func() { _ = os.RemoveAll(wd) }()
//
//	task := mustReadTask("one_output_task.proto")
//
//	dao := &mockDao{}
//	dao.On("FindOrigTask", task.Id).Return(&opb.TTaskInfo{}).Once()
//	dao.On("FindTask", task.Id).Return(task).Once()
//
//	dao.On("UpdateOrigTaskOutput", task.Id, mock.Anything, mock.Anything, mock.Anything).Return(task).Once()
//	dao.On("UpdateTaskContentStatus", task.Id, mock.Anything).Once()
//	dao.On("UpdateTaskPreviewStatus", task.Id, mock.Anything).Once()
//	dao.On("UpdateTaskSignaturesStatus", task.Id, mock.Anything).Once()
//	dao.On("UpdateTaskLowResStatus", task.Id, mock.Anything).Once()
//	dao.On("GetOrigTaskStatus", task.Id).Once()
//	// The main part of the test.
//	// We must not process task as lowres if it has only one output.
//	dao.AssertNotCalled(t, "UpdateTaskHighResStatus", task.Id, mock.Anything)
//	dao.AssertNotCalled(t, "GetTaskLowResStatus", task.Id)
//	dao.AssertNotCalled(t, "GetTaskHighResStatus", task.Id)
//
//	// Dummy mocking
//	ts := &mockTaskStorage{}
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(64000), mock.Anything).Return("audio-64000.mp4").Once()
//	for _, h := range []uint32{568} {
//		ts.On("DownloadAllTranscodedSegments", task.Id, mock.Anything, task.SegmentsCount-1, h, mock.Anything).Return("chunk-%d-%d.mp4").Once()
//	}
//
//	ts.On("RemoveTaskData", task.Id, mock.Anything).Once()
//
//	up := &mockUploader{}
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(3, nil).Once()
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(1, nil).Once()
//
//	keys := make(config.Keys)
//	keys["transcoder-test"] = config.BucketInfo{}
//
//	whQueue := &mockOutput{}
//	whQueue.On("PushFifo", mock.Anything).Once()
//
//	ctx := comb.CombiningContext{
//		Lg:           lg,
//		FfmpegPath:   ffmpegBin,
//		FfprobePath:  ffprobeBin,
//		Mp42hlsPath:  mp42hlsBin,
//		TaskStorage:  ts,
//		Dao:          dao,
//		WorkDir:      wd,
//		S3keys:       keys,
//		Up:           up,
//		Met:          met,
//		WebhookQueue: whQueue,
//	}
//
//	err = comb.Combine(ctx, fmt.Sprintf("Id: \"%s\"", task.Id))
//	require.NoError(t, err)
//
//	dao.AssertExpectations(t)
//	ts.AssertExpectations(t)
//	up.AssertExpectations(t)
//}

//func TestHlsLowResVideo(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := ioutil.TempDir("", "comb-test-")
//	require.NoError(t, err)
//	defer func() { _ = os.RemoveAll(wd) }()
//
//	task := mustReadTask("bin_task.proto")
//
//	task.OrigTask.Simple.MetarobotTask = nil
//	task.SegmentsCount = 7
//
//	dao := &mockDao{}
//	dao.On("FindOrigTask", task.Id).Return(&opb.TTaskInfo{})
//	dao.On("FindTask", task.Id).Return(task).Once()
//
//	dao.On("UpdateOrigTaskUrls", task.Id, mock.Anything, mock.Anything).Once()
//
//	dao.On("UpdateTaskLowResStatus", task.Id, opb.ELowResStatus_ELSLowResDone).Once()
//	dao.On("GetTaskHighResStatus", task.Id).Return(opb.EHighResStatus_EHSHighResTranscoding).Once()
//	dao.On("SetOrigTaskLowResTranscoded", task.Id).Once()
//
//	dao.On("GetTaskSegmentsWithTiles", task.Id, mock.Anything).Return([]pb.TTranscodeSegmentTask{}).Once()
//	dao.On("UpdateOrigTaskThumbs", task.Id, mock.Anything, mock.Anything).Once()
//
//	dao.On("GetOrigTaskStatus", task.Id).Times(2)
//
//	ts := &mockTaskStorage{}
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(64000), mock.Anything).Return("lowres_audio-64000.mp4").Once()
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(128000), mock.Anything).Return("lowres_audio-128000.mp4").Once()
//	ts.On("DownloadAllTilesAndScreens", task.Id, mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Once()
//
//	for _, h := range []uint32{240} {
//		ts.On("DownloadAllTranscodedSegments", task.Id, mock.Anything, task.SegmentsCount-1, h, mock.Anything).Return("out-%d-%d.mp4").Once()
//	}
//
//	up := &mockUploader{}
//
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(0, nil).Once()
//
//	// mpegts chunks uploading
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(29, nil).Once()
//
//	expMasterPlaylist := `#EXTM3U
//#EXT-X-VERSION:3
//#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=642103,RESOLUTION=480x240
//123_169_240p.m3u8
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.version",VALUE="2"
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.uri",VALUE=""
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.tiles",VALUE=""
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.duration",VALUE="0.000000"
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.resolution",VALUE="0x0"
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.lowres.uri",VALUE=""
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.lowres.tiles",VALUE=""
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.lowres.duration",VALUE="0.000000"
//#EXT-X-SESSION-DATA:DATA-ID="com.yandex.video.thumbnail.lowres.resolution",VALUE="0x0"`
//	// playlist uploading
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(1, func(f string) error {
//		raw, err := ioutil.ReadFile(f)
//		require.NoError(t, err)
//		require.Equal(t, expMasterPlaylist, string(raw))
//		return nil
//	}).Once()
//
//	keys := make(config.Keys)
//	keys["transcoder-test"] = config.BucketInfo{Strm: true}
//
//	whQueue := &mockOutput{}
//	whQueue.On("PushFifo", mock.Anything).Once()
//
//	ctx := comb.CombiningContext{
//		Lg:           lg,
//		FfmpegPath:   ffmpegBin,
//		FfprobePath:  ffprobeBin,
//		Mp42hlsPath:  mp42hlsBin,
//		TaskStorage:  ts,
//		Dao:          dao,
//		WorkDir:      wd,
//		S3keys:       keys,
//		Up:           up,
//		WebhookQueue: whQueue,
//		Met:          met,
//	}
//
//	ws, err := testutil.NewWwebhookServer(13095)
//	require.NoError(t, err)
//
//	err = comb.Combine(ctx, fmt.Sprintf("Id: \"%s\"\n IsLowRes: true \n SpritesInfo: {}", task.Id))
//	require.NoError(t, err)
//
//	require.NoError(t, ws.Stop())
//
//	dao.AssertExpectations(t)
//	ts.AssertExpectations(t)
//	up.AssertExpectations(t)
//}

// deleted resource by ttl
//func TestMp4OnlyVideo(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := ioutil.TempDir("", "comb-test-")
//	require.NoError(t, err)
//	defer func() { _ = os.RemoveAll(wd) }()
//
//	task := mustReadTask("task2.proto")
//
//	dao := &mockDao{}
//	dao.On("FindOrigTask", task.Id).Return(&opb.TTaskInfo{}).Once()
//	dao.On("FindTask", task.Id).Return(task).Once()
//	var ourl *string
//	dao.On("UpdateOrigTaskOutput", task.Id, "mt", ourl, mock.MatchedBy(func(o []*opb.TOutputInfo) bool { return len(o) == 3 })).Return(task).Once()
//	dao.On("UpdateTaskContentStatus", task.Id, opb.EContentStatus_ECSNotNeeded).Once()
//	dao.On("UpdateTaskPreviewStatus", task.Id, opb.EPreviewStatus_EPSNotNeeded).Once()
//	dao.On("UpdateTaskSignaturesStatus", task.Id, opb.ESignaturesStatus_ESSNotNeeded).Once()
//	dao.On("GetTaskSegmentsWithTiles", task.Id, mock.Anything).Once()
//	dao.On("UpdateOrigTaskThumbs", task.Id, mock.Anything, mock.Anything).Once()
//	dao.On("GetOrigTaskStatus", task.Id).Once()
//
//	ts := &mockTaskStorage{}
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(64000), mock.Anything).Return("audio-64000.mp4").Once()
//	for _, h := range []uint32{240, 360, 480} {
//		ts.On("DownloadAllTranscodedSegments", task.Id, mock.Anything, task.SegmentsCount-1, h, mock.Anything).Return("chunk-%d-%d.mp4").Once()
//	}
//
//	ts.On("DownloadAllTilesAndScreens", task.Id, mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Once()
//
//	ts.On("RemoveTaskData", task.Id, mock.Anything).Once()
//
//	up := &mockUploader{}
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(0, nil).Once()
//
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(1, nil).Times(3)
//
//	keys := make(config.Keys)
//	keys["transcoder-test"] = config.BucketInfo{}
//
//	metarobotQueue := &mockOutput{}
//	metarobotQueue.On("Push", mock.Anything).Once()
//
//	whQueue := &mockOutput{}
//	whQueue.On("PushFifo", mock.Anything).Once()
//
//	ctx := comb.CombiningContext{
//		Lg:           lg,
//		FfmpegPath:   ffmpegBin,
//		FfprobePath:  ffprobeBin,
//		Mp42hlsPath:  mp42hlsBin,
//		TaskStorage:  ts,
//		Dao:          dao,
//		WorkDir:      wd,
//		S3keys:       keys,
//		Up:           up,
//		MrobotQ:      metarobotQueue,
//		WebhookQueue: whQueue,
//		Met:          met,
//	}
//
//	err = comb.Combine(ctx,
//		fmt.Sprintf(`Id: "%s"
//SpritesInfo: <
//HorizontalTilesNum: 5
//VerticalTilesNum: 5
//>`, task.Id))
//	require.NoError(t, err)
//
//	dao.AssertExpectations(t)
//	ts.AssertExpectations(t)
//	up.AssertExpectations(t)
//}

func TestMp4NoAudio(t *testing.T) {
	lg := util.MakeLogger()

	wd, err := ioutil.TempDir("", "comb-test-")
	require.NoError(t, err)
	defer func() { _ = os.RemoveAll(wd) }()

	task := mustReadTask("task3.proto")

	dao := &mockDao{}
	dao.On("FindOrigTask", task.Id).Return(&opb.TTaskInfo{}).Once()
	dao.On("FindTask", task.Id).Return(task).Once()
	var ourl *string
	dao.On("UpdateOrigTaskOutput", task.Id, "mt", ourl, mock.MatchedBy(func(o []*opb.TOutputInfo) bool { return len(o) == 3 })).Return(task).Once()
	dao.On("UpdateTaskContentStatus", task.Id, opb.EContentStatus_ECSNotNeeded).Once()
	dao.On("UpdateTaskPreviewStatus", task.Id, opb.EPreviewStatus_EPSNotNeeded).Once()
	dao.On("UpdateTaskSignaturesStatus", task.Id, opb.ESignaturesStatus_ESSNotNeeded).Once()
	dao.On("GetTaskSegmentsWithTiles", task.Id, mock.Anything).Once()
	dao.On("UpdateOrigTaskThumbs", task.Id, mock.Anything, mock.Anything).Once()
	dao.On("GetOrigTaskStatus", task.Id).Once()

	ts := &mockTaskStorage{}
	for _, h := range []uint32{240, 360, 480} {
		ts.On("DownloadAllTranscodedSegments", task.Id, mock.Anything, task.SegmentsCount, h, mock.Anything).Return("chunk-%d-%d.mp4").Once()
	}

	ts.On("DownloadAllTilesAndScreens", task.Id, mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Once()

	ts.On("RemoveTaskData", task.Id, mock.Anything).Once()

	up := &mockUploader{}
	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(0, nil).Once()
	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(1, nil).Times(3)

	keys := make(config.Keys)
	keys["transcoder-test"] = config.BucketInfo{}

	metarobotQueue := &mockOutput{}
	metarobotQueue.On("Push", mock.Anything).Once()

	whQueue := &mockOutput{}
	whQueue.On("PushFifo", mock.Anything).Once()

	ctx := comb.CombiningContext{
		Lg:           lg,
		FfmpegPath:   ffmpegBin,
		FfprobePath:  ffprobeBin,
		Mp42hlsPath:  mp42hlsBin,
		TaskStorage:  ts,
		Dao:          dao,
		WorkDir:      wd,
		S3keys:       keys,
		Up:           up,
		MrobotQ:      metarobotQueue,
		WebhookQueue: whQueue,
		Met:          met,
	}

	err = comb.Combine(ctx,
		fmt.Sprintf(`Id: "%s"
SpritesInfo: <
HorizontalTilesNum: 5
VerticalTilesNum: 5
>`, task.Id))
	require.NoError(t, err)

	dao.AssertExpectations(t)
	ts.AssertExpectations(t)
	up.AssertExpectations(t)
}

// deleted resource by ttl
//func TestSegmentProcessingError(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := ioutil.TempDir("", "comb-test-")
//	require.NoError(t, err)
//	defer func() { _ = os.RemoveAll(wd) }()
//
//	task := mustReadTask("task1.proto")
//
//	dao := &mockDao{}
//	dao.On("FindTask", task.Id).Return(task).Once()
//	dao.On("UpdateSentToPlainQueue", task.Id, true).Once()
//	dao.On("GetOrigTaskStatus", task.Id).Once()
//
//	ts := &mockTaskStorage{}
//	ts.On("RemoveTaskData", task.Id, mock.Anything).Once()
//
//	metarobotQueue := &mockOutput{}
//	metarobotQueue.On("Push", mock.Anything).Once()
//
//	plainQueue := &mockOutput{}
//	plainQueue.On("Push", mock.Anything).Once()
//
//	whQueue := &mockOutput{}
//	whQueue.On("PushFifo", mock.Anything).Once()
//
//	ctx := comb.CombiningContext{
//		Lg:           lg,
//		Dao:          dao,
//		WorkDir:      wd,
//		MrobotQ:      metarobotQueue,
//		TaskStorage:  ts,
//		PlainQueue:   plainQueue,
//		WebhookQueue: whQueue,
//		Met:          met,
//	}
//
//	doneMsg := proto.MarshalTextString(&pb.TTranscodeSegmentDone{
//		Id:         "ba10d6b5-955c23bd-82167488-1dcd4492",
//		SegmentNum: 1,
//		Error:      "some error",
//	})
//
//	_ = comb.Combine(ctx, doneMsg)
//
//	dao.AssertExpectations(t)
//	ts.AssertExpectations(t)
//}

// deleted resource by ttl
//func TestConcatError(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := ioutil.TempDir("", "comb-test-")
//	require.NoError(t, err)
//	defer func() { _ = os.RemoveAll(wd) }()
//
//	task := mustReadTask("task_combine_error.proto")
//
//	dao := &mockDao{}
//	dao.On("FindTask", task.Id).Return(task).Once()
//	dao.On("UpdateSentToPlainQueue", task.Id, true).Once()
//	dao.On("GetOrigTaskStatus", task.Id).Once()
//
//	ts := &mockTaskStorage{}
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(64000), mock.Anything).Return("audio-64000.mp4").Once()
//	ts.On("DownloadAllTranscodedSegments", task.Id, mock.Anything, uint32(4), uint32(240), mock.Anything).Return("concat-err-%d-%d.mp4").Once()
//
//	//ts.On("DownloadAllTilesAndScreens", task.Id, mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Once()
//	ts.On("RemoveTaskData", task.Id, mock.Anything).Once()
//
//	plainQueue := &mockOutput{}
//	plainQueue.On("Push", mock.Anything).Once()
//
//	whQueue := &mockOutput{}
//	whQueue.On("PushFifo", mock.Anything).Once()
//
//	ctx := comb.CombiningContext{
//		Lg:           lg,
//		FfmpegPath:   ffmpegBin,
//		FfprobePath:  ffprobeBin,
//		Mp42hlsPath:  mp42hlsBin,
//		TaskStorage:  ts,
//		Dao:          dao,
//		WorkDir:      wd,
//		PlainQueue:   plainQueue,
//		WebhookQueue: whQueue,
//		Met:          met,
//	}
//
//	err = comb.Combine(ctx,
//		fmt.Sprintf(`Id: "%s"
//SpritesInfo: <
//HorizontalTilesNum: 5
//VerticalTilesNum: 5
//>`, task.Id))
//	require.NoError(t, err)
//
//	dao.AssertExpectations(t)
//	ts.AssertExpectations(t)
//	plainQueue.AssertExpectations(t)
//}

// deleted resource by ttl
//func TestSendingTaskToMetarobot(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := ioutil.TempDir("", "comb-test-")
//	require.NoError(t, err)
//	defer func() { _ = os.RemoveAll(wd) }()
//
//	task := mustReadTask("task4.proto")
//
//	dao := &mockDao{}
//	dao.On("FindOrigTask", task.Id).Return(&opb.TTaskInfo{}).Times(2)
//	dao.On("FindTask", task.Id).Return(task).Once()
//	dao.On("GetTaskSegmentsWithTiles", task.Id, mock.Anything).Once()
//	dao.On("UpdateOrigTaskThumbs", task.Id, mock.Anything, mock.Anything).Once()
//	dao.On("GetOrigTaskStatus", task.Id).Once()
//
//	var ourl *string
//	dao.On("UpdateOrigTaskOutput", task.Id, "mt", ourl, mock.MatchedBy(func(o []*opb.TOutputInfo) bool { return len(o) == 3 })).Return(task).Once()
//	dao.AssertNotCalled(t, "UpdateTaskContentStatus", mock.Anything, mock.Anything)
//	dao.AssertNotCalled(t, "UpdateTaskPreviewStatus", mock.Anything, mock.Anything)
//	dao.AssertNotCalled(t, "UpdateTaskSignaturesStatus", mock.Anything, mock.Anything)
//
//	ts := &mockTaskStorage{}
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(64000), mock.Anything).Return("audio-64000.mp4").Once()
//	for _, h := range []uint32{240, 360, 480} {
//		ts.On("DownloadAllTranscodedSegments", task.Id, mock.Anything, task.SegmentsCount-1, h, mock.Anything).Return("chunk-%d-%d.mp4").Once()
//	}
//
//	ts.On("DownloadAllTilesAndScreens", task.Id, mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Once()
//
//	ts.On("RemoveTaskData", task.Id, mock.Anything).Once()
//
//	up := &mockUploader{}
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(0, nil).Once()
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(1, nil).Times(3)
//
//	keys := make(config.Keys)
//	keys["transcoder-test"] = config.BucketInfo{}
//
//	metarobotQueue := &mockOutput{}
//	metarobotQueue.On("Push", mock.Anything).Once()
//
//	whQueue := &mockOutput{}
//	whQueue.On("PushFifo", mock.Anything).Once()
//
//	ctx := comb.CombiningContext{
//		Lg:           lg,
//		FfmpegPath:   ffmpegBin,
//		FfprobePath:  ffprobeBin,
//		Mp42hlsPath:  mp42hlsBin,
//		TaskStorage:  ts,
//		Dao:          dao,
//		WorkDir:      wd,
//		S3keys:       keys,
//		Up:           up,
//		MrobotQ:      metarobotQueue,
//		WebhookQueue: whQueue,
//		Met:          met,
//	}
//
//	err = comb.Combine(ctx,
//		fmt.Sprintf(`Id: "%s"
//SpritesInfo: <
//HorizontalTilesNum: 5
//VerticalTilesNum: 5
//>`, task.Id))
//	require.NoError(t, err)
//	whQueue.AssertExpectations(t)
//	dao.AssertExpectations(t)
//	ts.AssertExpectations(t)
//	up.AssertExpectations(t)
//}

// deleted resource by ttl
//func TestMetarobotPushFailed(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := ioutil.TempDir("", "comb-test-")
//	require.NoError(t, err)
//	defer func() { _ = os.RemoveAll(wd) }()
//
//	task := mustReadTask("task4.proto")
//
//	dao := &mockDao{}
//	dao.On("FindOrigTask", task.Id).Return(&opb.TTaskInfo{}).Times(2)
//	dao.On("FindTask", task.Id).Return(task).Once()
//	var ourl *string
//	dao.On("UpdateOrigTaskOutput", task.Id, "mt", ourl, mock.MatchedBy(func(o []*opb.TOutputInfo) bool { return len(o) == 3 })).Return(task).Once()
//	dao.On("UpdateTaskContentStatus", task.Id, opb.EContentStatus_ECSFail).Once()
//	dao.On("UpdateTaskPreviewStatus", task.Id, opb.EPreviewStatus_EPSFail).Once()
//	dao.On("UpdateTaskSignaturesStatus", task.Id, opb.ESignaturesStatus_ESSFail).Once()
//	dao.On("GetTaskSegmentsWithTiles", task.Id, mock.Anything).Once()
//	dao.On("UpdateOrigTaskThumbs", task.Id, mock.Anything, mock.Anything).Once()
//	dao.On("GetOrigTaskStatus", task.Id).Once()
//
//	ts := &mockTaskStorage{}
//	ts.On("DownloadTranscodedSegment", task.Id, mock.Anything, uint32(0), uint32(64000), mock.Anything).Return("audio-64000.mp4").Once()
//	for _, h := range []uint32{240, 360, 480} {
//		ts.On("DownloadAllTranscodedSegments", task.Id, mock.Anything, task.SegmentsCount-1, h, mock.Anything).Return("chunk-%d-%d.mp4").Once()
//	}
//
//	ts.On("DownloadAllTilesAndScreens", task.Id, mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Once()
//
//	up := &mockUploader{}
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(0, nil).Once()
//	up.On("S3Upload", mock.Anything, mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(1, nil).Times(3)
//
//	keys := make(config.Keys)
//	keys["transcoder-test"] = config.BucketInfo{}
//
//	metarobotBrokenQueue := &mockOutputBrokenQueue{}
//	metarobotBrokenQueue.On("Push", mock.Anything).Once()
//
//	whQueue := &mockOutput{}
//	whQueue.On("PushFifo", mock.Anything).Once()
//
//	ctx := comb.CombiningContext{
//		Lg:           lg,
//		FfmpegPath:   ffmpegBin,
//		FfprobePath:  ffprobeBin,
//		Mp42hlsPath:  mp42hlsBin,
//		TaskStorage:  ts,
//		Dao:          dao,
//		WorkDir:      wd,
//		S3keys:       keys,
//		Up:           up,
//		MrobotQ:      metarobotBrokenQueue,
//		WebhookQueue: whQueue,
//		Met:          met,
//	}
//
//	err = comb.Combine(ctx,
//		fmt.Sprintf(`Id: "%s"
//SpritesInfo: <
//HorizontalTilesNum: 5
//VerticalTilesNum: 5
//>`, task.Id))
//	require.Error(t, err)
//	require.False(t, xerrors.Is(err, comb.ErrNotRetryable))
//	whQueue.AssertExpectations(t)
//	metarobotBrokenQueue.AssertExpectations(t)
//	dao.AssertExpectations(t)
//	ts.AssertExpectations(t)
//	up.AssertExpectations(t)
//}

func TestDoneTaskInput(t *testing.T) {
	lg := util.MakeLogger()

	wd, err := ioutil.TempDir("", "comb-test-")
	require.NoError(t, err)
	defer func() { _ = os.RemoveAll(wd) }()

	task := mustReadTask("task4.proto")

	dao := &mockDao{}
	dao.On("FindOrigTask", task.Id).Return(&opb.TTaskInfo{}).Once()
	dao.On("FindTask", task.Id).Return(task).Once()
	dao.On("GetOrigTaskStatus", task.Id).Return(opb.ETaskStatus(5)).Once()

	up := &mockUploader{}

	ts := &mockTaskStorage{}

	whQueue := &mockOutput{}
	whQueue.On("PushFifo", mock.Anything).Once()

	ctx := comb.CombiningContext{
		Lg:           lg,
		FfmpegPath:   ffmpegBin,
		FfprobePath:  ffprobeBin,
		Mp42hlsPath:  mp42hlsBin,
		TaskStorage:  ts,
		Dao:          dao,
		WorkDir:      wd,
		Up:           up,
		WebhookQueue: whQueue,
		Met:          met,
	}

	err = comb.Combine(ctx,
		fmt.Sprintf(`Id: "%s"`, task.Id))
	require.NoError(t, err)
	whQueue.AssertExpectations(t)
	dao.AssertExpectations(t)
	ts.AssertExpectations(t)
}

func TestFailedTaskInput(t *testing.T) {
	lg := util.MakeLogger()

	task := mustReadTask("task4.proto")

	dao := &mockDao{}
	dao.On("FindOrigTask", task.Id).Return(&opb.TTaskInfo{}).Once()
	dao.On("FindTask", task.Id).Return(task).Once()
	dao.On("GetOrigTaskStatus", task.Id).Return(opb.ETaskStatus(5)).Once()

	up := &mockUploader{}

	whQueue := &mockOutput{}
	whQueue.On("PushFifo", mock.Anything).Once()

	ctx := comb.CombiningContext{
		Lg:           lg,
		FfmpegPath:   ffmpegBin,
		FfprobePath:  ffprobeBin,
		Mp42hlsPath:  mp42hlsBin,
		Dao:          dao,
		Up:           up,
		WebhookQueue: whQueue,
		Met:          met,
	}

	err := comb.Combine(ctx,
		fmt.Sprintf(`Id: "%s"`, task.Id))
	require.NoError(t, err)
	whQueue.AssertExpectations(t)
	dao.AssertExpectations(t)
}
