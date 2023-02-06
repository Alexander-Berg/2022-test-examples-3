import json

with open("topic_honeypots.json")as f:
    data = json.load(f)


def select_uniform_elements(arr, count):
    size = len(arr)
    count = min(count, size)

    return [arr[int(float(i * size) / count)] for i in range(count)]


for i in data:
    j = i["input_values"]
    if len(j["screenshots"]) > 15:
        j["screenshots"] = select_uniform_elements(j["screenshots"], 15)

with open("topic_honeypots.json", "w")as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
