package tcode_test

import (
	"encoding/json"
	"io"
	"io/ioutil"
	"os"
	"testing"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"golang.org/x/xerrors"

	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/db"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/ff"
	pb "a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/proto"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/queue"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/tcode"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/testutil"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/tstor"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/util"
)

const (
	// sandbox resource binaries come readonly, not executable. we copy them and make executable.
	ffmpegBin  = "./ffmpeg"
	ffprobeBin = "./ffprobe"
	mp4dumpBin = "./mp4dump_bin"
)

type mockDao struct {
	mock.Mock
	db.Dao
}

func (d *mockDao) AddSegment(taskID string, segmentNum uint32, isLowRes bool, tilesNum uint32) error {
	d.Called(taskID, segmentNum, isLowRes, tilesNum)
	return nil
}

func (d *mockDao) CountSegments(taskID string, isLowRes bool) (uint32, error) {
	args := d.Called(taskID, isLowRes)
	return uint32(args.Int(0)), nil
}

func (d *mockDao) UpdateTaskSequentialTiles(taskID string, seq bool) error {
	d.Called(taskID, seq)
	return nil
}

type mockTaskStorage struct {
	mock.Mock
	tstor.TaskStorage
}

func (ts *mockTaskStorage) DownloadSegment(taskID string, timestamp uint64, segmentNum uint32, to string) error {
	args := ts.Called(taskID, timestamp, segmentNum, to)
	return util.CopyFile(args.String(0), to)
}

func (ts *mockTaskStorage) UploadTranscodedSegment(taskID string, timestamp uint64, segmentNum uint32, height uint32, file string) error {
	args := ts.Called(taskID, timestamp, segmentNum, height, file)
	if len(args) == 1 {
		df, err := os.Create(args.String(0))
		if err != nil {
			return err
		}
		defer df.Close()

		data, err := os.Open(file)
		if err != nil {
			return err
		}
		defer data.Close()
		_, err = io.Copy(df, data)
		return err
	}
	return nil
}

