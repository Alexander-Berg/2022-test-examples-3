DROP TABLE IF EXISTS riverbank_testing.gogol;
CREATE TABLE riverbank_testing.gogol (
  date Date,
  timestamp UInt32,
  additionalParameters_reqid String,
  additionalParameters_from_block String,
  additionalParameters_stream_block String,
  additionalParameters_ppi String,
  adsid String,
  clientTimestamp UInt64,
  data_category String,
  data_details String,
  data_details_details_0 String,
  data_details_details_frag_url String,
  data_duration Float64,
  data_isFullscreen UInt8,
  data_message String,
  data_reportData String,
  data_reported String,
  data_reportId String,
  data_sandboxJSAPI UInt8,
  data_sandboxVersion String,
  data_stalledDuration Float64,
  data_stack String,
  data_state_ad String,
  data_state_audioBitrate Float64,
  data_state_audioLang String,
  data_state_audioTrack String,
  data_state_auto String,
  data_state_bandwidthEstimate Float64,
  data_state_bitrate Float64,
  data_state_capHeight Int32,
  data_state_currentTime Float64,
  data_state_droppedFrames UInt32,
  data_state_height Int32,
  data_state_isMuted String,
  data_state_maxHeight Int32,
  data_state_remainingBufferedTime Float64,
  data_state_remainingAudioBufferedTime Float64,
  data_state_remainingVideoBufferedTime Float64,
  data_state_rtt Float64,
  data_state_shownFrames UInt32,
  data_state_stalledCount UInt32,
  data_state_stalledTime Float64,
  data_state_state String,
  data_state_watchedTime Float64,
  data_time Float64,
  data_utcTime Float64,
  data_videoType String,
  data_watchedSec Float64,
  device_id String,
  errorId String,
  eventIndex UInt32,
  eventName String,
  eventType String,
  host String,
  labels_from String,
  labels_reason String,
  location String,
  puid String,
  referrer String,
  serverTimestamp UInt64,
  service String,
  streamArg_imp_id String,
  streamArg_partner_id String,
  streamArg_video_category_id String,
  streamBucket String,
  streamHost String,
  streamPath String,
  streamPrj String,
  streamUrl String,
  topLocation String,
  topReferrer String,
  userAgent String,
  userAgent_browser_name String,
  userAgent_browser_version_major String,
  userAgent_device_name String,
  userAgent_device_vendor String,
  userAgent_is_mobile UInt8,
  userAgent_is_robot UInt8,
  userAgent_is_tablet UInt8,
  userAgent_is_touch UInt8,
  userAgent_is_tv UInt8,
  userAgent_os_family String,
  userAgent_os_name String,
  userAgent_os_version String,
  version String,
  videoContentId String,
  vsid String,
  xForwardedFor String,
  xRealIp String,
  xRequestId String,
  xYandexExpboxes String,
  yandexuid String,
  streamType String,
  testIds Array(UInt32),
  data_state_isVisible UInt8,
  data_state_stalledReason String,
  data_state_width Int32
) ENGINE = Log();