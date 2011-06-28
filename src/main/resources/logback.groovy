import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.TRACE
import static ch.qos.logback.classic.Level.DEBUG

appender("RootConsoleAppender", ConsoleAppender) {
  filter(ThresholdFilter) {
	level = TRACE
  }
  encoder(PatternLayoutEncoder) {
	pattern = "%r [%t] %p, %c - %m%n"
  }
}
root(DEBUG, ["RootConsoleAppender"])
