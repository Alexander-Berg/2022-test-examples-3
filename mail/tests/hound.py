import tornado.ioloop
import tornado.web
import time


class Ping(tornado.web.RequestHandler):
    def get(self):
        self.write('pong')


class Hound(tornado.web.RequestHandler):
    def get(self):
        mid = self.get_argument('mids')
        if mid == '11':
            self.write('{"envelopes": [{"threadId": "999"}]}')
        elif mid == '22':
            self.write('djvnslirfdvg')
        elif mid == '33':
            raise tornado.web.HTTPError(500)
        elif mid == '44':
            time.sleep(5)
            self.write('{"envelopes": [{"threadId": "tid_given"}]}')
        else:
            self.write('{{"envelopes": [{{"threadId": "{mid}"}}]}}'.format(mid=mid))


if __name__ == "__main__":
    application = tornado.web.Application([
        (r"/ping", Ping),
        (r"/filter_search", Hound),
    ])
    application.listen(9999)
    tornado.ioloop.IOLoop.current().start()
