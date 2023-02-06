#include <rcl/rcl.h>
#include <rcl/error_handling.h>
#include <rclc/rclc.h>
#include <rclc/executor.h>

#include <sensor_msgs/msg/imu.h>
#include <std_msgs/msg/header.h>

#include <stdio.h>
#include <unistd.h>
#include <time.h>

#define RCCHECK(fn)                                                                      \
	{                                                                                    \
		rcl_ret_t temp_rc = fn;                                                          \
		if ((temp_rc != RCL_RET_OK))                                                     \
		{                                                                                \
			printf("Failed status on line %d: %d. Aborting.\n", __LINE__, (int)temp_rc); \
			return 1;                                                                    \
		}                                                                                \
	}
#define RCSOFTCHECK(fn)                                                                    \
	{                                                                                      \
		rcl_ret_t temp_rc = fn;                                                            \
		if ((temp_rc != RCL_RET_OK))                                                       \
		{                                                                                  \
			printf("Failed status on line %d: %d. Continuing.\n", __LINE__, (int)temp_rc); \
		}                                                                                  \
	}

#define STRING_BUFFER_LEN 100

rcl_publisher_t payload_publisher;
rcl_subscription_t feedback_subscriber;

sensor_msgs__msg__Imu payload = {
	.orientation = {0.2, 0.3, 0.1, 0.4},
	.orientation_covariance = {0.1},
	.angular_velocity = {0.1, 0.2, 0.3},
	.angular_velocity_covariance = {0.2},
	.linear_acceleration = {1, 2, 3},
	.linear_acceleration_covariance = {0.3}};
std_msgs__msg__Header feedback;

int device_id;
int seq_no;
float RTT_alpha = 0.f;
struct timespec RTT_old = {0, 0};

static inline void timespec_diff(struct timespec *a, struct timespec *b,
								 struct timespec *result)
{
	result->tv_sec = a->tv_sec - b->tv_sec;
	result->tv_nsec = a->tv_nsec - b->tv_nsec;
	if (result->tv_nsec < 0)
	{
		--result->tv_sec;
		result->tv_nsec += 1000000000L;
	}
}

static inline void timespec_ab(struct timespec *old, struct timespec *new,
							   struct timespec *result)
{
	result->tv_nsec = RTT_alpha * old->tv_nsec + (1 - RTT_alpha) * new->tv_nsec;
	result->tv_sec = RTT_alpha * old->tv_sec + (1 - RTT_alpha) * new->tv_sec;
}

void timer_callback(rcl_timer_t *timer, int64_t last_call_time)
{
	(void)last_call_time;

	if (timer != NULL)
	{
		seq_no = rand();
		sprintf(payload.header.frame_id.data, "%d_%d", seq_no, device_id);
		payload.header.frame_id.size = strlen(payload.header.frame_id.data);

		struct timespec ts;
		clock_gettime(CLOCK_REALTIME, &ts);
		payload.header.stamp.sec = ts.tv_sec;
		payload.header.stamp.nanosec = ts.tv_nsec;

		RCSOFTCHECK(rcl_publish(&payload_publisher, (const void *)&payload, NULL));
	}
}

void feedback_callback(const void *msgin)
{
	const std_msgs__msg__Header *msg = (const std_msgs__msg__Header *)msgin;

	struct timespec a, b, res;
	clock_gettime(CLOCK_REALTIME, &a);
	b.tv_sec = msg->stamp.sec;
	b.tv_nsec = msg->stamp.nanosec;
	timespec_diff(&a, &b, &res);
	timespec_ab(&RTT_old, &res, &res);

	RTT_old = res;
	printf("RTT: %lld.%.9ld\n", (long long)res.tv_sec, res.tv_nsec);
}

int main(int argc, char *argv[])
{
	int opt;
	uint32_t delay = 1000;
	while ((opt = getopt(argc, argv, "a::t::")) != -1)
	{
		switch (opt)
		{
		case 'a':
			RTT_alpha = atof(optarg);
			break;
		case 't':
			delay = atoi(optarg);
			break;
		default:
			fprintf(stderr, "Usage: %s -a [rtt_alpha] -t [msg_period_ms]\n", argv[0]);
			abort();
		}
	}

	rcl_allocator_t allocator = rcl_get_default_allocator();
	rclc_support_t support;

	RCCHECK(rclc_support_init(&support, 0, NULL, &allocator));
	rcl_node_t node;
	RCCHECK(rclc_node_init_default(&node, "perf_test_node", "", &support));
	RCCHECK(rclc_publisher_init_default(&payload_publisher, &node, ROSIDL_GET_MSG_TYPE_SUPPORT(sensor_msgs, msg, Imu), "/microROS/data"));
	RCCHECK(rclc_subscription_init_default(&feedback_subscriber, &node, ROSIDL_GET_MSG_TYPE_SUPPORT(std_msgs, msg, Header), "/microROS/feedback"));

	rcl_timer_t timer = rcl_get_zero_initialized_timer();
	RCCHECK(rclc_timer_init_default(&timer, &support, RCL_MS_TO_NS(delay), timer_callback));

	rclc_executor_t executor = rclc_executor_get_zero_initialized_executor();
	RCCHECK(rclc_executor_init(&executor, &support.context, 2, &allocator));

	unsigned int rcl_wait_timeout = 1000; // in ms
	RCCHECK(rclc_executor_set_timeout(&executor, RCL_MS_TO_NS(rcl_wait_timeout)));
	RCCHECK(rclc_executor_add_timer(&executor, &timer));
	RCCHECK(rclc_executor_add_subscription(&executor, &feedback_subscriber, &feedback, &feedback_callback, ON_NEW_DATA));

	device_id = 42;

	char outcoming_buffer[STRING_BUFFER_LEN];
	payload.header.frame_id.data = outcoming_buffer;
	payload.header.frame_id.capacity = STRING_BUFFER_LEN;

	char incoming_buffer[STRING_BUFFER_LEN];
	feedback.frame_id.data = incoming_buffer;
	feedback.frame_id.capacity = STRING_BUFFER_LEN;

	rclc_executor_spin(&executor);

	RCCHECK(rcl_publisher_fini(&payload_publisher, &node));
	RCCHECK(rcl_subscription_fini(&feedback_subscriber, &node));
	RCCHECK(rcl_node_fini(&node));
	return 0;
}