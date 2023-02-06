import argparse
import itertools
import json
import random
import requests
import sys

from itertools import cycle

defaultProfile = """{
                "school_school": 20,
                "school_academy": 20,
                "blogShowcase_academy": 21,
                "popularPostsUsed_academy": 45,
                "post_academy": 45,
                "post_schoolbook": 39,
                "postRelated_schoolbook": 40,
                "schoolsPublished_school": 170,
                "mediaMaterialsPublished_academy": 55,
                "mediaMaterialsPublished_schoolbook": 52,
                "categoriesAll_academy": 60,
                "categoriesAll_schoolbook": 48,
                "taskRunnerAggregator": 150,
                "taskRunnerInstantMail": 150,
                "taskRunnerMailer": 150,
                "taskRunnerPostsPublish": 150
                }"""


def weighted_random(handlers):
	choices = [[handler] * weight for handler, weight in handlers.items()]
	return random.choice(list(itertools.chain.from_iterable(choices)))


class Ammo(object):
    def __init__(self, wmi, profile, count):
        self.session = requests.session()
        self.wmi = wmi
        self.ammo_count = count
        self.tags = [weighted_random(json.loads(profile.strip())) for _ in xrange(self.ammo_count)]
        self.legal_tags = (
            # Controller /v1/blogs
            "blogs",
            "blogsPrivate",
            # Controller /v1/blog
            "blog_academy",
            "blogSubscribers_academy",
            "blogShowcase_academy",
            # Controller /v1/schools
            "schools_school",
            "schoolsArchived_school",
            "schoolsDrafts_school",
            "schoolsPublished_school",
            # Controller /v1/school
            "school_academy",
            "school_school",
            # Controller /v1/post
            "post_academy",
            "post_schoolbook",
            "postRelated_academy",
            "postRelated_schoolbook",
            # Controller /v1/mediaMaterials
            "mediaMaterialsDrafts_academy",
            "mediaMaterialsDrafts_schoolbook",
            "mediaMaterialsFuture_academy",
            "mediaMaterialsPublished_academy",
            "mediaMaterialsPublished_schoolbook",
            # Controller /v1/taskRunner
            "taskRunnerAggregator",
            "taskRunnerSaas",
            "taskRunnerPostsPublish",
            "taskRunnerMailer",
            "taskRunnerInstantMail",
            # Controller /v1/category
            "categoriesAll_academy",
            "categoriesAll_schoolbook",
            "categoriesNotEmpty_academy",
            "categoryExist_academy",
            # Controller /v1/popularPosts
            "popularPostsUsed_academy",
            "popularPosts_academy"
        )

# Get id's list of the posts or schools  
    def get_ids(self, bid, controller):
        ids = list()
        itemsUri = "/v1/%s/%s" % (controller, bid)
        items = self.session.get("http://" + self.wmi + itemsUri, stream=True).json()
        if type(items) is list:
            for item in items:
                if "_id" in item.keys():
                    ids.append(item["_id"])
        elif type(items) is dict:
            if "_id" in items.keys():
                ids.append(items["_id"])
        return ids

# Get id's lists of the posts and schools for each blog and generate the cycle sequences from them 
    def make_bids(self):
        bids = dict()
        blogsUri = '/v1/blogs'
        blogs = self.session.get("http://" + self.wmi + blogsUri, stream=True).json()
        for blog in blogs:
            if "slug" in blog.keys():
                bids[blog["slug"]] = {
                    "postIdentity": cycle(self.get_ids(blog["slug"], "posts")),
                    "schoolIdentity": cycle(self.get_ids(blog["slug"], "schools"))
                }
        return bids 

# Create the ammo structure for each generated bullit and fill them by the data 
    def make_ammo(self):
        bullits = list()
        idsForBlogs = self.make_bids()
        for tag in self.tags:
            if tag in self.legal_tags:
                bid = pid = sid = ""
                if len(tag.split('_')) > 1:
                    uri, bid = tag.split('_')
                    if uri in ('school'):
                        sid = next(idsForBlogs[bid]['schoolIdentity'])
                    elif uri in ('post', 'postRelated'):
                        pid = next(idsForBlogs[bid]['postIdentity'])
                bullit = {'tag': tag, 'bid': bid, 'pid': pid, 'sid': sid}                 
                bullits.append(bullit)
            else:
                print("Wrong tag: {}".format(tag))
                sys.exit(3)
        with open("ammo.json", "w") as ammo:
            ammo.write("\n".join(json.dumps(bullit) for bullit in bullits).encode('utf-8'))
        self.session.close()


if __name__ == "__main__":

    parser = argparse.ArgumentParser()
    parser.add_argument('--wmi', type=str, help='Target host', default='mmtl2hkb2c4lp33f.sas.yp-c.yandex.net')
    parser.add_argument('--profile', type=str, help='string with json profile, in handler:weight format', default=defaultProfile)
    parser.add_argument('--count', type=int, help='Count of bullits in ammo file', default=40000)
    args = parser.parse_args()

    ammo = Ammo(args.wmi, args.profile, args.count)
    ammo.make_ammo()
