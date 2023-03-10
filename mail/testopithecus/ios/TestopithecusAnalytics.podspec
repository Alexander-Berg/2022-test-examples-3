#
#  Be sure to run `pod spec lint xmail.podspec' to ensure this is a
#  valid spec and to remove all comments including this before submitting the spec.
#
#  To learn more about Podspec attributes see http://docs.cocoapods.org/specification.html
#  To see working Podspecs in the CocoaPods repo see https://github.com/CocoaPods/Specs/
#

Pod::Spec.new do |s|

  # ―――  Spec Metadata  ―――――――――――――――――――――――――――――――――――――――――――――――――――――――――― #
  #
  #  These will help people to find your library, and whilst it
  #  can feel like a chore to fill in it's definitely to your advantage. The
  #  summary should be tweet-length, and the description more in depth.
  #

  s.name             = 'TestopithecusAnalytics'
  s.version          = '0.1'
  s.summary          = 'Testopithecus Analytics package'
  s.homepage         = 'https://a.yandex-team.ru/arc/trunk/arcadia/mail/testopithecus/common/code/mail/logging'
  s.license          = { type: 'PROPRIETARY', text: '2019 © Yandex. All rights reserved.' }
  s.author           = { 'Amosov Fedor' => 'amosov-f@yandex-team.ru' }
  s.platform         = :ios, '11.0'
  s.swift_version    = '5.0'
  s.source           = { svn: 'https://arcadia.yandex.ru/arc/trunk/arcadia/mail/testopithecus/ios/analytics' }
  s.source_files     = 'Sources/testopithecus/generated/mail/logging/**/*.swift', 'Sources/testopithecus/dependencies/*.swift'
  s.requires_arc     = true

  # ――― Resources ―――――――――――――――――――――――――――――――――――――――――――――――――――――――――――――――― #
  #
  #  A list of resources included with the Pod. These are copied into the
  #  target bundle with a build phase script. Anything else will be cleaned.
  #  You can preserve files from being cleaned, please don't preserve
  #  non-essential files like tests, examples and documentation.
  #

  # s.resource  = 'icon.png'
  # s.resources = 'Resources/*.png'

  # s.preserve_paths = 'FilesToSave', 'MoreFilesToSave'

  # ――― Project Linking ―――――――――――――――――――――――――――――――――――――――――――――――――――――――――― #
  #
  #  Link your library with frameworks, or libraries. Libraries do not include
  #  the lib prefix of their name.
  #

  # s.framework  = 'SomeFramework'
  # s.frameworks = 'SomeFramework', 'AnotherFramework'

  # s.libraries = 'iconv', 'xml2'

  # ――― Project Settings ――――――――――――――――――――――――――――――――――――――――――――――――――――――――― #
  #
  #  If your library depends on compiler flags you can set them in the xcconfig hash
  #  where they will only apply to your library. If you depend on other Podspecs
  #  you can include multiple dependencies to ensure it works.
end
