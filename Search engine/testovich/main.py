from flask import Flask, jsonify
import yt.wrapper as yt

app = Flask(__name__)

ascii_girl = '''
                              _.._
                             .'    '.
                            (____/`\\ \\
                           (  |' ' )  )
                           )  _\\= _/  (
                 __..---.(`_.'  ` \\    )
                `;-""-._(_( .      `; (
                /       `-`'--'     ; )
               /    /  .    ( .  ,| |(
_.-`'---...__,'    /-,..___.-'--'_| |_)
'-'``'-.._       ,'  |   / .........'
          ``;--"`;   |   `-`
             `'..__.'

'''


@app.route('/')
def hello_world():
    return f'Hello World!\n\n{ascii_girl}\n'


@app.route('/stat')
def stat():
    count = 0
    yt.config["proxy"]["url"] = "locke"
    # yt.config["token"]
    count = len(yt.list('//home/deploy/scheduler'))
    payload = [
        ["rviewer_errors_max", count]
    ]
    return jsonify(payload)


def main():
    app.run(host='::', port=80)


if __name__ == "__main__":
    main()
