import argparse
import datetime
import math
import os
import random
import sys
import time
from datetime import datetime
from os import listdir
from os.path import isfile, join
from threading import Thread
from typing import List

import uiautomator2 as u2
from adb.client import Client as AdbClient

DEFAULT_APP = 'ru.yandex.mail'
class Device:
    def __init__(self, device_id: str) -> None:
        client = AdbClient(host="127.0.0.1", port=5037)
        print(client.version())
        self.adb_device = client.device(device_id)


        self.ui_device = u2.connect(device_id)  # alias for u2.connect_wifi('10.0.0.1')
        print(self.ui_device.info)

        if self.adb_device is None or self.ui_device is None:
            raise Exception('cant connect to device %s' % device_id)

        self.dh = self.ui_device.info['displayHeight']
        self.dw = self.ui_device.info['displayWidth']

        lines = [x for x in (self.shell('getevent -il /dev/input/event0') + self.shell('getevent -il /dev/input/event1')).split('\n') if 'ABS_MT_POSITION' in x]

        if len(lines) != 2:
            raise Exception('cant get max_mt_pos')

        self.max_mt_pos_x = int([x for x in lines if 'ABS_MT_POSITION_X' in x][0].split(',')[2].strip().split(' ')[1])
        self.max_mt_pos_y = int([x for x in lines if 'ABS_MT_POSITION_Y' in x][0].split(',')[2].strip().split(' ')[1])

    def shell(self, cmd, handler=None, timeout=None):
        return self.adb_device.shell(cmd, handler, timeout)

    def pull(self, src, dst):
        return self.adb_device.pull(src, dst)

    def rm(self, src):
        return self.adb_device.shell('rm {}'.format(src))

    def click(self, x: int, y: int):
        self.ui_device.click(x, y)

    def swipe(self, x1, y1, x2, y2, duration=0.5):
        self.ui_device.swipe(x1, y1, x2, y2, duration)

    def back(self):
        self.shell('input keyevent 4')

    def get_active_windows(self):
        return self.shell('dumpsys window windows')

    def was_crashed(self, old_pid, app=DEFAULT_APP):
        return int(old_pid) != int(self.adb_device.get_pid(app))
        # try:
        #     if len(self.shell('ps -x | grep {}'.format(app))) > 0:
        #     # if int(self.shell('pidof {}'.format(app))) > 0:
        #         return False
        # except Exception as e:
        #     pass
        # return True

    def is_focused(self, app=DEFAULT_APP):
        # return self.ui_device.info['currentPackageName'] == app
        top = self.adb_device.get_top_activity()
        attempts = 10
        while top is None and attempts > 0:
            time.sleep(1)
            attempts = attempts - 1
            print('waiting for top activity ...')
            top = self.adb_device.get_top_activity()
            print(top)
        if top:
            return top.package == app
        else:
            return sum(1 for y in [x for x in self.get_active_windows().split('\n') if 'mCurrentFocus' in x or 'mFocusedApp' in x]if app in y) > 0

    def restart_app(self, app=DEFAULT_APP):
        self.shell('am force-stop {}'.format(app))
        self.shell('monkey -p {} 1'.format(app))  # TODO use am start§
        while self.adb_device.get_top_activity() is None or self.adb_device.get_top_activity().package != app:
            print('waiting for %s' % app)
            time.sleep(1)
        return self.adb_device.get_top_activity().pid

    def run_monkey(self, c, app=DEFAULT_APP):
        self.shell('monkey -p {} c'.format(app, int(c)))

    def transform_coordinates(self, x, y):
        if self.dw == self.max_mt_pos_x + 1:
            return min(x, self.max_mt_pos_x), min(y, self.max_mt_pos_y)
        return int(self.dw * (float(x) / (self.max_mt_pos_x + 1))), int(self.dh * (float(y) / (self.max_mt_pos_y + 1)))

    def is_forbidden_activity(self, activity):
        return self.adb_device.get_top_activity().activity\
               == activity

    def start_record(self, name):
        return datetime.now(), ScreenRecorder(device=self, name=name)

    def stop_record(self, pid):
        self.shell("kill -SIGINT {}".format(pid))


