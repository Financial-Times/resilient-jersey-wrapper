package com.ft.jerseyhttpwrapper.config.validation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * MultivaluePatternsValidator
 *
 * @author Georgi Kazakov
 */
public class MultivaluePatternsValidator
    implements ConstraintValidator<MultivaluePatterns, Iterable<?>> {

  public static final String VALIDATED_VALUE_PLACEHOLDER = "{validatedValue}";
  public static final String VALIDATED_REGEXP_PATTERN_PLACEHOLDER = "{regexp}";

  List<Pattern> regexps = new ArrayList<Pattern>();
  String message;

  @Override
  public void initialize(MultivaluePatterns constraintAnnotation) {
    for (String regex : constraintAnnotation.regexps()) {
      regexps.add(Pattern.compile(regex));
    }
    message = constraintAnnotation.message();
  }

  @Override
  public boolean isValid(Iterable<?> value, ConstraintValidatorContext context) {
    boolean result = true;
    int i = 0;

    Iterator<?> iterator = value.iterator();

    while (iterator.hasNext()) {
      List<String> compromisedRegexps = new ArrayList<String>();
      String member = iterator.next().toString();

      for (Pattern regexp : regexps) {
        if (regexp.matcher(member).matches()) {
          compromisedRegexps.add(regexp.pattern());
        }
      }
      if (!compromisedRegexps.isEmpty()) {
        String messageWithValue =
            message
                .replace(VALIDATED_VALUE_PLACEHOLDER, member)
                .replace(VALIDATED_REGEXP_PATTERN_PLACEHOLDER, compromisedRegexps.toString());

        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate(messageWithValue)
            .addPropertyNode(String.valueOf(i++))
            .addConstraintViolation();

        result = false;
      }
    }
    return result;
  }
}
