require 'buildr/groovy/compiler'

# Version number for this release
VERSION_NUMBER = "1.0.0"

# Group identifier for your projects
GROUP = "com.e4net.rovio"
COPYRIGHT = "Jim Morris c 2011"

repositories.remote << 'http://www.ibiblio.org/maven2/'
repositories.remote << 'http://repo1.maven.org/maven2/'
repositories.remote << "http://repository.codehaus.org/"
repositories.remote << "http://snapshots.repository.codehaus.org/"

COMMONS             = struct(
  :codec            =>"commons-codec:commons-codec:jar:1.3",
  :io               =>"commons-io:commons-io:jar:1.2",
  :collections      =>"commons-collections:commons-collections:jar:3.2.1",
  :beanutils        =>"commons-beanutils:commons-beanutils:jar:1.7.0",
  :dbcp             =>"commons-dbcp:commons-dbcp:jar:1.2.1",
  :fileupload       =>"commons-fileupload:commons-fileupload:jar:1.1.1",
  :httpclient       =>"commons-httpclient:commons-httpclient:jar:3.1",
  :lang             =>"commons-lang:commons-lang:jar:2.4",
  :logging          =>"commons-logging:commons-logging:jar:1.1.1",
  :pool             =>"commons-pool:commons-pool:jar:1.4",
  :primitives       =>"commons-primitives:commons-primitives:jar:1.0"
)

LOGGER= [group('slf4j-api', :under => 'org.slf4j', :version => '1.6.1'), transitive('ch.qos.logback:logback-classic:jar:0.9.29')]

# 'org.codehaus.groovy.modules.http-builder:http-builder:jar:0.5.2-SNAPSHOT',
HTTPBUILDER= [ 'org.apache.httpcomponents:httpclient:jar:4.0.3', 'org.apache.httpcomponents:httpcore:jar:4.0.1',
               COMMONS.logging, COMMONS.codec, COMMONS.collections,
               'net.sf.json-lib:json-lib:jar:jdk15:2.3', 'xml-resolver:xml-resolver:jar:1.2' ]

MIGLAYOUT= 'com.miglayout:miglayout:jar:swing:3.7.4'

LIBS= [LOGGER, HTTPBUILDER, MIGLAYOUT]

artifact_ns(Buildr::Groovy::Groovyc).groovy = '1.8.0'


define 'rovio' do
  project.group = GROUP
  project.version = VERSION_NUMBER
  compile.options.target = '1.6'
  compile.with LIBS, _("libs/Joystick.jar"), _("libs/http-builder-0.5.2-20110320.041601-11.jar")
  #, _("libs/groovypp-0.4.268_1.8.0.jar")
  package(:jar)

  # load up local profiles info, can store passwords here
  if File.exist?("user.local.yml")
    Buildr.settings.user.merge!(YAML.load(File.read(_("user.local.yml"))))
    #puts "settings: #{Buildr.settings.user.inspect}"
    username, password= Buildr.settings.user['rovio'].values_at('username', 'password')
  else
    username= "admin"
    password= "admin"
  end
  run.using :main => ["com.e4net.rovio.RovioConsole", username, password], :java_args => ["-Djava.library.path=./libs"]
end

desc "copy artifacts into libs"
task :make_libs do
    tasks = Buildr.artifacts(LIBS)
    paths = tasks.map(&:name)
    paths.each do |f|
      puts f
      `cp #{f} ./run-libs`
    end
end

desc "print out classpath"
task :classpath do
    cp= Buildr.artifacts(LIBS).each(&:invoke).map(&:name).collect{ |f| "./libs/#{File.basename(f)}" }
    puts cp.join(":")
end

task :trans do
  # puts transitive(LOGGING.first)
  puts transitive('org.codehaus.groovy.modules.http-builder:http-builder:jar:0.5.2-SNAPSHOT')
  #puts transitive('org.apache.httpcomponents:httpclient:jar:4.0.3')
end