class ScreenRecorder(Thread):
    def __init__(self, device):
        Thread.__init__(self)
        self.device = device
        self.records = []
        self.stopped = False

    def run(self):
        now_str = datetime.strftime(datetime.now(), "%Y_%m_%d_%H_%M_%S")
        part = 0
        while not self.stopped:
            self.records.append('/sdcard/{}_{}.mp4'.format(now_str, part))
            part = part + 1
            self.device.shell('screenrecord --bugreport --time-limit 60 {}'.format(self.records[-1]))

        print('stopped')

    def stop(self):
        self.stopped = True
        pid_line = self.device.shell('ps | grep screenrecord')
        pid = pid_line.split()[1]
        self.device.shell('kill -9 {}'.format(pid))

class Recorder(Thread):
    def __init__(self, device, output):
        Thread.__init__(self)
        self.device = device
        self.running = True
        self.output = open(output, 'w')
        self.connection = None

    def run(self):
        def dump_eventlog(connection):
            self.connection = connection
            while True:
                data = connection.read(1024)
                if not data:
                    break
                self.output.write(data.decode('utf-8'))
            if connection is not None:
                connection.close()
            self.output.close()
        self.device.shell('getevent -lt', dump_eventlog)

    def stop(self):
        self.running = False
        if self.connection is not None:
            self.connection.close()

# class EventType(Enum):
#   TAP = 1
#   SWIPE = 2

# herolte:/ $ getevent -il /dev/input/event1
# add device 1: /dev/input/event1
#   bus:      0018
#   vendor    0000
#   product   0000
#   version   0000
#   name:     "sec_touchscreen"
#   location: "sec_touchscreen/input1"
#   id:       ""
#   version:  1.0.1
#   events:
#     KEY (0001): BTN_TOOL_FINGER       BTN_TOUCH             01c6                  01c7
#                 01ca                  01cb
#     ABS (0003): ABS_MT_SLOT           : value 0, min 0, max 9, fuzz 0, flat 0, resolution 0
#                 ABS_MT_TOUCH_MAJOR    : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
#                 ABS_MT_TOUCH_MINOR    : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
#                 ABS_MT_POSITION_X     : value 0, min 0, max 4095, fuzz 0, flat 0, resolution 0
#                 ABS_MT_POSITION_Y     : value 0, min 0, max 4095, fuzz 0, flat 0, resolution 0
#                 ABS_MT_TRACKING_ID    : value 0, min 0, max 65535, fuzz 0, flat 0, resolution 0
#                 ABS_MT_DISTANCE       : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
#                 ABS_MT_PALM           : value 0, min 0, max 1, fuzz 0, flat 0, resolution 0
#   input props:
#     INPUT_PROP_DIRECT

# herolte:/ $ wm size
# Physical size: 1440x2560
# Override size: 1080x1920


class Event:
    def __init__(self, ts: float = -1) -> None:
        self.ts = ts

    def send(self, device):
        pass


class Tap(Event):
    def __init__(self, x: int, y: int, ts: float = -1) -> None:
        super().__init__(ts)
        self.x = x
        self.y = y

    def send(self, device):
        x, y = device.transform_coordinates(self.x, self.y)
        cmd = 'input tap {} {}'.format(x, y)
        print('Execution %s' % cmd)
        # # device.Shell(cmd)
        # device.shell(cmd)
        device.click(x, y)

    def __str__(self):
        return 'Tap({}, {}, at:{})'.format(self.x, self.y, self.ts)


class Swipe(Event):
    def __init__(self, x1: int, y1: int, x2: int, y2: int, duration: float, ts: float = -1) -> None:
        super().__init__(ts)
        self.x1 = x1
        self.y1 = y1
        self.x2 = x2
        self.y2 = y2
        self.duration = duration

    def send(self, device):
        x1, y1 = device.transform_coordinates(self.x1, self.y1)
        x2, y2 = device.transform_coordinates(self.x2, self.y2)

        cmd = 'input swipe {} {} {} {} {}'.format(x1, y1, x2, y2, int(self.duration * 1000))
        print('Execution %s' % cmd)
        # device.Shell(cmd)
        device.swipe(x1, y1, x2, y2, self.duration)

    def __str__(self):
        return 'Swipe(({},{}) -> ({},{}), d:{}, at:{})'.format(self.x1, self.y1, self.x2, self.y2, self.duration, self.ts)


