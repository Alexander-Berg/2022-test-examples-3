package tc_test

import (
	"io/ioutil"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/config"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/db"
	pb "a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/proto"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/queue"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/segment"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/testutil"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/tstor"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/util"
	opb "a.yandex-team.ru/extsearch/video/robot/rt_transcoder/proto"
)

const (
	// sandbox resource binaries come readonly, not executable. we copy them and make executable.
	ffmpegBin   = "./ffmpeg"
	ffprobeBin  = "./ffprobe"
	jpegtranBin = "./jpegtran_bin"
)

var ffmpegBinAbs string
var ffprobeBinAbs string
var jpegtranBinAbs string

type mockDownloader struct {
	mock.Mock
	segment.Downloader
}

func (dl *mockDownloader) Download(url, to string) (int64, error) {
	args := dl.Called(url, to)
	return 1, util.CopyFile(args.String(0), to)
}

type mockDao struct {
	mock.Mock
	db.Dao
}

func (d *mockDao) UpdateOrigTaskStatus(taskID string, status opb.ETaskStatus) error {
	d.Called(taskID, status)
	return nil
}

func (d *mockDao) UpdateOrigTaskError(taskID, user string, errorCode opb.EErrorCode, errorDesc string) error {
	d.Called(taskID, user, errorCode, errorDesc)
	return nil
}

func (d *mockDao) AddTask(task *pb.TTranscodeTask) error {
	d.Called(task)
	return nil
}

func (d *mockDao) UpdateOrigTaskThumbs(taskID string, thumbs []string, tiles *opb.TTimelineTiles) error {
	d.Called(taskID, thumbs, tiles)
	return nil
}

func (d *mockDao) FindOrigTask(taskID string) (*opb.TTaskInfo, error) {
	d.Called(taskID)
	return &opb.TTaskInfo{}, nil
}

func (d *mockDao) UpdateDownloadTime(taskID string, ts uint32) error {
	d.Called(taskID, ts)
	return nil
}

type mockTaskStorage struct {
	mock.Mock
	tstor.TaskStorage
}

func (d *mockTaskStorage) UploadSegment(taskID string, timestamp uint64, segmentNum uint32, file string) error {
	d.Called(taskID, timestamp, segmentNum, file)
	return nil
}

func (d *mockTaskStorage) RemoveTaskData(taskID string, timestamp uint64) error {
	d.Called(taskID, timestamp)
	return nil
}

type mockOutput struct {
	mock.Mock
	queue.Output
}

func (o *mockOutput) Push(msg string) error {
	o.Called(msg)
	return nil
}

type Metrics struct {
}

func (m *Metrics) MessageProceed(segmenting time.Duration, err error)      {}
func (m *Metrics) DownloadStatus(downloadingTime time.Duration, err error) {}
func (m *Metrics) SentToPlain(user string)                                 {}

var met = &Metrics{}

func TestMain(m *testing.M) {
	testutil.MustCopyBin("./pack/ffmpeg", ffmpegBin)
	testutil.MustCopyBin("./pack/ffprobe", ffprobeBin)
	testutil.MustCopyBin("./jpegtran", jpegtranBin)
	ffmpegBinAbs, _ = filepath.Abs(ffmpegBin)
	ffprobeBinAbs, _ = filepath.Abs(ffprobeBin)
	jpegtranBinAbs, _ = filepath.Abs(jpegtranBin)
	os.Exit(m.Run())
}

// deleted resource by ttl
//func TestIncorrectFfmpegPath(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := os.Getwd()
//	require.NoError(t, err)
//	wd += "/" + t.Name()
//	require.NoError(t, os.Mkdir(wd, 0777))
//	taskB64, err := ioutil.ReadFile("task1.b64")
//	require.NoError(t, err)
//
//	dl := &mockDownloader{}
//	dl.On("Download", mock.Anything, mock.Anything).Return("P02PdluXQVk.mp4").Once()
//
//	ctx := segment.SegmentingContext{
//		Lg:          lg,
//		FfmpegPath:  "./no-file",
//		FfprobePath: "./no-file",
//		WorkDir:     wd,
//		Dl:          dl,
//		Met:         met,
//	}
//
//	err = segment.Segment(ctx, string(taskB64))
//	require.Error(t, err)
//	require.False(t, xerrors.Is(err, segment.ErrBadInput))
//}

