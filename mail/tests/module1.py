from ymod_python_sys import deadline_timer, terminate_application, log, info
import sys


num = 0


def cb1(ident):
    global num
    if num < 5:
        num = num + 1
        print(["time", ident, "s", 42, "garbage", [123, "345", 123]])
        deadline_timer(ident, 100, cb1)


a = (1,2,3)
deadline_timer(a, 10, cb1)
deadline_timer(None, 1500, lambda x: terminate_application())
del a
log(info, "Test info")
