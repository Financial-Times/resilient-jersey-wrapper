package com.ft.jerseyhttpwrapper.config.validation;

public interface MultivaluePatternsConstants {
  String REGEXP_DENIED_USER_AGENT = "(?i:^User-Agent$)";
  String REGEXP_DENIED_REFERER = "(?i:^Referer$)";
  String REGEXP_DENIED_X_REQUEST_ID = "(?i:^X-Request-Id$)";
  String REGEXP_DENIED_X_API_KEY = "(?i:^X-Api-Key$)";
  String REGEXP_DENIED_X_VARNISH = "(?i:^X-Varnish$)";
  String REGEXP_DENIED_X_TIMER = "(?i:^X-Timer$)";
  String REGEXP_DENIED_CONNECTION = "(?i:^Connection$)";
  String REGEXP_DENIED_CONTENT_LENGTH = "(?i:^Content-Length$)";
  String REGEXP_DENIED_CDN_LOOP = "(?i:^Cdn-Loop$)";
  String REGEXP_DENIED_FASTLY = "(?i:^Fastly)";
}
