package com.ft.jerseyhttpwrapper.config.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * MultivaluePatternValidator
 *
 * @author Simon.Gibbs
 */
public class MultivaluePatternValidator implements ConstraintValidator<MultivaluePattern, Iterable<?>> {

    public static final String VALIDATED_VALUE_PLACEHOLDER = "{validatedValue}";

    Pattern regex;
    String message;

    @Override
    public void initialize(MultivaluePattern constraintAnnotation) {
        regex = Pattern.compile(constraintAnnotation.regexp());
        message = constraintAnnotation.message();

    }

    @Override
    public boolean isValid(Iterable<?> value, ConstraintValidatorContext context) {

        boolean result = true;
        int i=0;

        Iterator iterator = value.iterator();

        while(iterator.hasNext() && result) {

            String member  = iterator.next().toString();

            if(!regex.matcher(member).matches()) {

                String messageWithValue = message.replace(VALIDATED_VALUE_PLACEHOLDER,member);

                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(messageWithValue)
                        .addNode(Integer.toString(i))
                        .addConstraintViolation();

                result = false;
            }

            i++;
        }
        return result;
    }
}
