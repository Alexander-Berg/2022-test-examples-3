package ff_test

import (
	"io/ioutil"
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/ff"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/testutil"
)

const ffprobeBin = "./ffprobe_bin"
const mp4dumpBin = "./mp4dump_bin"

func TestMain(m *testing.M) {
	testutil.MustCopyBin("./ffprobe", ffprobeBin)
	testutil.MustCopyBin("./mp4dump", mp4dumpBin)

	os.Exit(m.Run())
}

func TestNotVideo(t *testing.T) {
	someFile, err := ioutil.TempFile(".", "input")
	require.NoError(t, err)
	defer func() {
		if err := os.Remove(someFile.Name()); err != nil {
			panic(err)
		}
	}()

	_, err = someFile.Write([]byte("qwerty"))
	require.NoError(t, err)

	_, _, err = ff.GetVideoFormat(ffprobeBin, someFile.Name())
	require.Error(t, err)
}

// deleted resource by ttl
//func TestVideo(t *testing.T) {
//	info, streams, err := ff.GetVideoFormat(ffprobeBin, "./in-5.mp4")
//	require.NoError(t, err)
//
//	require.NotNil(t, info)
//
//	require.Equal(t, "mov,mp4,m4a,3gp,3g2,mj2", info.Name)
//	require.Equal(t, uint32(1), info.NbStreams)
//	require.Equal(t, float64(40.500000), info.StartTime)
//	require.Equal(t, time.Duration(10.82*1000)*time.Millisecond, info.Duration)
//	require.Equal(t, uint64(5334228), info.Size)
//	require.Equal(t, uint64(3943976), info.Bitrate)
//
//	require.Equal(t, 1, len(streams))
//
//	s := streams[0]
//	require.Equal(t, "video", s.CodecType)
//	require.Equal(t, uint32(1920), s.Width)
//	require.Equal(t, uint32(1080), s.Height)
//	require.Equal(t, uint64(138496), s.DurationTS)
//	require.Equal(t, uint32(541), *s.NbFrames)
//	require.Equal(t, time.Duration(10.82*1000)*time.Millisecond, *s.Duration)
//	require.Equal(t, uint32(1), s.TimeBase.Num)
//	require.Equal(t, uint32(12800), s.TimeBase.Den)
//}

func TestVerticalVideoWithRotation(t *testing.T) {
	info, streams, err := ff.GetVideoFormat(ffprobeBin, "./vertical.mp4")
	require.NoError(t, err)

	require.NotNil(t, info)
	require.NotNil(t, streams)

	strm := streams[0]
	require.Equal(t, "video", strm.CodecType)
	require.Equal(t, uint32(480), strm.Width)
	require.Equal(t, uint32(848), strm.Height)
}

// deleted resource by ttl
//func TestStts(t *testing.T) {
//	stss, err := ff.GetMovStts(mp4dumpBin, "./in-5.mp4")
//	require.NoError(t, err)
//
//	require.Equal(t, 1, len(stss))
//	require.Equal(t, uint32(541), stss[0].Samples)
//	require.Equal(t, uint32(256), stss[0].DurationTS)
//}

// deleted resource by ttl
//func TestGetSamples(t *testing.T) {
//	frames, err := ff.GetSamples(ffprobeBin, "./in-5.mp4")
//	require.NoError(t, err)
//
//	require.Equal(t, 541, len(frames))
//
//	f := frames[100]
//	require.Equal(t, "video", f.MediaType)
//	require.Equal(t, uint32(0), f.StreamIndex)
//	require.Equal(t, false, f.KeyFrame)
//	require.Equal(t, uint32(544000), f.BestEffortTimestamp)
//	require.Equal(t, time.Duration(42.5*1000)*time.Millisecond, f.BestEffortTimestampTime)
//	require.Equal(t, uint32(256), f.PktDuration)
//	require.Equal(t, uint32(1920), f.Width)
//	require.Equal(t, uint32(1080), f.Height)
//	require.Equal(t, "P", f.PictType)
//}

func TestWebm(t *testing.T) {
	info, streams, err := ff.GetVideoFormat(ffprobeBin, "./in.webm")
	require.NoError(t, err)

	require.NotNil(t, info)

	require.Equal(t, "matroska,webm", info.Name)
	require.Equal(t, uint32(2), info.NbStreams)
	require.Equal(t, time.Duration((31*60+47.281)*1000)*time.Millisecond, info.Duration)
	require.Equal(t, uint64(316975300), info.Size)
	require.Equal(t, uint64(1329537), info.Bitrate)

	require.Equal(t, 2, len(streams))

	s := streams[0]
	require.Equal(t, "video", s.CodecType)
	require.Equal(t, uint32(1280), s.Width)
	require.Equal(t, uint32(720), s.Height)
	require.Equal(t, uint64(0), s.DurationTS)
	require.Nil(t, s.NbFrames)
	require.Nil(t, s.Duration)
	require.Equal(t, uint32(1), s.TimeBase.Num)
	require.Equal(t, uint32(1000), s.TimeBase.Den)

	s = streams[1]
	require.Equal(t, "audio", s.CodecType)
}
