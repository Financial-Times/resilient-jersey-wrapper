package com.ft.jerseyhttpwrapper.config.validation;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * MultivaluePatternValidatorTest
 *
 * @author Simon.Gibbs
 */
public class MultivaluePatternValidatorTest {

  public static final String ONE_OR_MORE_DIGITS = "\\d+";
  public static final String ONE_OR_MORE_LETTERS = "[A-Za-z]+";

  private static class ExampleClass {

    private final List<String> numbers;

    public ExampleClass(List<String> numbers) {
      this.numbers = numbers;
    }

    @MultivaluePattern(regexp = ONE_OR_MORE_DIGITS)
    public List<String> getNumbers() {
      return numbers;
    }
  }

  private static class TwoRegexExampleClass {

    private final List<String> numbers;
    private final List<String> letters;

    public TwoRegexExampleClass(List<String> numbers, List<String> letters) {
      this.numbers = numbers;
      this.letters = letters;
    }

    @MultivaluePattern(regexp = ONE_OR_MORE_DIGITS)
    public List<String> getNumbers() {
      return numbers;
    }

    @MultivaluePattern(regexp = ONE_OR_MORE_LETTERS)
    private List<String> getLetters() {
      return letters;
    }
  }

  Validator validator;

  @Before
  public void setUpValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  public void shouldPassASuccessfulExample() {

    ExampleClass success = new ExampleClass(Arrays.asList("0", "123", "678"));
    Set<ConstraintViolation<ExampleClass>> result = validator.validate(success);

    assertTrue(result.isEmpty());
  }

  @Test
  public void shouldFailAFailingExample() {

    ExampleClass success = new ExampleClass(Arrays.asList("0", "ABC", "678", ""));
    Set<ConstraintViolation<ExampleClass>> result = validator.validate(success);

    assertFalse(result.isEmpty());
  }

  @Test
  public void shouldFailABlankString() {

    ExampleClass success = new ExampleClass(Arrays.asList("0", ""));
    Set<ConstraintViolation<ExampleClass>> result = validator.validate(success);

    assertFalse(result.isEmpty());
  }

  @Test
  public void shouldProvideMessageWithFailingValue() {

    ExampleClass success = new ExampleClass(Arrays.asList("0", "ABC", "678", ""));
    Set<ConstraintViolation<ExampleClass>> result = validator.validate(success);

    assertThat(result.iterator().next().getMessage(), containsString("ABC"));
  }

  @Test
  public void shouldProvideMessageWithCurlyBracesResolved() {

    ExampleClass success = new ExampleClass(Arrays.asList("0", "ABC", "678", ""));
    Set<ConstraintViolation<ExampleClass>> result = validator.validate(success);

    String message = result.iterator().next().getMessage();

    assertThat(message, not(containsString("{")));
    assertThat(message, not(containsString("}")));
  }

  @Test
  public void shouldReturnOneViolationPerField() {

    // put the string lists in backwards
    TwoRegexExampleClass success =
        new TwoRegexExampleClass(Arrays.asList("ABC", "DEF", "GHI"), Arrays.asList("123", "456"));
    Set<ConstraintViolation<TwoRegexExampleClass>> result = validator.validate(success);

    assertThat(result.size(), is(2));
  }
}
