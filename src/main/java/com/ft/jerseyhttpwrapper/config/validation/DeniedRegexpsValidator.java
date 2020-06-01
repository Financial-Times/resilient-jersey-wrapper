package com.ft.jerseyhttpwrapper.config.validation;

import static com.ft.jerseyhttpwrapper.config.validation.MultivaluePatternsConstants.REGEXP_DENIED_CDN_LOOP;
import static com.ft.jerseyhttpwrapper.config.validation.MultivaluePatternsConstants.REGEXP_DENIED_CONNECTION;
import static com.ft.jerseyhttpwrapper.config.validation.MultivaluePatternsConstants.REGEXP_DENIED_CONTENT_LENGTH;
import static com.ft.jerseyhttpwrapper.config.validation.MultivaluePatternsConstants.REGEXP_DENIED_FASTLY;
import static com.ft.jerseyhttpwrapper.config.validation.MultivaluePatternsConstants.REGEXP_DENIED_REFERER;
import static com.ft.jerseyhttpwrapper.config.validation.MultivaluePatternsConstants.REGEXP_DENIED_USER_AGENT;
import static com.ft.jerseyhttpwrapper.config.validation.MultivaluePatternsConstants.REGEXP_DENIED_X_API_KEY;
import static com.ft.jerseyhttpwrapper.config.validation.MultivaluePatternsConstants.REGEXP_DENIED_X_REQUEST_ID;
import static com.ft.jerseyhttpwrapper.config.validation.MultivaluePatternsConstants.REGEXP_DENIED_X_TIMER;
import static com.ft.jerseyhttpwrapper.config.validation.MultivaluePatternsConstants.REGEXP_DENIED_X_VARNISH;

import java.util.Set;
import javax.validation.Validator;

public class DeniedRegexpsValidator {
  private final Set<String> headers;
  private final Validator validator;

  public DeniedRegexpsValidator(Set<String> headers, Validator validator) {
    this.headers = headers;
    this.validator = validator;
  }

  @MultivaluePatterns(
      regexps = {
        REGEXP_DENIED_USER_AGENT,
        REGEXP_DENIED_REFERER,
        REGEXP_DENIED_X_REQUEST_ID,
        REGEXP_DENIED_X_API_KEY,
        REGEXP_DENIED_X_VARNISH,
        REGEXP_DENIED_X_TIMER,
        REGEXP_DENIED_CONNECTION,
        REGEXP_DENIED_CONTENT_LENGTH,
        REGEXP_DENIED_CDN_LOOP,
        REGEXP_DENIED_FASTLY
      })
  public Set<String> getHeaders() {
    return headers;
  }

  public Validator getValidator() {
    return validator;
  }
}
