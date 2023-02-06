import time
import threading
import traceback


def run_multithreaded(target, thread_count, duration=None, final_target=None):
    proceed = threading.Event()
    proceed.set()

    def target_caller():
        target(proceed)

    def create_and_start_thread():
        t = threading.Thread(target=target_caller)
        t.start()
        return t

    threads = [create_and_start_thread() for _ in range(thread_count)]

    if duration:
        def stop_proceed():
            print 'Stopping after %d seconds...' % duration
            proceed.clear()

        stop_by_timer_thread = threading.Timer(duration, stop_proceed)
        stop_by_timer_thread.start()
    else:
        stop_by_timer_thread = None

    try:
        while any(t.is_alive() for t in threads):
            time.sleep(.1)
    except KeyboardInterrupt:
        pass # ignore interruption by keyboard
    finally:
        try:
            proceed.clear()
            if stop_by_timer_thread:
                stop_by_timer_thread.cancel()
            for t in threads:
                t.join()

            if final_target:
                final_target()
        except Exception, e:
            traceback.print_exc()
            pass