// deleted resource by ttl
//func TestUnsupportedInput(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := os.Getwd()
//	require.NoError(t, err)
//	wd += "/" + t.Name()
//	require.NoError(t, os.Mkdir(wd, 0777))
//	taskB64, err := ioutil.ReadFile("task1.b64")
//	require.NoError(t, err)
//
//	dao := &mockDao{}
//	dao.On("UpdateOrigTaskError", "22e4a7fd-85d1b60f-790d58a4-60916813", "", opb.EErrorCode_EBadInput, mock.Anything).Once()
//	dao.On("UpdateOrigTaskStatus", mock.Anything, mock.Anything).Once()
//	dao.On("UpdateDownloadTime", mock.Anything, mock.Anything).Once()
//	dao.On("FindOrigTask", mock.Anything).Times(2)
//
//	dl := &mockDownloader{}
//	dl.On(
//		"Download",
//		"http://trailers.s3.mds.yandex.net/video_original/158396-fe0e27f6797ae597e5be6fcb3afcdfb0.mov",
//		mock.Anything).Return("P02PdluXQVk.mp4").Once()
//
//	plainQueue := &mockOutput{}
//	plainQueue.On("Push", mock.Anything).Once()
//
//	ts := &mockTaskStorage{}
//	ts.On("RemoveTaskData", mock.Anything, mock.Anything).Once()
//
//	ctx := segment.SegmentingContext{
//		Lg:              lg,
//		FfmpegPath:      ffmpegBinAbs,
//		FfprobePath:     ffprobeBinAbs,
//		WorkDir:         wd,
//		Dao:             dao,
//		Dl:              dl,
//		PlainQueue:      plainQueue,
//		SegmentDuration: 10,
//		TaskStorage:     ts,
//		Met:             met,
//	}
//
//	ws, err := testutil.NewWwebhookServer(13090)
//	require.NoError(t, err)
//
//	err = segment.Segment(ctx, string(taskB64))
//	require.Error(t, err)
//	require.True(t, xerrors.Is(err, segment.ErrBadInput))
//
//	require.NoError(t, ws.Stop())
//	require.Equal(t, 0, len(ws.GetCalls()))
//	ts.AssertExpectations(t)
//	plainQueue.AssertExpectations(t)
//	dl.AssertExpectations(t)
//}

// deleted resource by ttl
//func TestUnsupportedFFprobeInput(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := os.Getwd()
//	require.NoError(t, err)
//	wd += "/" + t.Name()
//	require.NoError(t, os.Mkdir(wd, 0777))
//	taskB64, err := ioutil.ReadFile("task1.b64")
//	require.NoError(t, err)
//
//	dao := &mockDao{}
//	dao.On("UpdateOrigTaskError", "22e4a7fd-85d1b60f-790d58a4-60916813", "", opb.EErrorCode_EBadInput, mock.Anything).Once()
//	dao.On("UpdateOrigTaskStatus", mock.Anything, mock.Anything).Once()
//	dao.On("UpdateDownloadTime", mock.Anything, mock.Anything).Once()
//	dao.On("FindOrigTask", mock.Anything).Times(3)
//
//	dl := &mockDownloader{}
//	dl.On(
//		"Download",
//		"http://trailers.s3.mds.yandex.net/video_original/158396-fe0e27f6797ae597e5be6fcb3afcdfb0.mov",
//		mock.Anything).Return("10802624590361804523").Once()
//
//	plainQueue := &mockOutput{}
//	plainQueue.On("Push", mock.Anything).Once()
//
//	ts := &mockTaskStorage{}
//	ts.On("RemoveTaskData", mock.Anything, mock.Anything).Once()
//
//	ctx := segment.SegmentingContext{
//		Lg:              lg,
//		FfmpegPath:      ffmpegBinAbs,
//		FfprobePath:     ffprobeBinAbs,
//		WorkDir:         wd,
//		Dao:             dao,
//		Dl:              dl,
//		PlainQueue:      plainQueue,
//		SegmentDuration: 10,
//		TaskStorage:     ts,
//		Met:             met,
//	}
//	err = segment.Segment(ctx, string(taskB64))
//	require.Error(t, err)
//	require.True(t, xerrors.Is(err, segment.ErrBadInput))
//	ts.AssertExpectations(t)
//	plainQueue.AssertExpectations(t)
//	dl.AssertExpectations(t)
//}