func (ts *mockTaskStorage) UploadSegmentTiles(taskID string, timestamp uint64, segmentNum uint32, tiles []string, concurrency int) error {
	ts.Called(taskID, timestamp, segmentNum, tiles, concurrency)
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

func (m Metrics) InOutDurationDiff(diff time.Duration) {}
func (m Metrics) MessageProceed(task *pb.TTranscodeSegmentTask, inputSize uint64, transcoding time.Duration, err error) {
}
func (m Metrics) KeframeMode(mode string) {}

func jsonTaskToProto(taskFile string) (*pb.TTranscodeSegmentTask, string) {
	raw, err := ioutil.ReadFile(taskFile)
	if err != nil {
		panic(err)
	}

	var task pb.TTranscodeSegmentTask
	err = json.Unmarshal(raw, &task)
	if err != nil {
		panic(err)
	}

	return &task, proto.MarshalTextString(&task)
}

func TestMain(m *testing.M) {
	testutil.MustCopyBin("./pack/ffmpeg", ffmpegBin)
	testutil.MustCopyBin("./pack/ffprobe", ffprobeBin)
	testutil.MustCopyBin("./mp4dump", mp4dumpBin)

	os.Exit(m.Run())
}

func TestBrokenTask(t *testing.T) {
	lg := util.MakeLogger()
	err := tcode.Transcode(tcode.TranscodingContext{Lg: lg, Met: &Metrics{}}, "123")
	require.Error(t, err)
	require.False(t, xerrors.Is(err, tcode.ErrBadInput))
}

func TestCustomVideoNotLast(t *testing.T) {
	lg := util.MakeLogger()

	wd, err := os.Getwd()
	require.NoError(t, err)

	task, taskProto := jsonTaskToProto("custom_video_task.json")

	ts := &mockTaskStorage{}
	ts.On("DownloadSegment", task.Id, mock.Anything, task.SegmentNum, mock.Anything).Return("custom_video.mp4").Once()

	outFile := "out-1.mp4"
	ts.On("UploadTranscodedSegment", task.Id, mock.Anything, task.SegmentNum, uint32(0), mock.Anything).Return(outFile).Once()
	ts.On("UploadTranscodedSegment", task.Id, mock.Anything, task.SegmentNum, uint32(1), mock.Anything).Once()
	ts.On("UploadSegmentTiles", task.Id, task.TaskCreatedAt, task.SegmentNum, mock.Anything, mock.Anything).Once()

	dao := &mockDao{}
	dao.On("AddSegment", task.Id, task.SegmentNum, false, uint32(5)).Once()
	dao.On("CountSegments", task.Id, false).Return(1).Once()

	ctx := tcode.TranscodingContext{
		Lg:          lg,
		FfmpegPath:  ffmpegBin,
		FfprobePath: ffprobeBin,
		Mp4DumpPath: mp4dumpBin,
		TaskStorage: ts,
		WorkDir:     wd,
		Dao:         dao,
		Met:         &Metrics{},
	}

	err = tcode.Transcode(ctx, taskProto)
	require.NoError(t, err)

	samples, err := ff.GetSamples(ffprobeBin, outFile)
	require.NoError(t, err)

	require.Equal(t, 240, len(samples))
	require.True(t, samples[48].KeyFrame)
	require.Equal(t, time.Duration(2000)*time.Millisecond, samples[48].BestEffortTimestampTime)

	require.True(t, samples[144].KeyFrame)
	require.Equal(t, time.Duration(6000)*time.Millisecond, samples[144].BestEffortTimestampTime)

	dao.AssertExpectations(t)
	ts.AssertExpectations(t)
}

func TestSuccessVideoTaskNotLast(t *testing.T) {
	lg := util.MakeLogger()

	task, taskProto := jsonTaskToProto("video_task_not_last.json")

	wd, err := os.Getwd()
	require.NoError(t, err)

	ts := &mockTaskStorage{}
	ts.On("DownloadSegment", task.Id, mock.Anything, task.SegmentNum, mock.Anything).Return("video_not_last.mp4").Once()

	outFile := "out-1.mp4"
	ts.On("UploadTranscodedSegment", task.Id, mock.Anything, task.SegmentNum, uint32(240), mock.Anything).Return(outFile).Once()
	ts.On("UploadTranscodedSegment", task.Id, mock.Anything, task.SegmentNum, uint32(360), mock.Anything).Once()
	ts.On("UploadTranscodedSegment", task.Id, mock.Anything, task.SegmentNum, uint32(480), mock.Anything).Once()
	ts.On("UploadSegmentTiles", task.Id, task.TaskCreatedAt, task.SegmentNum, mock.Anything, mock.Anything).Once()

	dao := &mockDao{}
	dao.On("AddSegment", task.Id, task.SegmentNum, false, uint32(1)).Once()
	dao.On("CountSegments", task.Id, false).Return(1).Once()

	ctx := tcode.TranscodingContext{
		Lg:          lg,
		FfmpegPath:  ffmpegBin,
		FfprobePath: ffprobeBin,
		Mp4DumpPath: mp4dumpBin,
		TaskStorage: ts,
		WorkDir:     wd,
		Dao:         dao,
		Met:         &Metrics{},
	}

	err = tcode.Transcode(ctx, taskProto)
	require.NoError(t, err)

	samples, err := ff.GetSamples(ffprobeBin, outFile)
	require.NoError(t, err)

	require.Equal(t, 315, len(samples))
	require.True(t, samples[99].KeyFrame)
	require.Equal(t, time.Duration(3303)*time.Millisecond, samples[99].BestEffortTimestampTime)

	require.True(t, samples[219].KeyFrame)
	require.Equal(t, time.Duration(7307)*time.Millisecond, samples[219].BestEffortTimestampTime)

	dao.AssertExpectations(t)
	ts.AssertExpectations(t)
}

// Broken segment, ffmpeg fails, we push error to combiner.
func TestBrokenVideoSegment(t *testing.T) {
	lg := util.MakeLogger()

	task, taskProto := jsonTaskToProto("video_task_not_last.json")

	wd, err := os.Getwd()
	require.NoError(t, err)

	ts := &mockTaskStorage{}
	ts.On("DownloadSegment", task.Id, mock.Anything, task.SegmentNum, mock.Anything).Return("in-254.mp4").Once()

	dao := &mockDao{}
	dao.On("UpdateTaskSequentialTiles", task.Id, true).Once()

	errorMsg := proto.MarshalTextString(&pb.TTranscodeSegmentDone{
		Id:         task.Id,
		SegmentNum: task.SegmentNum,
		Error:      tcode.ErrBadInput.Error(),
	})

	doneQueue := &mockOutput{}
	doneQueue.On("Push", mock.MatchedBy(func(m string) bool { return errorMsg == m })).Once()

	ctx := tcode.TranscodingContext{
		Lg:          lg,
		FfmpegPath:  ffmpegBin,
		FfprobePath: ffprobeBin,
		Mp4DumpPath: mp4dumpBin,
		Dao:         dao,
		TaskStorage: ts,
		WorkDir:     wd,
		OutQueue:    doneQueue,
		Met:         &Metrics{},
	}

	err = tcode.Transcode(ctx, taskProto)
	require.Error(t, err)
	require.True(t, xerrors.Is(err, tcode.ErrBadInput))

	ts.AssertExpectations(t)
	doneQueue.AssertExpectations(t)
}

func TestBrokenAudioSegment(t *testing.T) {
	lg := util.MakeLogger()

	task, taskProto := jsonTaskToProto("audio_task.json")

	wd, err := os.Getwd()
	require.NoError(t, err)

	ts := &mockTaskStorage{}
	ts.On("DownloadSegment", task.Id, mock.Anything, task.SegmentNum, mock.Anything).Return("in-0.mp4").Once()

	errorMsg := proto.MarshalTextString(&pb.TTranscodeSegmentDone{
		Id:         task.Id,
		SegmentNum: 0,
		Error:      tcode.ErrBadInput.Error(),
	})

	doneQueue := &mockOutput{}
	doneQueue.On("Push", mock.MatchedBy(func(m string) bool { return errorMsg == m })).Once()

	ctx := tcode.TranscodingContext{
		Lg:          lg,
		FfmpegPath:  ffmpegBin,
		FfprobePath: ffprobeBin,
		Mp4DumpPath: mp4dumpBin,
		TaskStorage: ts,
		WorkDir:     wd,
		OutQueue:    doneQueue,
		Met:         &Metrics{},
	}

	err = tcode.Transcode(ctx, taskProto)
	require.Error(t, err)
	require.True(t, xerrors.Is(err, tcode.ErrBadInput))

	ts.AssertExpectations(t)
	doneQueue.AssertExpectations(t)
}

func TestFailOnLargeDurationDiff(t *testing.T) {
	lg := util.MakeLogger()

	task, taskProto := jsonTaskToProto("video_task_not_last.json")

	wd, err := os.Getwd()
	require.NoError(t, err)

	ts := &mockTaskStorage{}
	ts.On("DownloadSegment", task.Id, mock.Anything, task.SegmentNum, mock.Anything).Return("large-dur-diff.mp4").Once()
	ts.On("UploadSegmentTiles", task.Id, task.TaskCreatedAt, task.SegmentNum, mock.Anything, mock.Anything).Once()

	dao := &mockDao{}
	dao.On("UpdateTaskSequentialTiles", task.Id, true).Once()

	doneQueue := &mockOutput{}
	doneQueue.On("Push", mock.Anything).Once()

	ctx := tcode.TranscodingContext{
		Lg:          lg,
		FfmpegPath:  ffmpegBin,
		FfprobePath: ffprobeBin,
		Mp4DumpPath: mp4dumpBin,
		Dao:         dao,
		TaskStorage: ts,
		WorkDir:     wd,
		OutQueue:    doneQueue,
		Met:         &Metrics{},
	}

	err = tcode.Transcode(ctx, taskProto)
	require.Error(t, err)
	require.True(t, xerrors.Is(err, tcode.ErrBadInput))

	ts.AssertExpectations(t)
	doneQueue.AssertExpectations(t)
}