class UnknownEvent(Event):
    def __init__(self):
        super().__init__()
        pass

    def __str__(self):
        return 'UnknownEvent'


class Atom(object):
    def __init__(self, ts: float, device: str, key: str, action: str, data: str):
        self.ts = ts
        self.key = key
        self.action = action
        self.data = data
        self.device = device

    @staticmethod
    def from_string(s):
        ts, device, key, action, data = [x for x in [x.replace('[', '').replace(']', '') for x in s.split()] if x]
        return Atom(float(ts), device, key, action, data)


class Tokenizer(object):
    def tokenize(self, events: List[Atom]) -> List[List[Atom]]:
        pass


class DefaultTokenizer(Tokenizer):
    def __init__(self, max_delta=0.5):
        self.max_delta = max_delta

    def tokenize(self, events: List[Atom]) -> List[List[Atom]]:
        if not events:
            return []
        result = [[]]
        last_ts = events[0].ts
        for atom in events:
            # if atom.ts - last_ts < self.max_delta:
            is_action_end = False
            is_action_end |= atom.action == 'BTN_TOUCH' and atom.data == 'UP'
            is_action_end |= atom.action == 'ABS_MT_TRACKING_ID'
            #TODO HARDWARE SUPPORT
            result[-1].append(atom)
            if is_action_end:
                result.append([])
            last_ts = atom.ts
        return result


class ActionRecognizer(object):
    def __init__(self, device, tokenizer: Tokenizer = DefaultTokenizer()) -> None:
        self.device = device
        self.tokenizer = tokenizer
        self.basic_recognizers = [self.try_recognize_tap, self.try_recognize_swipe]

    def recognize(self, atoms: List[Atom]) -> List[Event]:
        result = []
        for group in self.tokenizer.tokenize(atoms):
            for br in self.basic_recognizers:
                event = br(group)
                # TODO check that only one recognizer can recognize
                if event is not None:
                    result.append(event)
                    break
        return result

    @staticmethod
    def filter_atoms(atoms: List[Atom], action: str, filters=[]) -> List[Atom]:
        result = []
        for a in (x for x in atoms if x.action == action):
            if sum([1 if f(a) else 0 for f in filters]) == len(filters):
                result.append(a)
        return result

    @staticmethod
    def get_touch_down(atoms: List[Atom]) -> List[Atom]:
        # return ActionRecognizer.filter_atoms(atoms, 'BTN_TOUCH', filters=[lambda x: x.data == 'DOWN'])
        return [atoms[0]]

    @staticmethod
    def get_touch_up(atoms: List[Atom]) -> List[Atom]:
        # return ActionRecognizer.filter_atoms(atoms, 'BTN_TOUCH', filters=[lambda x: x.data == 'UP'])
        return [atoms[-1]]


    def get_max_diff(self, arr: List[int], is_x: bool):
        if is_x:
           _x = [self.device.transform_coordinates(x, 0)[0] for x in arr]
        else:
           _x = [self.device.transform_coordinates(0, x)[1] for x in arr]

        return max(_x) - min(_x)

        # return m

    def try_recognize_tap(self, atoms: List[Atom]) -> Tap:
        down = ActionRecognizer.get_touch_down(atoms)
        up = ActionRecognizer.get_touch_up(atoms)
        if len(down) != 1 or len(up) != 1:
            return None
        down = down[0]
        up = up[0]
        # TODO remove timediff 1s?
        if up.ts < down.ts or up.ts - down.ts > 3.:
            return None
        x_positions = [int(x.data, 16) for x in ActionRecognizer.filter_atoms(atoms, 'ABS_MT_POSITION_X', filters=[
            lambda x: x.ts >= down.ts and x.ts <= up.ts])]
        y_positions = [int(x.data, 16) for x in ActionRecognizer.filter_atoms(atoms, 'ABS_MT_POSITION_Y', filters=[
            lambda x: x.ts >= down.ts and x.ts <= up.ts])]
        # assert len(x_positions) == len(y_positions)

        if not (len(x_positions) > 0 and len(y_positions) > 0):
            return None
        if self.get_max_diff(x_positions, is_x=True) > 10 or self.get_max_diff(y_positions, is_x=False) > 10:
            return None
        return Tap(x=x_positions[0], y=y_positions[0], ts=up.ts)

    def try_recognize_swipe(self, atoms: List[Atom]) -> Swipe:
        down = ActionRecognizer.get_touch_down(atoms)
        up = ActionRecognizer.get_touch_up(atoms)
        if len(down) != 1 or len(up) != 1:
            return None
        down = down[0]
        up = up[0]
        # TODO remove timediff 1s?
        if up.ts < down.ts or up.ts - down.ts > 1.:
            return None
        x_positions = [int(x.data, 16) for x in ActionRecognizer.filter_atoms(atoms, 'ABS_MT_POSITION_X', filters=[
            lambda x: x.ts >= down.ts and x.ts <= up.ts])]
        y_positions = [int(x.data, 16) for x in ActionRecognizer.filter_atoms(atoms, 'ABS_MT_POSITION_Y', filters=[
            lambda x: x.ts >= down.ts and x.ts <= up.ts])]

        if not (len(x_positions) > 0 and len(y_positions) > 0):
            return None
        # assert len(x_positions) == len(y_positions)

        # y_positions2 = [(x.ts, int(x.data, 16)) for x in ActionRecognizer.filter_atoms(atoms, 'ABS_MT_POSITION_Y', filters=[
        #     lambda x: x.ts >= down.ts and x.ts <= up.ts])]

        if self.get_max_diff(x_positions, is_x=True) < 10 and self.get_max_diff(y_positions, is_x=False) < 10:
            return None
        # TODO check cast duration to int
        return Swipe(x1 = x_positions[0], y1 = y_positions[0], x2 = x_positions[-1], y2 = y_positions[-1], duration=(up.ts - down.ts) / 2, ts = down.ts)


