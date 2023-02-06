import unittest
from mock.mock import patch
from yaml import load
from static import *
import logging
import subprocess


class MailGenmapReclusterHandlerTestServer(unittest.TestCase):

    def test_installMongo(self):
        config = "tests/configs/config_env.yaml"

        with open(config) as f:
            conf_p = load("".join(f.readlines()))

        print(conf_p['mongo']['envdir'])
        print(conf_p['mongo']['mongourl'])
        print(conf_p['mongo']['mongodistrib'])

        mngurl = conf_p['mongo']['mongourl'].format(mongodistrib=conf_p['mongo']['mongodistrib'])
        print(mngurl)

        subprocess.Popen("pwd",
                         shell=True
                         ).wait()

        print("mkdir -p {mongodir}".format(mongodir=conf_p['mongo']['envdir']))
        print("mkdir -p {mongodir}/{dirdb1}".format(mongodir=conf_p['mongo']['envdir'],
                                                    dirdb1=conf_p['mongo']['dirdb1']))

        subprocess.Popen("mkdir -p {mongodir}".format(mongodir=conf_p['mongo']['envdir']),
                         shell=True
                         ).wait()

        subprocess.Popen("mkdir -p {mongodir}/{dirdb1}".format(mongodir=conf_p['mongo']['envdir'],
                                                               dirdb1=conf_p['mongo']['dirdb1']),
                         shell=True
                         ).wait()

        print("wget {mngurl} -O {mongodir}/{mongodistrib}.tgz".format(mngurl=mngurl,
                                                                      mongodir=conf_p['mongo']['envdir'],
                                                                      mongodistrib=conf_p['mongo']['mongodistrib']))

        #TODO: if we have binaries we doesn't need download it again

        #subprocess.Popen("wget {mngurl} -O {mongodir}/{mongodistrib}.tgz".format(mngurl=mngurl,
        #                                                                         mongodir=conf_p['mongo']['envdir'],
        #                                                                         mongodistrib=conf_p['mongo']['mongodistrib']),
        #                 shell=True
        #                 ).wait()

        #subprocess.Popen("tar -xvf {mongodir}/{mongodistrib}.tgz -C {mongodir}/".format(mongodir=conf_p['mongo']['envdir'],
        #                                                                                mongodistrib=conf_p['mongo']['mongodistrib']),
        #                 shell=True
        #                 ).wait()

        #../ build / mongo / mongodb - linux - x86_64 - ubuntu1404 - 3.4.9 / bin / mongod - f tests / configs / mongodb1.conf

        print("{mongodir}/{mongodistrib}/bin/mongod -f {configdb1}".format(mongodir=conf_p['mongo']['envdir'],
                                                                                mongodistrib=conf_p['mongo']['mongodistrib'],
                                                                                configdb1=conf_p['mongo']['configdb1']))

        mongoproc = subprocess.Popen("{mongodir}/{mongodistrib}/bin/mongod -f {configdb1}".format(mongodir=conf_p['mongo']['envdir'],
                                                                                                  mongodistrib=conf_p['mongo']['mongodistrib'],
                                                                                                  configdb1=conf_p['mongo']['configdb1']),
                                     shell=True,
                                     stdout=subprocess.PIPE,
                                     stderr=subprocess.PIPE)

        mongoproc.wait()
        output = mongoproc.communicate()

        print(output)

        #tar - xvf.. / build / mongo / mongo.tgz - C.. / build / mongo /



        #"wget https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-ubuntu1404-3.4.9.tgz"
        self.assertTrue("" == "")


    #def launchMongo(self):
    #    pass

if __name__ == "__main__":
    print("the great test begin")
    unittest.main()