// deleted resource by ttl
//func TestSuccessPathNoThumbs(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := os.Getwd()
//	require.NoError(t, err)
//	wd += "/" + t.Name()
//	require.NoError(t, os.Mkdir(wd, 0777))
//
//	taskB64, err := ioutil.ReadFile("task1.b64")
//	require.NoError(t, err)
//
//	dao := &mockDao{}
//	dao.On("AddTask", mock.Anything).Once()
//	dao.On("UpdateOrigTaskStatus", mock.Anything, mock.Anything).Once()
//	dao.On("UpdateDownloadTime", mock.Anything, mock.Anything).Once()
//	dao.On("FindOrigTask", mock.Anything).Once()
//
//	dl := &mockDownloader{}
//	dl.On("Download", mock.Anything, mock.Anything).Return("1.mp4").Once()
//
//	ts := &mockTaskStorage{}
//	ts.On("UploadSegment", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Times(7)
//
//	out := &mockOutput{}
//	out.On("Push", mock.Anything).Times(7)
//
//	ctx := segment.SegmentingContext{
//		Lg:                  lg,
//		FfmpegPath:          ffmpegBinAbs,
//		FfprobePath:         ffprobeBinAbs,
//		WorkDir:             wd,
//		Dao:                 dao,
//		Dl:                  dl,
//		TaskStorage:         ts,
//		LowPriorVideoQueue:  out,
//		HighPriorVideoQueue: out,
//		LowPriorAudioQueue:  out,
//		HighPriorAudioQueue: out,
//		SegmentDuration:     10 * time.Second,
//		Met:                 met,
//	}
//
//	require.NoError(t, err)
//
//	err = segment.Segment(ctx, string(taskB64))
//	require.NoError(t, err)
//
//	ts.AssertExpectations(t)
//	out.AssertExpectations(t)
//}

// deleted resource by ttl
//func TestSuccessPathWithThumbs(t *testing.T) {
//	lg := util.MakeLogger()
//
//	wd, err := os.Getwd()
//	require.NoError(t, err)
//	wd += "/" + t.Name()
//	require.NoError(t, os.Mkdir(wd, 0777))
//
//	taskB64, err := ioutil.ReadFile("task2.b64")
//	require.NoError(t, err)
//
//	dao := &mockDao{}
//	dao.On("AddTask", mock.Anything).Once()
//	dao.On("UpdateOrigTaskThumbs", mock.Anything, mock.Anything, mock.Anything).Once()
//	dao.On("UpdateOrigTaskStatus", mock.Anything, mock.Anything).Once()
//	dao.On("UpdateDownloadTime", mock.Anything, mock.Anything).Once()
//	dao.On("FindOrigTask", mock.Anything).Times(2)
//
//	dl := &mockDownloader{}
//	dl.On("Download", mock.Anything, mock.Anything).Return("1.mp4").Once()
//
//	ts := &mockTaskStorage{}
//	ts.On("UploadSegment", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Times(7)
//
//	out := &mockOutput{}
//	out.On("Push", mock.Anything).Times(7)
//
//	keys := make(config.Keys)
//	keys["transcoder-test"] = config.BucketInfo{}
//
//	ctx := segment.SegmentingContext{
//		Lg:                  lg,
//		FfmpegPath:          ffmpegBinAbs,
//		FfprobePath:         ffprobeBinAbs,
//		JpegTranBin:         jpegtranBinAbs,
//		WorkDir:             wd,
//		Dao:                 dao,
//		Dl:                  dl,
//		TaskStorage:         ts,
//		LowPriorVideoQueue:  out,
//		HighPriorVideoQueue: out,
//		LowPriorAudioQueue:  out,
//		HighPriorAudioQueue: out,
//		S3keys:              keys,
//		SegmentDuration:     10 * time.Second,
//		Met:                 met,
//	}
//
//	err = segment.Segment(ctx, string(taskB64))
//	require.NoError(t, err)
//
//	ts.AssertExpectations(t)
//	out.AssertExpectations(t)
//}