#
# # # KitKat+ devices require authentication
# signer = sign_m2crypto.M2CryptoSigner(op.expanduser('~/.android/adbkey'))
# # # Connect to the device
# device = adb_commands.AdbCommands()
# device.ConnectDevice(rsa_keys=[signer])
# #
# # # Now we can use Shell, Pull, Push, etc!
# for d in device.Devices():
#   print("Available %s" % d)
# #
# for i in range(10):
#   print(device.Shell('echo %d' % i))
#

#
# # device.Shell("getevent -lt /dev/input/event1")
# # print('qq')

# def random_actions(n):
#     for i in range(0, n):
#         print('{} / {}'.format(i, n))
#         action = random.choice(['tap', 'tap', 'tap', 'tap', 'tap', 'tap', 'swipe'])
#         if action == 'tap':
#             yield Tap(random.randint(0, 4095), 150 +  random.randint(0, 4095 - 150))
#         elif action == 'swipe':
#             yield Swipe(random.randint(0, 4095), 150 +  random.randint(0, 4095  - 150), random.randint(0, 4095), 150 +  random.randint(0, 4095  - 150), 20 / 1000 + random.random() / 10)


def replay(device, input):
    recognizer = ActionRecognizer(device)
    # with open('open_msg.log') as f:

    # with open('swipe_and_click') as f:
    atoms = [Atom.from_string(line) for line in input.readlines() if line.startswith('[')]
    actions = recognizer.recognize(atoms)
    prev_ts = None
    for action in actions:
        if prev_ts:
            print('sleeping :{}'.format(action.ts - prev_ts))
            time.sleep(0.9 * (action.ts - prev_ts))
        print('recognized %s' % action)
        prev_ts = action.ts
        action.send(device)


