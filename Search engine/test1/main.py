import random

from flask import Flask
from flask_restful import Api, Resource


app = Flask(__name__)
api = Api(app)


class Ping(Resource):
    def get(self):
        return "OK", 200


class Pong(Resource):
    def get(self):
        return "Not Ok, but its OK", 500


class Rand(Resource):
    def get(self):
        return random.randint(0, 300), 200


api.add_resource(Ping, "/ping")
api.add_resource(Rand, "/rand")
api.add_resource(Pong, "/pong")
api.add_resource(Ping, "/")


def main():
    app.run(port=9999)


if __name__ == '__main__':
    main()
