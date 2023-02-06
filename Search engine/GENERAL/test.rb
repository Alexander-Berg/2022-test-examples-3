{
  somename1: { domain: 'test', port: 8081 },
  somename2: { domain: 'test', port: 8081 },
  somename3: { domain: 'test', port: 8082 },
  somename4: { domain: 'test2', port: 8081 },
}.each do |name, info|
  disco name.to_s do
    action :announce
    type 'web-application'
    publish do |data|
      data[:domain] = info[:domain]
      data[:port]   = info[:port].to_s
    end
  end
end

disco "nginx-balancer-ttt" do
  action :announce
  type 'front-application'
  publish do |data|
    data[:domain] = 'ttt'
    data[:port]   = '1212'
    # data[:https_only] = app.https_only
  end
end
