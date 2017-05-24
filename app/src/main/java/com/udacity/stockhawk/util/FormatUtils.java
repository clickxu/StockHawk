package com.udacity.stockhawk.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by t-xu on 5/23/17.
 */

public class FormatUtils {

    public static final DecimalFormat sDollarFormat =
            (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
    public static final DecimalFormat sDollarFormatWithPlus = getDollarFormatWithPlus();
    public static final DecimalFormat sPercentageFormat = getPercentageFormat();

    private static DecimalFormat getDollarFormatWithPlus() {
        DecimalFormat result = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        result.setPositivePrefix("+$");
        return result;
    }

    private static DecimalFormat getPercentageFormat() {
        DecimalFormat result = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        result.setMaximumFractionDigits(2);
        result.setMinimumFractionDigits(2);
        result.setPositivePrefix("+");
        return result;
    }
}
