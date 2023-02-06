package queue_test

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/aws/aws-sdk-go/service/sqs"
	"github.com/aws/aws-sdk-go/service/sqs/sqsiface"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/queue"
	"a.yandex-team.ru/extsearch/video/robot/rt_transcoder/ng/util"
)

type mockSqs struct {
	mock.Mock
	sqsiface.SQSAPI
}

func (s *mockSqs) ReceiveMessage(mi *sqs.ReceiveMessageInput) (*sqs.ReceiveMessageOutput, error) {
	args := s.Called(mi)

	msg := args.String(0)
	res := &sqs.ReceiveMessageOutput{
		Messages: []*sqs.Message{},
	}

	if len(msg) > 0 {
		receipt := args.String(1)
		sentAt := args.String(2)
		res.Messages = append(res.Messages, &sqs.Message{
			Body:          &msg,
			ReceiptHandle: &receipt,
			Attributes: map[string]*string{
				"SentTimestamp": &sentAt,
			},
		})
	}

	return res, nil
}

func (s *mockSqs) DeleteMessage(mi *sqs.DeleteMessageInput) (*sqs.DeleteMessageOutput, error) {
	s.Called(mi)
	return nil, nil
}

func (s *mockSqs) ChangeMessageVisibility(mi *sqs.ChangeMessageVisibilityInput) (*sqs.ChangeMessageVisibilityOutput, error) {
	s.Called(mi)
	return nil, nil
}

// TestPullApprove tests simple case: pull-approve for two messages
func TestPullApprove(t *testing.T) {
	lg := util.MakeLogger()
	s := &mockSqs{}

	s.On("ReceiveMessage", mock.Anything).Return("msg1", "rec1", "1576596805000").Once()
	s.On("DeleteMessage", mock.MatchedBy(func(mi *sqs.DeleteMessageInput) bool { return *mi.ReceiptHandle == "rec1" })).Once()

	s.On("ReceiveMessage", mock.Anything).Return("msg2", "rec2", "1576596805000").Once()
	s.On("DeleteMessage", mock.MatchedBy(func(mi *sqs.DeleteMessageInput) bool { return *mi.ReceiptHandle == "rec2" })).Once()

	s.On("ReceiveMessage", mock.Anything).Return("", "", "0").Once()
	input, err := queue.NewSqsInputForSvc("queue_url", s, lg, 20)
	require.NoError(t, err)

	for i := 1; i <= 2; i++ {
		msgs, err := input.Pull()
		require.NoError(t, err)
		require.EqualValues(t, 1, len(msgs))
		require.EqualValues(t, fmt.Sprintf("msg%d", i), msgs[0].Body)

		require.EqualValues(t, int64(1576596805), msgs[0].SentAt.Unix())
		err = input.Approve(msgs[0])
		require.NoError(t, err)
	}
	msgs, err := input.Pull()
	require.NoError(t, err)
	require.EqualValues(t, 0, len(msgs))

	s.AssertExpectations(t)
}

// TestHeartbeat checks heartbeats called enough times for 2 messages in queue
func TestHeartbeat(t *testing.T) {
	lg := util.MakeLogger()

	s := &mockSqs{}
	// 1st message
	s.On("ReceiveMessage", mock.Anything).Return("msg1", "rec1", "0").Once()
	s.On("ChangeMessageVisibility", mock.MatchedBy(func(mi *sqs.ChangeMessageVisibilityInput) bool {
		return *mi.ReceiptHandle == "rec1" && *mi.VisibilityTimeout == 2
	})).Times(3)
	s.On("DeleteMessage", mock.MatchedBy(func(mi *sqs.DeleteMessageInput) bool { return *mi.ReceiptHandle == "rec1" })).Once()

	// 2nd message
	s.On("ReceiveMessage", mock.Anything).Return("msg2", "rec2", "0").Once()
	s.On("ChangeMessageVisibility", mock.MatchedBy(func(mi *sqs.ChangeMessageVisibilityInput) bool {
		return *mi.ReceiptHandle == "rec2" && *mi.VisibilityTimeout == 2
	})).Times(3)
	s.On("DeleteMessage", mock.MatchedBy(func(mi *sqs.DeleteMessageInput) bool { return *mi.ReceiptHandle == "rec2" })).Once()

	// prepare
	input, err := queue.NewSqsInputForSvc("queue_url", s, lg, 20)
	require.NoError(t, err)

	ctx, stop := context.WithCancel(context.Background())
	input.RunHeartbeater(ctx, time.Second)

	// run
	for i := 1; i <= 2; i++ {
		msgs, _ := input.Pull()

		time.Sleep(2100 * time.Millisecond)

		err = input.Approve(msgs[0])
		require.NoError(t, err)
	}
	stop()

	s.AssertExpectations(t)
}

// TestHeartbeatNoApprove checks if we stop heartbeats when we did not approve message we proceed
func TestHeartbeatNoApprove(t *testing.T) {
	lg := util.MakeLogger()

	s := &mockSqs{}
	s.On("ReceiveMessage", mock.Anything).Return("msg1", "rec1", "0").Once()
	s.On("ChangeMessageVisibility", mock.MatchedBy(func(mi *sqs.ChangeMessageVisibilityInput) bool {
		return *mi.ReceiptHandle == "rec1" && *mi.VisibilityTimeout == 2
	})).Times(3)

	s.On("ReceiveMessage", mock.Anything).Return("", "", "0").Times(2)

	// prepare
	input, err := queue.NewSqsInputForSvc("queue_url", s, lg, 20)
	require.NoError(t, err)

	ctx, stop := context.WithCancel(context.Background())
	input.RunHeartbeater(ctx, time.Second)

	// run
	for i := 0; i < 3; i++ {
		_, err = input.Pull()
		require.NoError(t, err)

		time.Sleep(2100 * time.Millisecond)
	}
	stop()

	s.AssertExpectations(t)
}
