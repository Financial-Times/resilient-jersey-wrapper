package com.ft.jerseyhttpwrapper.config.validation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * MultivaluePatternsValidatorTest
 *
 * @author Georgi Kazakov
 */
public class MultivaluePatternsValidatorTest {

  private DeniedRegexpsValidator deniedRegexpsValidator;

  @Before
  public void setUpValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    deniedRegexpsValidator =
        new DeniedRegexpsValidator(
            Stream.of("Referer", "Transaction-Id", "Fastly", "X-Timer", "content-Length")
                .collect(Collectors.toSet()),
            validator);
  }

  @Test
  public void shouldReturnOneViolationPerField() {

    // put the string lists in backwards

    Set<ConstraintViolation<DeniedRegexpsValidator>> result =
        deniedRegexpsValidator.getValidator().validate(deniedRegexpsValidator);

    Iterator<ConstraintViolation<DeniedRegexpsValidator>> iter = result.iterator();
    while (iter.hasNext()) {
      System.out.println("Current denied header is :" + iter.next().getMessage());
    }
    assertThat(result.size(), is(4));
  }
}
