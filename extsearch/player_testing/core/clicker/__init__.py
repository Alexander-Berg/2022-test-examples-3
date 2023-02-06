from play_button import PlayButton
from motion_detector import MotionDetector
from time import sleep
import random


class Clicker(object):
    def __init__(self, display, input_device, start_timeout):
        self.display = display
        self.input_device = input_device
        self.start_timeout = start_timeout

    def _move_and_click(self, target):
        viewport = self.display.viewport
        for i in range(5):
            x = random.randint(target[0] - 25, target[0] + 25)
            y = random.randint(target[1] - 25, target[1] + 25)
            if x >= 0 and x < viewport.width and y >= 0 and y < viewport.height:
                self.input_device.move_cursor(viewport.left + x, viewport.top + y).execute()
                sleep(0.2)
        self.input_device.move_cursor(viewport.left + target[0], viewport.top + target[1]).execute().click().execute()
        sleep(self.start_timeout)

    def play(self, logsvc, artifacts, player_id, result_url):
        # TODO(kuskarov): pass whole job context here
        if player_id == 'ivi' or result_url.find('www.youtube.com') != -1:
            self._move_and_click((self.display.viewport.width / 2, self.display.viewport.height / 2))
            artifacts.log('click', 'center', player_id)
            return
        has_ytb_before = logsvc.has_known_player()
        cap_before = artifacts.last_capture
        btn_before = PlayButton.lookup(cap_before, artifacts.add_visual())
        target = (btn_before[0].x, btn_before[0].y) if len(btn_before) > 0 else (self.display.viewport.width / 2, self.display.viewport.height / 2)
        self._move_and_click(target)
        self.display.capture_screen(artifacts.add_content_capture())
        cap_after = artifacts.last_capture
        btn_after = PlayButton.lookup(cap_after, artifacts.add_visual())
        has_ytb_after = logsvc.has_known_player()
        # attempt to second click
        second_click = False
        if (MotionDetector.calc_moving_area([cap_before, cap_after]) < 0.05 or (not has_ytb_before and has_ytb_after)) and len(btn_after) > 0:
            self._move_and_click((btn_after[0].x, btn_after[0].y))
            second_click = True
        artifacts.log('click', 'default:', len(btn_before) == 0, 'second:', second_click, 'target:', target)