class MonkeyRunner:
    def __init__(self, device: Device, path: str, each_nth = 4, c = 1000, app=DEFAULT_APP, random_action_rate=0.35):
        self.device = device
        self.path = path
        self.each_nth = each_nth
        self.c = c
        self.app = app
        self.random_action_rate = random_action_rate

        self.random_actions = []
        print('Initializing random actions')
        for i in range(0, int(device.max_mt_pos_x / 32)):
            for j in range(0, int(device.max_mt_pos_y / 32)):
                x = i * 32 + 16
                y = j * 32 + 16
                self.random_actions.append(Tap(x, y))
                if random.random() < 0.1:
                    self.random_actions.append(Swipe(x, y, min(x + self.device.max_mt_pos_x / 4, self.device.max_mt_pos_x), y, 0.05))
                if random.random() < 0.1:
                    self.random_actions.append(Swipe(x, y, max(x - self.device.max_mt_pos_x / 4, 0), y, 0.05))
                if random.random() < 0.1:
                    self.random_actions.append(Swipe(x, y, x, min(y + self.device.max_mt_pos_y / 4, self.device.max_mt_pos_y), 0.05))
                if random.random() < 0.1:
                    self.random_actions.append(Swipe(x, y, x, max(y - self.device.max_mt_pos_y / 4, 0), 0.05))

        recognizer = ActionRecognizer(self.device)
        self.recorded_tests = []
        for fname in ['{}/{}'.format(self.path, f) for f in listdir(self.path) if isfile(join(self.path, f))]:
            print('parsing test {}'.format(fname))
            with open(fname) as script:
                atoms = [Atom.from_string(line) for line in script.readlines() if line.startswith('[')]
                actions = recognizer.recognize(atoms)
                self.recorded_tests.append(actions)


        self.all_recorded_actions = []
        for actions in self.recorded_tests:
            for a in actions:
                self.all_recorded_actions.append(a)


    def do_monkey_test(self):
        for test in self.recorded_tests:
            print('next test')
            self.__apply_patches_and_run(test)

    def __apply_patches_and_run(self, test: List[Atom]):
        for i in range(0, int(math.ceil(len(test) / self.each_nth))):
            pivot = random.randint(0, len(test))

            was_crashed, records = self.__run(test[:pivot], monkey=True)
            if was_crashed:
                for rec in records:
                    self.device.pull(rec, './img/{}'.format(rec.replace('/sdcard/', '')))

            for rec in records:
                self.device.rm(rec)

        was_crashed, records = self.__run(test, monkey=True)
        if was_crashed:
            for rec in records:
                self.device.pull(rec, './img/{}'.format(rec.replace('/sdcard/', '')))

        for rec in records:
            self.device.rm(rec)

    def __run(self, test: List[Atom], monkey = False):
        additional_actions = self.c if monkey else 0
        pid = self.device.restart_app(app=self.app)
        screen_recorder = ScreenRecorder(device=self.device)
        screen_recorder.start()
        time.sleep(2.)
        print('strating with pid {} == {}'.format(pid, self.device.adb_device.get_pid(self.app)))
        was_crashed = False
        for i in range(0, additional_actions + len(test)):
            if i < len(test):
                action = test[i]
            elif random.random() < self.random_action_rate:
                action = random.choice(self.random_actions)
            else:
                action = random.choice(self.all_recorded_actions)

            print('[{}/{}] Executing action {}'.format(i - len(test), additional_actions, action))
            action.send(self.device)
            if not self.device.is_focused(app=self.app):
                print('app is not in focuse')
                self.device.back()
                if not self.device.is_focused(app=self.app):
                    time.sleep(2.5)
                if not self.device.is_focused(app=self.app):
                    if self.device.was_crashed(pid, app=self.app):
                        print('CRASH FOUND!!!')
                        # time.sleep(10000000)
                    print('something wrong, restarting')
                    was_crashed = True
                    break
                    # pid = self.device.restart_app(app=DEFAULT_APP)
                if self.device.is_forbidden_activity(activity='AddOrChangePinActivity'):
                    print('AddOrChangePinActivity actibvity is undebuggable')
                    self.device.back()

        screen_recorder.stop()
        if not was_crashed and int(pid) != int(self.device.adb_device.get_pid(self.app)):
            was_crashed = True
        return was_crashed, screen_recorder.records

