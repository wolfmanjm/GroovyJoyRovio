import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.TRACE
import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.ERROR
import static ch.qos.logback.classic.Level.WARN

appender("RootConsoleAppender", ConsoleAppender) {
  filter(ThresholdFilter) {
	level = TRACE
  }
  encoder(PatternLayoutEncoder) {
	pattern = "%r [%t] %p, %c - %m%n"
  }
}

root(INFO, ["RootConsoleAppender"])

logger("com.e4net", DEBUG)
logger("httpclient.wire.content", WARN)
logger("httpclient.wire.header", WARN)
logger("org.apache.commons.httpclient", WARN)
logger("org.apache.commons.httpclient.HttpMethodDirector", ERROR)