func TestSuccessPathWithThumbsLowResFirst(t *testing.T) {
	lg := util.MakeLogger()

	wd, err := os.Getwd()
	require.NoError(t, err)
	wd += "/" + t.Name()
	require.NoError(t, os.Mkdir(wd, 0777))

	taskB64, err := ioutil.ReadFile("task3.b64")
	require.NoError(t, err)

	dl := &mockDownloader{}
	dl.On("Download", mock.Anything, mock.Anything).Return("2.mp4").Once()

	dao := &mockDao{}
	dao.On("AddTask", mock.Anything).Once()
	dao.On("UpdateOrigTaskThumbs", mock.Anything, mock.Anything, mock.Anything).Once()
	dao.On("UpdateOrigTaskStatus", mock.Anything, mock.Anything).Once()
	dao.On("UpdateDownloadTime", mock.Anything, mock.Anything).Once()
	dao.On("FindOrigTask", mock.Anything).Times(2)

	ts := &mockTaskStorage{}
	ts.On("UploadSegment", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Times(7)

	out := &mockOutput{}
	out.On("Push", mock.Anything).Times(14)

	keys := make(config.Keys)
	keys["transcoder-test"] = config.BucketInfo{}

	ctx := segment.SegmentingContext{
		Lg:                  lg,
		FfmpegPath:          ffmpegBinAbs,
		FfprobePath:         ffprobeBinAbs,
		JpegTranBin:         jpegtranBinAbs,
		WorkDir:             wd,
		Dao:                 dao,
		Dl:                  dl,
		TaskStorage:         ts,
		LowPriorVideoQueue:  out,
		HighPriorVideoQueue: out,
		LowPriorAudioQueue:  out,
		HighPriorAudioQueue: out,
		S3keys:              keys,
		SegmentDuration:     10 * time.Second,
		Met:                 met,
	}

	err = segment.Segment(ctx, string(taskB64))
	require.NoError(t, err)

	ts.AssertExpectations(t)
	out.AssertExpectations(t)
}

func TestOneOutputLowRes(t *testing.T) {
	lg := util.MakeLogger()

	wd, err := os.Getwd()
	require.NoError(t, err)
	wd += "/" + t.Name()
	require.NoError(t, os.Mkdir(wd, 0777))

	taskB64, err := ioutil.ReadFile("one_output_task.b64")
	require.NoError(t, err)

	dl := &mockDownloader{}
	dl.On("Download", mock.Anything, mock.Anything).Return("one_output_video.mp4").Once()

	dao := &mockDao{}
	dao.On("AddTask", mock.Anything).Once()
	dao.On("UpdateOrigTaskStatus", mock.Anything, mock.Anything).Once()
	dao.On("UpdateDownloadTime", mock.Anything, mock.Anything).Once()
	dao.On("FindOrigTask", mock.Anything).Once()

	ts := &mockTaskStorage{}
	ts.On("UploadSegment", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Times(74)

	out := &mockOutput{}
	out.On("Push", mock.Anything).Times(74)

	keys := make(config.Keys)
	keys["transcoder-test"] = config.BucketInfo{}

	ctx := segment.SegmentingContext{
		Lg:                  lg,
		FfmpegPath:          ffmpegBinAbs,
		FfprobePath:         ffprobeBinAbs,
		JpegTranBin:         jpegtranBinAbs,
		WorkDir:             wd,
		Dao:                 dao,
		Dl:                  dl,
		TaskStorage:         ts,
		LowPriorVideoQueue:  out,
		HighPriorVideoQueue: out,
		LowPriorAudioQueue:  out,
		HighPriorAudioQueue: out,
		S3keys:              keys,
		SegmentDuration:     10 * time.Second,
		Met:                 met,
	}

	err = segment.Segment(ctx, string(taskB64))
	require.NoError(t, err)

	ts.AssertExpectations(t)
	out.AssertExpectations(t)
}
