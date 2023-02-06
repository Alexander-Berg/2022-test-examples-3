s3-metadata-config:
  service_id: 802
  client_id: 101
  client_secret: {{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[back-testing-s3-secret]") }}
  oauth_token: {{ salt["yav.get"]("sec-01fz0ymvb4tw2g91v6te3yqahy[back-testing-s3-oauth]") }}
  type: oauth
  password: False
  login: False
  use_cache: True
