import json

import xlsxwriter

workbook = xlsxwriter.Workbook('tables-new.xlsx')
worksheet1 = workbook.add_worksheet()

with open('mail-liza-new.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

rows = []
for id in data:
    rows.append([id, data[id]['PASSED'], data[id]['BROKEN'], data[id]['FAILED'], data[id]['lastModified']])

r = worksheet1.add_table('B2:G1510', {'data': rows})
workbook.close()
print(r)