def monkey_via_scripts(device, path, c = 100000):
    pid = device.restart_app(app=DEFAULT_APP)

    recognizer = ActionRecognizer(device)

    recorded_tests = []
    for fname in ['{}/{}'.format(path, f) for f in listdir(path) if isfile(join(path, f))]:
        with open(fname) as script:
            atoms = [Atom.from_string(line) for line in script.readlines() if line.startswith('[')]
            actions = recognizer.recognize(atoms)
            recorded_tests.append(actions)

    all_recorded_actions = []
    for actions in recorded_tests:
        for a in actions:
            all_recorded_actions.append(a)

    random_actions = []
    for i in range(0, int(device.max_mt_pos_x / 32)):
        for j in range(0, int(device.max_mt_pos_y / 32)):
            x = i * 32 + 16
            y = j * 32 + 16
            random_actions.append(Tap(x, y))
            if random.random() < 0.1:
                random_actions.append(Swipe(x, y, min(x + device.max_mt_pos_x / 4, device.max_mt_pos_x), y, 0.05))
            if random.random() < 0.1:
                random_actions.append(Swipe(x, y, max(x - device.max_mt_pos_x / 4, 0), y, 0.05))
            if random.random() < 0.1:
                random_actions.append(Swipe(x, y, x, min(y + device.max_mt_pos_y / 4, device.max_mt_pos_y), 0.05))
            if random.random() < 0.1:
                random_actions.append(Swipe(x, y, x, max(y - device.max_mt_pos_y / 4, 0), 0.05))

    for i in range(0, c):
        if i % 2 == 0:
            action = random.choice(all_recorded_actions)
        else:
            action = random.choice(random_actions)

        print('Executing action {}'.format(action))
        action.send(device)
        if not device.is_focused(app=DEFAULT_APP):
            print('app is not in focuse')
            device.back()
            if not device.is_focused(app=DEFAULT_APP):
                if device.was_crashed(pid, app=DEFAULT_APP):
                    print('CRASH FOUND!!!')
                    # time.sleep(10000000)
                print('something wrong, restarting')
                pid = device.restart_app(app=DEFAULT_APP)
            if device.is_forbidden_activity(activity='AddOrChangePinActivity'):
                print('AddOrChangePinActivity actibvity is undebugable')
                device.back()

def main(*args):
    parser = argparse.ArgumentParser(
        description='Record events from an Android device')
    parser.add_argument('--device', type=str,
                        help='Directs command to the only connected USB device', default='emulator-5554')
    parser.add_argument('--path', type=str,
                        help='path to saved scripts', default='./android/recorder/scripts_nexus')
    parser.add_argument('--monkey', action='store_true',
                        help='Run monkey tests')
    parser.add_argument('--record', action='store_true',
                        help='Record test case')
    parser.add_argument('--replay', action='store_true',
                        help='Replay test case')
    parser.add_argument('-i', '--input', type=str,
                        help='Input file')

    args = parser.parse_args()


    # Default is "127.0.0.1" and 5037
    device = Device(args.device)

    print(device.is_focused())

    if device is None:
        raise Exception('cant connect to device %s' % args.device)

    if args.record:
        recorder: Recorder = None
        for line in sys.stdin:
            line = line.strip()
            if line.startswith('r '):
                if recorder is not None:
                    recorder.stop()
                    recorder.join()
                name = line.split(' ')[1]
                recorder = Recorder(device, os.path.join(args.path, name))
                recorder.start()
            elif line == 'p':
                if recorder is not None:
                    recorder.stop()
                    recorder.join()
                recorder = None
                print('record on pause')
            elif line == 'exit':
                if recorder is not None:
                    recorder.stop()
                    recorder.join()
                recorder = None
                sys.exit(0)
            else:
                print('unrecognized command "%s"' % line)
    elif args.replay:
        print('Replaing test')
        with open(os.path.join(args.path, args.input), 'r') as input:
            replay(device, input)
    elif args.monkey:
        print('running monkey tests')
        mr = MonkeyRunner(device, args.path, c=350)
        mr.do_monkey_test()
        # monkey_via_scripts(device, path=args.path)


if __name__ == '__main__':
    main(*sys.argv)
