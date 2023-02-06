import random
import json
import argparse
import os


def weighted_json_choice(choices):
    '''dict where key is choice and value probability'''
    total = sum(choices[choice] for choice in choices)
    r = random.uniform(0, total)
    upto = 0
    for choice in choices:
        if upto + choices[choice] >= r:
            return choice
        upto += choices[choice]


class Ammo(object):
    def __init__(self, profile, count):
        self.ammo_count = count
        self.handlers = [weighted_json_choice(json.loads(profile.strip())) for _ in range(self.ammo_count)]
        self.tags = ("institutions", "students", "schools", "adult")
        self.classes = (5, 6, 7, 8, 9, 10, 11)
        self.legal_handlers = {
            "root": [
                lambda x: "/", 
                lambda x: "/?tag={}".format(random.choice(self.tags))],
            "lessons": [
                lambda x: "/lessons", 
                lambda x: "/lessons?class={}".format(random.choice(self.classes))],
            "projects": [
                lambda x: "/api/projects", 
                lambda x: "/api/projects?offset={}".format(random.randint(0,5)),
                lambda x: "/api/projects?limit={}".format(random.randint(0,5)),
                lambda x: "/api/projects?tag={}".format(random.choice(self.tags))],
            "samples": [
                lambda x: "/api/projects/samples", 
                lambda x: "/api/projects/samples?limit={}".format(random.randint(0,5))]
        }


    def make_ammo(self):
        with open("ammo.list", "w") as ammo:
            for  handle in self.handlers:
                if handle in self.legal_handlers.keys():
                    line = '%s %s\r\n' % (random.choice(self.legal_handlers[handle])(1), handle)
                    ammo.write(line)
                else:
                    print("Wrong tag: ".format(tag))
                    os.Exit(1)


if __name__ == "__main__":

    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", type=str, help="string with json profile, in handler:weight format", default='{"root": 5, "lessons": 3, "projects": 1, "samples": 1}')
    parser.add_argument("--count", type=int, help="count of bullits in the ammo", default=40000)
    args = parser.parse_args()

    ammo = Ammo(args.profile, args.count)
    ammo.make_ammo()