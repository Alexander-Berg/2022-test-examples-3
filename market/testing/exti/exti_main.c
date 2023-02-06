/****************************************************************************
 * Included Files
 ****************************************************************************/

#include <nuttx/config.h>

#include <sys/ioctl.h>
#include <stdbool.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <signal.h>
#include <errno.h>

#include <nuttx/ioexpander/gpio.h>

static uint64_t oldTime;
static bool is_signal_received = false;

/****************************************************************************
 * Private Functions
 ****************************************************************************/

static void exti_handler(int signo, FAR siginfo_t *info, FAR void *ucontext)
{
  if((g_system_timer - oldTime) > 15 ) 
  {
    oldTime = g_system_timer;
    printf("Signal received from the driver\n");
    is_signal_received = true;
  }
}

/****************************************************************************
 * Public Functions
 ****************************************************************************/

/****************************************************************************
 * test_exti_main
 ****************************************************************************/

int test_exti_main(int argc, FAR char *argv[])
{
    enum gpio_pintype_e pintype;
    bool invalue;
    int ret;

    /* Open the pin driver */
    int fd = open("/dev/gpint0", O_RDWR);
    if (fd < 0)
    {
        int errcode = errno;
        printf("ERROR: Failed to open: %d\n", errcode);
        return EXIT_FAILURE;
    }

    /* Get the pin type */
    ret = ioctl(fd, GPIOC_PINTYPE, &pintype);
    if (ret < 0) {
        int errcode = errno;
        printf("ERROR: Failed to read pintype: %d\n", errcode);
        close(fd);
        return EXIT_FAILURE;
    }

    if (pintype != GPIO_INTERRUPT_PIN) {
        printf("ERROR: Wrong pintype\n");
        close(fd);
        return EXIT_FAILURE;
    }

    /* Read the pin value */
    ret = ioctl(fd, GPIOC_READ, &invalue);
    if (ret < 0) {
        int errcode = errno;
        printf("ERROR: Failed to read value: %d\n", errcode);
        close(fd);
        return EXIT_FAILURE;
    }
    printf("Pin: Type=%u Value=%u\n", (unsigned int)pintype, invalue);

    /* Register signal handler */
    struct sigaction act;
    memset(&act, 0, sizeof(act));
    struct sigaction oact;
    act.sa_sigaction = exti_handler;
    act.sa_flags = SA_SIGINFO;
    sigemptyset(&act.sa_mask);
    sigfillset(&act.sa_mask);
    sigdelset(&act.sa_mask, SIGUSR1);
    ret = sigaction(SIGUSR1, &act, &oact);
    if (ret < 0) {
        int errcode = errno;
        printf("ERROR: sigaction failed: %d\n", errcode);
        close(fd);
        return EXIT_FAILURE;
    }

    /* Set up to receive signal */
    struct sigevent ev1;
    ev1.sigev_notify = SIGEV_SIGNAL;
    ev1.sigev_signo  = SIGUSR1;
    ev1.sigev_value.sival_int = 1;
    ret = ioctl(fd, GPIOC_REGISTER, &ev1);
    if (ret < 0) {
        int errcode = errno;
        printf("ERROR: Failed to setup for signal: %d\n", errcode);
        close(fd);
        return EXIT_FAILURE;
    }

    uint64_t start_time = g_system_timer;
    while (g_system_timer - start_time < 30000) {
        if (is_signal_received == true) {
            ret = ioctl(fd, GPIOC_READ, &invalue);
            if (ret < 0) {
                int errcode = errno;
                printf("ERROR: Failed to re-read value: %d\n", errcode);
                close(fd);
                return EXIT_FAILURE;
            }

            printf(" Value=%u\n", (unsigned int)invalue);
            is_signal_received = false;
        }
        usleep(500);
    }
    
    ioctl(fd, GPIOC_UNREGISTER);
    close(fd);
    return EXIT_SUCCESS;
}
